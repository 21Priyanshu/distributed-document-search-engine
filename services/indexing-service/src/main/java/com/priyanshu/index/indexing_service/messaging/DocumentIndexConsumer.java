package com.priyanshu.index.indexing_service.messaging;
import java.io.InputStream;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.priyanshu.index.indexing_service.dto.SearchDocument;
import com.priyanshu.index.indexing_service.service.DocumentStatusClient;
import com.priyanshu.index.indexing_service.service.IndexingService;

import lombok.extern.slf4j.Slf4j;
import com.priyanshu.documents.common.events.DocumentUploadedEvent;

@Component
@Slf4j
public class DocumentIndexConsumer {
    private final DocumentStatusClient statusClient;
    private final IndexingService indexingService;

    public DocumentIndexConsumer(DocumentStatusClient statusClient, IndexingService indexingService) {
        this.statusClient = statusClient;
        this.indexingService = indexingService;
    }

    @KafkaListener(topics = "document_uploaded", groupId = "search-indexer")
    @Retryable(maxAttempts = 3)
    public void consume(DocumentUploadedEvent event) throws Exception {
        log.info("Received document upload event for document {}", event.documentId());

        statusClient.updateStatus(event.documentId(), "INDEXING");

        var stat = indexingService.validateFileContent(event);

         if (stat.size() == 0) {
            log.warn("File size is 0 for document {}, marking as FAILED", event.documentId());
            statusClient.updateStatus(event.documentId(), "FAILED");
            return;
        }

        InputStream fileStream = indexingService.fetchFile(event.storagePath());

        String content = indexingService.extractText(fileStream);

        if (content == null || content.trim().isEmpty()) {
            log.warn("No extractable content for document {}, marking as FAILED", event.documentId());
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

        indexingService.indexDocument(event.documentId(), searchDoc);

        statusClient.updateStatus(event.documentId(), "READY");
        log.info("Successfully indexed document {}", event.documentId());
    }

    @Recover
    public void recover(DocumentUploadedEvent event, Exception e) {
        log.error("Failed to index document {} after retries: {}", event.documentId(), e.getMessage(), e);
        try {
            statusClient.updateStatus(event.documentId(), "FAILED");
        } catch (Exception statusEx) {
            log.error("Failed to update status to FAILED for document {}: {}", event.documentId(), statusEx.getMessage(), statusEx);
        }
    }

}
