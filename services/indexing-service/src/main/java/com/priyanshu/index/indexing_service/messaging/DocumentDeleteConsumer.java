package com.priyanshu.index.indexing_service.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.priyanshu.documents.DocumentDeletedEvent;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DocumentDeleteConsumer {

    private final ElasticsearchClient client ;

    public DocumentDeleteConsumer(ElasticsearchClient client) {
        this.client = client;
    }

    @KafkaListener(topics = "document_deleted", groupId = "search-indexer-delete")
    public void consume(DocumentDeletedEvent event) throws Exception {

        try {
            client.delete(d -> d
                .index("documents_index_v2")
                .id(event.documentId())
            );

            log.info("Deleted document {} from ES", event.documentId());

        } catch (Exception e) {
            log.error("Failed to delete document {} from ES", event.documentId(), e);
            throw e; // let Kafka retry
        }
    }
}
