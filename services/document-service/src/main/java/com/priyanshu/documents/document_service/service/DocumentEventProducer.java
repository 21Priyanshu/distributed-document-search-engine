package com.priyanshu.documents.document_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public DocumentEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDocumentUploaded(String documentId) {
        kafkaTemplate.send("document_uploaded", documentId);
    }
}

