package com.evho.usonly.domain.archive.repository;

import com.evho.usonly.domain.archive.model.Archive;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class ArchiveRepository {

    public static final String COLLECTION_NAME = "archives";

    public Archive save(Archive archive) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        // ID가 없으면 에러나니까, 저장할 때 ID를 문서 이름으로 씁니다.
        db.collection(COLLECTION_NAME).document(archive.getId()).set(archive);
        return archive;
    }

    public List<Archive> findAllByCoupleId(String coupleId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        var query = db.collection(COLLECTION_NAME).whereEqualTo("coupleId", coupleId)
                .orderBy("visitDate", Query.Direction.DESCENDING)
                .get()
                .get();

        return query.toObjects(Archive.class);
    }
}