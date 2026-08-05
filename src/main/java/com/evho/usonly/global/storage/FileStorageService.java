package com.evho.usonly.global.storage;

import com.evho.usonly.domain.couple.repository.CoupleRepository;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import com.evho.usonly.global.utils.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final CoupleRepository coupleRepository;

    @Value("${custom.file.dir}")
    private String uploadDir;

    @Value("${custom.file.domain}")
    private String baseUrl;

    // 커플당 저장 용량 한도(바이트). 기본 5GB.
    @Value("${custom.file.max-couple-bytes:5368709120}")
    private long maxCoupleBytes;

    public record StoreResult(String url, String thumbnailUrl) {}

    // 커플이 공유하는 콘텐츠(채팅 첨부, 앨범 등) 저장 - uploads/{coupleId}/ 하위에 저장하고 용량 카운터에 반영
    public String store(Long coupleId, MultipartFile file, Set<String> allowedExtensions) throws IOException {
        checkQuota(coupleId, file.getSize());
        String relativePath = coupleId + "/" + FileUploadUtil.generateSafeFilename(file, allowedExtensions);
        File dest = new File(uploadDir + relativePath);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        addUsage(coupleId, dest.length());
        return baseUrl + relativePath;
    }

    public String storeImage(Long coupleId, MultipartFile file) throws IOException {
        return store(coupleId, file, FileUploadUtil.imageExtensions());
    }

    public StoreResult storeImageWithThumbnail(Long coupleId, MultipartFile file) throws IOException {
        checkQuota(coupleId, file.getSize());
        String fileName = FileUploadUtil.generateSafeFilename(file, FileUploadUtil.imageExtensions());
        String relativePath = coupleId + "/" + fileName;
        File dest = new File(uploadDir + relativePath);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        String originalUrl = baseUrl + relativePath;
        long totalBytes = dest.length();

        try {
            String thumbRelativePath = coupleId + "/thumb_" + fileName;
            File thumbDest = new File(uploadDir + thumbRelativePath);
            Thumbnails.of(dest)
                    .size(400, 400)
                    .keepAspectRatio(true)
                    .outputQuality(0.65)
                    .toFile(thumbDest);
            totalBytes += thumbDest.length();
            addUsage(coupleId, totalBytes);
            return new StoreResult(originalUrl, baseUrl + thumbRelativePath);
        } catch (Exception e) {
            addUsage(coupleId, totalBytes);
            return new StoreResult(originalUrl, originalUrl);
        }
    }

    public String storeNoteImage(Long coupleId, MultipartFile file) throws IOException {
        checkQuota(coupleId, file.getSize());
        String relativePath = coupleId + "/notes/" + FileUploadUtil.generateSafeFilename(file, FileUploadUtil.imageExtensions());
        File dest = new File(uploadDir + relativePath);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        addUsage(coupleId, dest.length());
        return baseUrl + relativePath;
    }

    // 프로필 사진은 멤버 개인 자산이라 커플 용량 한도 대상이 아님. 커플 폴더 밖(profiles/)에 별도 저장.
    public String storeProfileImage(MultipartFile file) throws IOException {
        String relativePath = "profiles/" + FileUploadUtil.generateSafeFilename(file, FileUploadUtil.imageExtensions());
        File dest = new File(uploadDir + relativePath);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        return baseUrl + relativePath;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void delete(String url) {
        if (url == null || url.isEmpty()) return;
        String relativePath = url.replace(baseUrl, "");
        File file = new File(uploadDir + relativePath);
        if (!file.exists()) return;

        long size = file.length();
        if (!file.delete()) {
            log.warn("파일 삭제 실패: {}", file.getAbsolutePath());
            return;
        }

        // 커플 폴더({coupleId}/...) 소속 파일이면 용량 카운터에서 차감. profiles/ 등은 대상 아님.
        Long coupleId = extractCoupleId(relativePath);
        if (coupleId != null && size > 0) {
            coupleRepository.addStorageUsedBytes(coupleId, -size);
        }
    }

    // 회원탈퇴 등으로 커플 공유 데이터를 통째로 정리할 때 사용. couple row 자체도 곧 삭제되므로
    // 카운터는 따로 차감하지 않음.
    public void deleteCoupleFolder(Long coupleId) {
        File dir = new File(uploadDir + coupleId + "/");
        if (!dir.exists()) return;
        if (!FileSystemUtils.deleteRecursively(dir)) {
            log.warn("커플 폴더 삭제 실패: {}", dir.getAbsolutePath());
        }
    }

    private void checkQuota(Long coupleId, long incomingBytes) {
        long used = coupleRepository.findById(coupleId)
                .map(c -> c.getStorageUsedBytes())
                .orElse(0L);
        if (used + incomingBytes > maxCoupleBytes) {
            throw new CustomException(ErrorCode.STORAGE_QUOTA_EXCEEDED);
        }
    }

    private void addUsage(Long coupleId, long delta) {
        coupleRepository.addStorageUsedBytes(coupleId, delta);
    }

    private Long extractCoupleId(String relativePath) {
        int slash = relativePath.indexOf('/');
        if (slash <= 0) return null;
        try {
            return Long.parseLong(relativePath.substring(0, slash));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
