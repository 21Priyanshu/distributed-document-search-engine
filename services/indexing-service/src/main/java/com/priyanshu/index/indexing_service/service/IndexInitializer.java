package com.priyanshu.index.indexing_service.service;

import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;

@Component
public class IndexInitializer {

    private final ElasticsearchClient client;

    public IndexInitializer(ElasticsearchClient client) {
        this.client = client;
    }

    @PostConstruct
    public void createIndex() throws Exception {

        String indexName = "documents_index";

        boolean exists = client.indices().exists(e -> e.index(indexName)).value();
        if (exists) return;

        client.indices().create(c -> c
            .index(indexName)
            .mappings(m -> m
                .properties("title", p -> p.text(t -> t))
                .properties("description", p -> p.text(t -> t))
                .properties("content", p -> p.text(t -> t))
                .properties("ownerId", p -> p.keyword(k -> k))
                .properties("tags", p -> p.keyword(k -> k))
                .properties("createdAt", p -> p.date(d -> d))
            )
        );

        System.out.println("Elasticsearch index created");
    }
}

