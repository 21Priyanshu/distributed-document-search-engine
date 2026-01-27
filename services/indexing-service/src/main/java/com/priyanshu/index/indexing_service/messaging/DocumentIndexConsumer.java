package com.priyanshu.index.indexing_service.messaging;

import java.io.InputStream;
import java.time.Instant;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.priyanshu.documents.common.events.DocumentUploadedEvent;
import com.priyanshu.index.indexing_service.dto.SearchDocument;
import com.priyanshu.index.indexing_service.service.DocumentStatusClient;
import com.priyanshu.index.indexing_service.service.IndexingService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DocumentIndexConsumer {

    private static final int MAX_RETRIES = 3;

    private final DocumentStatusClient statusClient;
    private final IndexingService indexingService;
    private final KafkaFailureProducer failureProducer;

    public DocumentIndexConsumer(
            DocumentStatusClient statusClient,
            IndexingService indexingService,
            KafkaFailureProducer failureProducer) {
        this.statusClient = statusClient;
        this.indexingService = indexingService;
        this.failureProducer = failureProducer;
    }

    // Main topic
    @KafkaListener(topics = "document_uploaded", groupId = "search-indexer")
    public void consume(DocumentUploadedEvent event, Acknowledgment ack) {
        handle(event, ack);
    }

    // Retry topic
    @KafkaListener(topics = "document_uploaded_retry", groupId = "search-indexer")
    public void retry(DocumentUploadedEvent event, Acknowledgment ack) {
        handle(event, ack);
    }

    private void handle(DocumentUploadedEvent event, Acknowledgment ack) {
        try {
            process(event);
            ack.acknowledge(); // commit offset only on success
        } catch (Exception ex) {
            log.error("Indexing failed for document {}", event.documentId(), ex);
            handleFailure(event);
            ack.acknowledge(); // commit offset even on failure (we re-publish manually)
        }
    }

    private void handleFailure(DocumentUploadedEvent event) {
        int retries = event.retryCount() + 1;

        if (retries >= MAX_RETRIES) {
            log.error("Sending document {} to DLQ after {} attempts", event.documentId(), retries);
            failureProducer.sendToDlq(event);

            try {
                statusClient.updateStatus(event.documentId(), "FAILED");
            } catch (Exception e) {
                log.error("Failed to update status to FAILED for {}", event.documentId(), e);
            }

        } else {
            DocumentUploadedEvent retryEvent = new DocumentUploadedEvent(
                    event.documentId(),
                    event.storagePath(),
                    event.title(),
                    event.description(),
                    event.ownerId(),
                    retries
            );

            log.warn("Retrying document {} (attempt {})", event.documentId(), retries);
            failureProducer.sendToRetry(retryEvent);
        }
    }

    private void process(DocumentUploadedEvent event) throws Exception {

        log.info("Processing document {}", event.documentId());

        statusClient.updateStatus(event.documentId(), "INDEXING");

        var stat = indexingService.validateFileContent(event);
        if (stat.size() == 0) {
            log.warn("Empty file for document {}", event.documentId());
            statusClient.updateStatus(event.documentId(), "FAILED");
            return;
        }

        InputStream fileStream = indexingService.fetchFile(event.storagePath());
        String content = indexingService.extractText(fileStream);

        if (content == null || content.trim().isEmpty()) {
            log.warn("No extractable content for document {}", event.documentId());
            statusClient.updateStatus(event.documentId(), "FAILED");
            return;
        }

        SearchDocument searchDoc = new SearchDocument(
                event.documentId(),
                event.title(),
                event.description(),
                content,
                event.ownerId(),
                Instant.now().toString()
        );

        indexingService.indexDocument(event.documentId(), searchDoc); // idempotent

        statusClient.updateStatus(event.documentId(), "READY");

        log.info("Successfully indexed document {}", event.documentId());
    }
}
