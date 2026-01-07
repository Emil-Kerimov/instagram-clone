package com.kerimov.instagramclone.service.storage;

import com.kerimov.instagramclone.exceptions.FileStorageServiceException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOFileStorageService implements IMinIOFileStorageService {
    private final S3Client s3Client;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.external.url}")
    private String url;

    @Override
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

            return fileId;
        } catch (IOException e) {
            log.error("Failed to upload file from server.", e);
            throw new FileStorageServiceException("loading file from server is not possible");
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO with url {}/{}. ", url, bucketName, e);
            throw new FileStorageServiceException("loading file to MinIO is not possible");
        }

    }

    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (Exception e) {
            log.error("Failed to delete file with key: {} from MinIO with url {}/{}. ", key, url, bucketName, e);
            throw new FileStorageServiceException("Cant delete file from MinIO");
        }
    }

    @PostConstruct
    public void init(){
        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();
        try{
            s3Client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            try {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();

                s3Client.createBucket(createBucketRequest);
            } catch (Exception ex) {
                throw new FileStorageServiceException("Cannot create bucket");
            }
        } catch (Exception e) {
        throw new FileStorageServiceException("Cannot check bucket exists");}
    }

    @Override
    public String getFileUrl(String fileName) {
        return url+"/"+bucketName+"/"+ fileName;
    }
}
