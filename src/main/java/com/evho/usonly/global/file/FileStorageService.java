package com.evho.usonly.global.file;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileStorageService {
    // 파일을 받아서 저장하고, 접근 가능한 URL(String)을 돌려준다.
    String uploadFile(MultipartFile file, String path) throws IOException;
}