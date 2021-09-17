package com.afeka.expiryalert.logic;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DatabaseManager {

    private FirebaseFirestore db;
    private CollectionReference storageRef,usersRef;
    public static User user;
    public static Storage storage;

    public DatabaseManager() {
        db = FirebaseFirestore.getInstance();
        storageRef = db.collection("storage");
        usersRef = db.collection("users");
        user = new User();
        storage = new Storage();
    }

    public interface FirestoreCallback {
        void onStorageCallBack(Storage storage);
        void onUserCallBack(User user);
    }

    public void saveUser(User userToAdd){
        usersRef.document(userToAdd.getId())
                .set(userToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("USER-SAVE", "DocumentSnapshot successfully written user!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("USER-SAVE", "Error writing document user", e);
                    }
                });
    }

    public void saveStorage(Storage storageToAdd) {
       storageRef.document(storageToAdd.getId())
                .set(storageToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("STORAGE-SAVE", "DocumentSnapshot successfully written storage!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("STORAGE-SAVE", "Error writing document storage", e);
                    }
                });
    }

    public void loadStorage(String storageID, FirestoreCallback firestoreCallback){
        storageRef.document(storageID).get().addOnCompleteListener((Task<DocumentSnapshot> task) -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    storage = document.toObject(Storage.class);
                    firestoreCallback.onStorageCallBack(storage);
                } else {
                    Log.d("STORAGE-LOAD", "No such document");
                }
            } else {
                Log.d("STORAGE-LOAD", "get failed with ", task.getException());
            }

        });
    }


    public void loadUser(String userID, FirestoreCallback firestoreCallback) {
        usersRef.document(userID)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("USER-LOAD", "DocumentSnapshot data: " + document.getData());
                        user = document.toObject(User.class);
                        firestoreCallback.onUserCallBack(user);
                    } else {
                        Log.d("USER-LOAD", "No such document");
                        firestoreCallback.onUserCallBack(null);
                    }
                } else {
                    Log.d("USER-LOAD", "get failed with ", task.getException());
                }
            }
        });
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
