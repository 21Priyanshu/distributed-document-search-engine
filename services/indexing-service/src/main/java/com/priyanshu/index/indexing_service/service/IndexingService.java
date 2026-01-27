package com.priyanshu.index.indexing_service.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.priyanshu.index.indexing_service.dto.SearchDocument;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.priyanshu.documents.common.events.DocumentUploadedEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private final String secret = "dummuy_secret_key_for_jwt_signing_purposes_only";

    private final MinioClient minioClient;
    private final ElasticsearchClient client;

    @Value("${minio.bucket}")
    private String bucket;

    public StatObjectResponse validateFileContent(DocumentUploadedEvent event){
        try {
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket("documents")
                    .object(event.storagePath())
                    .build()
            );
            log.info("Validated file content for document {}: size {}", event.documentId(), stat.size());
            return stat;
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                | IllegalArgumentException | IOException e) {
            log.error("Failed to validate file content for document {}: {}", event.documentId(), e.getMessage(), e);
            throw new RuntimeException("File validation failed for document " + event.documentId(), e);
        }
    }

    public InputStream fetchFile(String storagePath) throws Exception {
        try {
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket("documents")
                    .object(storagePath)
                    .build()
            );
            log.info("Fetched file from storage path: {}", storagePath);
            return stream;
        } catch (Exception e) {
            log.error("Failed to fetch file from storage path {}: {}", storagePath, e.getMessage(), e);
            throw e;
        }
    }

    public String extractText(InputStream stream) throws Exception {
        try {
            Tika tika = new Tika();
            String content = tika.parseToString(stream);
            log.info("Extracted text content, length: {}", content != null ? content.length() : 0);
            return content;
        } catch (Exception e) {
            log.error("Failed to extract text from stream: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void indexDocument(String documentId, SearchDocument doc) throws IOException {
        try {
            client.index(i -> i
                .index("documents_index_v2")
                .id(documentId)
                .document(doc)
            );
            log.info("Indexed document {} successfully", documentId);
        } catch (IOException e) {
            log.error("Failed to index document {}: {}", documentId, e.getMessage(), e);
            throw e;
        }
    }

    public String generateServiceToken() {
        try {
            String token = Jwts.builder()
                .setSubject("indexing-service")
                .claim("type", "service")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
            log.debug("Generated service token");
            return token;
        } catch (Exception e) {
            log.error("Failed to generate service token: {}", e.getMessage(), e);
            throw new RuntimeException("Token generation failed", e);
        }
    }
}
