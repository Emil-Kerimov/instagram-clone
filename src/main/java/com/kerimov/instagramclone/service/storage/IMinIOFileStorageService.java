package com.kerimov.instagramclone.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface IMinIOFileStorageService {
    String upload(MultipartFile file);

    String getFileUrl(String fileName);
}
