package com.priyanshu.documents.document_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import main.java.com.priyanshu.documents.common.events.DocumentUploadedEvent;


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

