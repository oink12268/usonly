package com.evho.usonly.domain.member.repository;

import com.evho.usonly.domain.member.model.User;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

@Repository
public class MemberRepository {

    public static final String COLLECTION_NAME = "users";

    public User save(User user) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // users 컬렉션에 user.getUid() 라는 이름의 문서로 저장
        // set()은 덮어쓰기(Create or Update)입니다.
        db.collection(COLLECTION_NAME).document(user.getUid()) .set(user).get();

        return user;
    }

    // 초대 코드로 유저 찾기 (Where 조건 검색)
    public User findByInviteCode(String code) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // select * from users where myCode = 'CODE' limit 1
        QuerySnapshot query = db.collection(COLLECTION_NAME)
                .whereEqualTo("myCode", code)
                .get()
                .get(); // Future 대기

        if (query.isEmpty()) {
            return null; // 못 찾음
        }

        // 찾은 문서 하나를 User 객체로 변환해서 리턴
        return query.getDocuments().get(0).toObject(User.class);
    }

    // 내 정보 가져오기 (UID로 찾기)
    public User findByUid(String uid) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot doc = db.collection(COLLECTION_NAME).document(uid).get().get();

        if (!doc.exists()) return null;
        return doc.toObject(User.class);
    }
}