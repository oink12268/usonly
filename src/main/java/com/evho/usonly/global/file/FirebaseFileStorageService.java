package com.evho.usonly.global.file;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class FirebaseFileStorageService implements FileStorageService {

    // 아까 콘솔에서 본 버킷 이름 (gs:// 뒤에 있는 것)
    // 예: "evho-project.appspot.com" (반드시 본인 프로젝트 ID 확인!)
    private final String bucketName = "evho-2943a.firebasestorage.app";
    // ↑↑↑ 중요: Firebase 콘솔 Storage 탭 상단에 있는 주소를 복사해서 넣으세요 (gs:// 는 뺴고)

    @Override
    public String uploadFile(MultipartFile file, String path) throws IOException {
        Bucket bucket = StorageClient.getInstance().bucket(bucketName);

        // 파일 이름 중복 방지를 위해 UUID 붙이기
        String fileName = path + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 업로드 실행 (바이트 배열로 변환해서 전송)
        Blob blob = bucket.create(fileName, file.getBytes(), file.getContentType());

        // 다운로드 가능한 URL 리턴 (Firebase Storage 기본 URL 패턴)
        // 주의: 이 URL은 '공개' 설정이 되어있거나, 토큰이 있어야 보입니다.
        // 일단은 경로만 리턴하거나, mediaLink를 씁니다.
        return "https://firebasestorage.googleapis.com/v0/b/" + bucketName + "/o/"
                + fileName.replace("/", "%2F") + "?alt=media";
    }
}