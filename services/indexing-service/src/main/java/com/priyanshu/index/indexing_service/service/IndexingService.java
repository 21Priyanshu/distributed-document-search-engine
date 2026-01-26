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
import com.priyanshu.documents.common.events.DocumentUploadedEvent;

@Service
@RequiredArgsConstructor
public class IndexingService {

     private final String secret = "dummuy_secret_key_for_jwt_signing_purposes_only";

    private final MinioClient minioClient;
    private final ElasticsearchClient client;

    @Value("${minio.bucket}")
    private String bucket;

    public StatObjectResponse validateFileContent(DocumentUploadedEvent event){
        StatObjectResponse stat = null;
        try {
            stat=  minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket("documents")
                    .object(event.storagePath())
                    .build()
            );
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                | IllegalArgumentException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

       return stat;
    }

    public InputStream fetchFile(String storagePath) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket("documents")
                .object(storagePath)
                .build()
        );
    }

    public String extractText(InputStream stream) throws Exception {
        Tika tika = new Tika();
        return tika.parseToString(stream);
    }

    public void indexDocument(String documentId, SearchDocument doc) throws IOException {

        client.index(i -> i
            .index("documents_index_v2")
            .id(documentId)
            .document(doc)
        );
    }

    public String generateServiceToken() {

        return Jwts.builder()
            .setSubject("indexing-service")
            .claim("type", "service")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }
}
