package com.priyanshu.documents.document_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.priyanshu.documents.document_service.dto.DocumentUploadedEvent;

@Service
public class DocumentEventProducer {

    private final KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;

    public DocumentEventProducer(KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDocumentUploaded(DocumentUploadedEvent documentId) {
        kafkaTemplate.send("document_uploaded", documentId);
    }
}

