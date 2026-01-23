package com.priyanshu.documents.search_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.priyanshu.documents.search_service.dto.SearchResponse;
import com.priyanshu.documents.search_service.dto.SearchResultItem;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@Service
public class DocumentSearchService {

    private final ElasticsearchClient client;

    public DocumentSearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public SearchResponse search(String query, int page, int size) throws IOException {

        int from = page * size;

        var response = client.search(s -> s
                        .index("documents_index_v2")
                        .from(from)
                        .size(size)
                        .query(q -> q
                                .multiMatch(m -> m
                                        .fields("title", "description", "content")
                                        .query(query)
                                )
                        ),
                Map.class
        );

        List<SearchResultItem> results = response.hits().hits().stream()
                .map(hit -> {
                    Map<String, Object> src = hit.source();

                    return new SearchResultItem(
                            hit.id(),
                            (String) src.get("title"),
                            (String) src.get("description"),
                            (String) src.get("createdAt")
                    );
                })
                .toList();

        return new SearchResponse(response.hits().total().value(), results);
    }
}

