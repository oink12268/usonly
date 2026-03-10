package com.evho.usonly.global.storage;

import com.evho.usonly.global.utils.FileUploadUtil;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@Service
public class FileStorageService {

    @Value("${custom.file.dir}")
    private String uploadDir;

    @Value("${custom.file.domain}")
    private String baseUrl;

    public record StoreResult(String url, String thumbnailUrl) {}

    public String store(MultipartFile file, Set<String> allowedExtensions) throws IOException {
        String fileName = FileUploadUtil.generateSafeFilename(file, allowedExtensions);
        File dest = new File(uploadDir + fileName);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        return baseUrl + fileName;
    }

    public String storeImage(MultipartFile file) throws IOException {
        return store(file, FileUploadUtil.imageExtensions());
    }

    public StoreResult storeImageWithThumbnail(MultipartFile file) throws IOException {
        String fileName = FileUploadUtil.generateSafeFilename(file, FileUploadUtil.imageExtensions());
        File dest = new File(uploadDir + fileName);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        String originalUrl = baseUrl + fileName;

        try {
            String thumbFileName = "thumb_" + fileName;
            Thumbnails.of(dest)
                    .size(400, 400)
                    .keepAspectRatio(true)
                    .outputQuality(0.65)
                    .toFile(new File(uploadDir + thumbFileName));
            return new StoreResult(originalUrl, baseUrl + thumbFileName);
        } catch (Exception e) {
            return new StoreResult(originalUrl, originalUrl);
        }
    }

    public void delete(String url) {
        if (url == null || url.isEmpty()) return;
        String fileName = url.replace(baseUrl, "");
        File file = new File(uploadDir + fileName);
        if (file.exists()) {
            file.delete();
        }
    }
}
