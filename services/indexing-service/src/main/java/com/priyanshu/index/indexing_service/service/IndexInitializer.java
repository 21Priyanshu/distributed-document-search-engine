package com.priyanshu.index.indexing_service.service;

import java.io.StringReader;

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

        String indexName = "documents_index_v2";

        boolean exists = client.indices().exists(e -> e.index(indexName)).value();
        if (exists) return;

        client.indices().create(c -> c
            .index(indexName)
            .withJson(new StringReader(Constants.INDEX_MAPPING_JSON))
        );

        System.out.println("documents_index_v2 created");
    }
}

