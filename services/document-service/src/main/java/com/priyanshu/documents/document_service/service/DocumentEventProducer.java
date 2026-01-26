package com.priyanshu.documents.document_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.priyanshu.documents.common.events.DocumentUploadedEvent;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(DocumentEventProducer.class);
    private final KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;

    public DocumentEventProducer(KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDocumentUploaded(DocumentUploadedEvent event) {
        try {
            kafkaTemplate.send("document_uploaded", event);
            logger.info("Published DocumentUploadedEvent: {}", event);
        } catch (Exception e) {
            logger.error("Failed to publish DocumentUploadedEvent: {}", event, e);
            throw new RuntimeException("Failed to publish document uploaded event", e);
        }
    }
}

