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
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import com.priyanshu.documents.common.events.DocumentUploadedEvent;

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

    public UploadDocResponse upload(MultipartFile file, String title, String description, List<String> tags, String userId) throws Exception {

    UUID documentId = UUID.randomUUID();

    // 1. Upload to MinIO (temporary object name)
    String objectName = documentId + "-" + file.getOriginalFilename();

   minioClient.putObject( PutObjectArgs.builder() .bucket(bucket) .object(objectName) .stream(file.getInputStream(), file.getSize(), -1) .contentType(file.getContentType()) .build() );

    // 2. Save metadata
    Document doc = new Document();
    doc.setId(documentId);
    doc.setTitle(title);
    doc.setDescription(description);
    doc.setFileName(file.getOriginalFilename());
    doc.setFileSize(file.getSize());
    doc.setContentType(file.getContentType());
    doc.setStoragePath(objectName);
    doc.setStatus(DocumentStatus.UPLOADED);
    doc.setOwnerId(userId);

    Document saved = repository.save(doc); 

    // 3. Kafka event
    DocumentUploadedEvent event = new DocumentUploadedEvent(
        documentId.toString(),
        objectName,               // storagePath
        title,
        description,
        userId             // ownerId (from auth later)
    );

    producer.publishDocumentUploaded(event);


    return new UploadDocResponse(
        saved.getId(),
        "UPLOADED",
        "Document uploaded successfully"
    );

    }

    public Document getDocument(UUID documentId, String userId) {
        return repository.findByIdAndOwnerId(documentId, userId)
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

    public DocumentStatus getDocumentStatus(UUID documentId, String userId){

        Document doc = repository.findByIdAndOwnerId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return doc.getStatus();
    }

    @Transactional
    public void updateStatus(UUID documentId, DocumentStatus status, String userId, boolean isService) {
        if (!isService) {
            // For user calls, verify ownership
            Document doc = repository.findByIdAndOwnerId(documentId, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found or access denied: " + documentId));
        }

        int updated = repository.updateStatus(documentId, status);

        if (updated == 0) {
            throw new EntityNotFoundException("Document not found: " + documentId);
        }
    }

}

