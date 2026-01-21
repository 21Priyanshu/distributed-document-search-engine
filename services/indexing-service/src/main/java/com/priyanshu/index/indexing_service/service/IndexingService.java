package com.priyanshu.index.indexing_service.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.priyanshu.index.indexing_service.entity.Document;
import com.priyanshu.index.indexing_service.entity.DocumentStatus;
import com.priyanshu.index.indexing_service.repository.DocumentRepository;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private final DocumentRepository repository;
    private final MinioClient minioClient;
    private final ElasticsearchService elasticsearchService;

    @Value("${minio.bucket}")
    private String bucket;

    @Transactional
    public void indexDocument(UUID documentId) {

        Document doc = repository.findById(documentId)
                .orElseThrow();

        try {
            doc.setStatus(DocumentStatus.INDEXING);
            repository.save(doc);

            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(doc.getStoragePath())
                    .build()
            );

            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            elasticsearchService.index(doc, content);

            doc.setStatus(DocumentStatus.READY);
            repository.save(doc);

        } catch (Exception e) {
            doc.setStatus(DocumentStatus.FAILED);
            repository.save(doc);
            throw new RuntimeException(e);
        }
    }
}
