package com.afeka.expiryalert.logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {

    private String id;
    private String username;
    private String email;
    private String storageID;
    private List<User> sharingList;

    public User() {
    }

    public User(String id, String username, String email, String storageID) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.storageID = storageID;
        this.sharingList = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", storageID='" + storageID + '\'' +
                ", sharingList=" + sharingList +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStorageID() {
        return storageID;
    }

    public void setStorageID(String storageID) {
        this.storageID = storageID;
    }

    public List<User> getSharingList() {
        return sharingList;
    }

    public void setSharingList(List<User> sharingList) {
        this.sharingList = sharingList;
    }
}