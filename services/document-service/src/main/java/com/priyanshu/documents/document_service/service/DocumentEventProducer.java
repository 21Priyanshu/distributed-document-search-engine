package com.priyanshu.documents.document_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.priyanshu.documents.DocumentDeletedEvent;
import com.priyanshu.documents.common.events.DocumentUploadedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(DocumentEventProducer.class);
    private final KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;
    private final KafkaTemplate<String, DocumentDeletedEvent> kafkaTemplateDeleted;

    public DocumentEventProducer(KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate,
                                 KafkaTemplate<String, DocumentDeletedEvent> kafkaTemplateDeleted) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplateDeleted = kafkaTemplateDeleted;
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

    public void publishDocumentDeleted(String documentId) {
        try {
            kafkaTemplateDeleted.send("document_deleted", documentId, new DocumentDeletedEvent(documentId));
            logger.info("Published DocumentDeletedEvent for document: {}", documentId);
        } catch (Exception e) {
            logger.error("Failed to publish DocumentDeletedEvent: {}", documentId, e);
            throw new RuntimeException("Failed to publish document deleted event", e);
        }
    }
}