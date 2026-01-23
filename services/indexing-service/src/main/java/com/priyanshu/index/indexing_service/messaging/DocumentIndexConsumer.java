package com.priyanshu.index.indexing_service.messaging;
import java.io.InputStream;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.priyanshu.index.indexing_service.dto.SearchDocument;
import com.priyanshu.index.indexing_service.service.DocumentStatusClient;
import com.priyanshu.index.indexing_service.service.IndexingService;

import com.priyanshu.documents.common.events.DocumentUploadedEvent;

@Component
public class DocumentIndexConsumer {
    private final DocumentStatusClient statusClient;
    private final IndexingService indexingService;

    public DocumentIndexConsumer(DocumentStatusClient statusClient, IndexingService indexingService) {
        this.statusClient = statusClient;
        this.indexingService = indexingService;
    }

    @KafkaListener(topics = "document_uploaded", groupId = "search-indexer")
    public void consume(DocumentUploadedEvent event) throws Exception {

        try {
            statusClient.updateStatus(event.documentId(), "INDEXING");

            var stat = indexingService.validateFileContent(event);

             if (stat.size() == 0) {
                statusClient.updateStatus(event.documentId(), "FAILED");
                return;
            }

            InputStream fileStream = indexingService.fetchFile(event.storagePath());

            String content = indexingService.extractText(fileStream);

            if (content == null || content.trim().isEmpty()) {
                statusClient.updateStatus(event.documentId(), "FAILED");
                // log.warn("No extractable content for document {}", event.documentId());
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

        } catch (Exception e) {
            statusClient.updateStatus(event.documentId(), "FAILED");
            throw e;
        }
    }

}
