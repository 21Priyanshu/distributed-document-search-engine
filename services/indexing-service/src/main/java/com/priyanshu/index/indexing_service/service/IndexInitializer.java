package com.priyanshu.index.indexing_service.service;

import java.io.StringReader;

import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IndexInitializer {

    private final ElasticsearchClient client;

    public IndexInitializer(ElasticsearchClient client) {
        this.client = client;
    }

    @PostConstruct
    public void createIndex() {
        String indexName = "documents_index_v2";

        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                log.info("Index {} already exists", indexName);
                return;
            }

            client.indices().create(c -> c
                .index(indexName)
                .withJson(new StringReader(Constants.INDEX_MAPPING_JSON))
            );

            log.info("Index {} created successfully", indexName);
        } catch (Exception e) {
            log.error("Failed to create index {}: {}", indexName, e.getMessage(), e);
            throw new RuntimeException("Index initialization failed", e);
        }
    }
}

