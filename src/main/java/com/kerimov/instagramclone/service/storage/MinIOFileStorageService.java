package com.kerimov.instagramclone.service.storage;

import com.kerimov.instagramclone.exceptions.FileStorageServiceException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinIOFileStorageService {
    private final S3Client s3Client;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.url}")
    private String url;

    public String upload(MultipartFile file){
        try {
            String fileId = UUID.randomUUID().toString();

            PutObjectRequest request = PutObjectRequest
                    .builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody
                    .fromInputStream(file.getInputStream(), file.getSize()));

            return url + "/" + bucketName + "/" + fileId;
        } catch (IOException e) {
            throw new FileStorageServiceException("loading file from server is not possible");
        } catch (Exception e) {
            throw new FileStorageServiceException("loading file to MinIO is not possible");
        }

    }
}
