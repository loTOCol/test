package com.example.walkingmate.feature.mate.data.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class MateRepository {
    private static MateRepository instance;

    private final CollectionReference mateDataRef;
    private final CollectionReference mateDataListRef;
    private final CollectionReference mateRequestRef;
    private final CollectionReference mateUserRef;
    private final CollectionReference usersRef;
    private final CollectionReference blockListRef;

    private MateRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        mateDataRef = db.collection("matedata");
        mateDataListRef = db.collection("matedatalist");
        mateRequestRef = db.collection("materequest");
        mateUserRef = db.collection("mateuser");
        usersRef = db.collection("users");
        blockListRef = db.collection("blocklist");
    }

    public static synchronized MateRepository getInstance() {
        if (instance == null) {
            instance = new MateRepository();
        }
        return instance;
    }

    public CollectionReference getMateDataRef() {
        return mateDataRef;
    }

    public CollectionReference getMateDataListRef() {
        return mateDataListRef;
    }

    public CollectionReference getMateRequestRef() {
        return mateRequestRef;
    }

    public CollectionReference getMateUserRef() {
        return mateUserRef;
    }

    public CollectionReference getUsersRef() {
        return usersRef;
    }

    public CollectionReference getBlockListRef() {
        return blockListRef;
    }
}

