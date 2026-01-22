package com.priyanshu.index.indexing_service.messaging;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.priyanshu.index.indexing_service.entity.Document;
import com.priyanshu.index.indexing_service.entity.DocumentStatus;
import com.priyanshu.index.indexing_service.repository.DocumentRepository;

import jakarta.transaction.Transactional;
import main.java.com.priyanshu.documents.common.events.DocumentUploadedEvent;

@Component
public class DocumentIndexConsumer {
    private final DocumentRepository repository;

    public DocumentIndexConsumer(DocumentRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "document_uploaded", groupId = "search-indexer")
    @Transactional
    public void handleDocumentUploaded(DocumentUploadedEvent event){
        UUID id = UUID.fromString(event.documentId());

        Document doc = repository.findById(id).orElseThrow();

        // marks the status as indexing
        doc.setStatus(DocumentStatus.INDEXING);

        repository.save(doc);

        // simulating indexing
        try{
            Thread.sleep(500);
            doc.setStatus(DocumentStatus.READY);
        }catch(Exception e){
            doc.setStatus(DocumentStatus.FAILED);
        }

        repository.save(doc);
    }
}
