package com.priyanshu.documents.search_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.priyanshu.documents.search_service.dto.SearchResponse;
import com.priyanshu.documents.search_service.dto.SearchResultItem;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;

@Service
public class DocumentSearchService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSearchService.class);

    private final ElasticsearchClient client;

    public DocumentSearchService(ElasticsearchClient client) {
        this.client = client;
        logger.info("DocumentSearchService initialized with Elasticsearch client");
    }

    public SearchResponse search(String query, int page, int size, String userId) throws IOException {
        logger.debug("Performing search - query: '{}', page: {}, size: {}, userId: {}", query, page, size, userId);

        int from = page * size;

        long startTime = System.nanoTime();
        try {
            var response = client.search(s -> s
                    .index("documents_index_v2")
                    .from(from)
                    .size(size)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m
                                .multiMatch(mm -> mm
                                    .fields("title", "description", "content")
                                    .query(query)
                                )
                            )
                            .filter(f -> f
                                    .term(t -> t
                                        .field("ownerId")
                                        .value(userId)
                                    )
                    )
                        )
                    ).sort(srt -> srt.field(f -> f.field("createdAt").order(SortOrder.Desc))),
                    Map.class
            );

            long searchTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            List<SearchResultItem> results = response.hits().hits().stream()
                    .map(hit -> {
                        Map<String, Object> src = hit.source();

                        return new SearchResultItem(
                                hit.id(),
                                (String) src.get("title"),
                                (String) src.get("description"),
                                String.valueOf(src.get("createdAt"))   // safer
                        );
                    })
                    .toList();

            long totalHits = response.hits().total().value();
            logger.info("Search completed - query: '{}', totalHits: {}, returned: {}, searchTime: {}ms",
                       query, totalHits, results.size(), searchTimeMs);

            if (logger.isDebugEnabled()) {
                logger.debug("Search results details - query: '{}', hits: {}, took: {}ms",
                           query, totalHits, response.took());
            }

            return new SearchResponse(totalHits, results);

        } catch (IOException e) {
            long searchTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            logger.error("Elasticsearch search failed - query: '{}', userId: {}, searchTime: {}ms, error: {}",
                        query, userId, searchTimeMs, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            long searchTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            logger.error("Unexpected error during search - query: '{}', userId: {}, searchTime: {}ms, error: {}",
                        query, userId, searchTimeMs, e.getMessage(), e);
            throw new RuntimeException("Search operation failed", e);
        }
    }
}

