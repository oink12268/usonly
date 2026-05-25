package com.evho.usonly.global.utils;

import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

public class FileUploadUtil {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "heic", "heif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "avi", "mkv");

    /**
     * 안전한 파일명 생성: UUID + 검증된 확장자
     * - 경로 순회(Path Traversal) 방지
     * - 허용되지 않은 확장자 차단
     */
    public static String generateSafeFilename(MultipartFile file, Set<String> allowedExtensions) {
        String original = file.getOriginalFilename();
        String ext = extractExtension(original);

        if (!allowedExtensions.contains(ext)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE, "허용되지 않는 파일 형식입니다: " + ext);
        }
        return UUID.randomUUID() + "." + ext;
    }

    public static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        // / 와 \ 중 마지막 경로 구분자 이후의 순수 파일명만 추출 (Path Traversal 방지)
        int lastSep = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = lastSep >= 0 ? filename.substring(lastSep + 1) : filename;
        int dotIdx = name.lastIndexOf('.');
        return dotIdx >= 0 ? name.substring(dotIdx + 1).toLowerCase() : "";
    }

    public static Set<String> imageExtensions() {
        return IMAGE_EXTENSIONS;
    }

    public static Set<String> imageAndVideoExtensions() {
        Set<String> all = new java.util.HashSet<>(IMAGE_EXTENSIONS);
        all.addAll(VIDEO_EXTENSIONS);
        return all;
    }

    public static Set<String> fileExtensions() {
        return Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "zip");
    }
}
