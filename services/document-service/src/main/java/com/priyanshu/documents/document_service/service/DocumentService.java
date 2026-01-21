package com.priyanshu.documents.document_service.service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.priyanshu.documents.document_service.dto.UploadDocResponse;
import com.priyanshu.documents.document_service.entity.Document;
import com.priyanshu.documents.document_service.entity.DocumentStatus;
import com.priyanshu.documents.document_service.repository.DocumentRepository;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final MinioClient minioClient;
    private final DocumentEventProducer producer;

    @Value("${minio.bucket}")
    private String bucket;

    public DocumentService(DocumentRepository repository,
                           MinioClient minioClient,
                           DocumentEventProducer producer) {
        this.repository = repository;
        this.minioClient = minioClient;
        this.producer = producer;
    }

    public UploadDocResponse upload(MultipartFile file, String title, String description, List<String> tags) throws Exception {

    // 1. Upload to MinIO (temporary object name)
    String objectName = UUID.randomUUID() + "-" + file.getOriginalFilename();

   minioClient.putObject( PutObjectArgs.builder() .bucket(bucket) .object(objectName) .stream(file.getInputStream(), file.getSize(), -1) .contentType(file.getContentType()) .build() );

    // 2. Save metadata
    Document doc = new Document();
    doc.setTitle(title);
    doc.setDescription(description);
    doc.setFileName(file.getOriginalFilename());
    doc.setFileSize(file.getSize());
    doc.setContentType(file.getContentType());
    doc.setStoragePath(objectName);
    doc.setStatus(DocumentStatus.UPLOADED);

    Document saved = repository.save(doc);   // DB assigns ID

    // 3. Kafka event
    producer.publishDocumentUploaded(saved.getId().toString());

    return new UploadDocResponse(
        saved.getId(),
        "UPLOADED",
        "Document uploaded successfully"
    );

    }

    public Document getDocument(UUID documentId) {
        return repository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public InputStream downloadFile(String storagePath) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(storagePath)
                        .build()
        );
    }
}

