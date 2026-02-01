package com.priyanshu.index.indexing_service.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.priyanshu.documents.common.events.DocumentUploadedEvent;

@Service
public class KafkaFailureProducer {

    private final KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;

    public KafkaFailureProducer(KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendToRetry(DocumentUploadedEvent event) {
        kafkaTemplate.send("document_uploaded_retry", event.documentId(), event);
    }

    public void sendToDlq(DocumentUploadedEvent event) {
        kafkaTemplate.send("document_uploaded_dlq", event.documentId(), event);
    }
}
