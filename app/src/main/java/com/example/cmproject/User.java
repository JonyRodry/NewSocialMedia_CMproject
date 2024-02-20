package com.example.cmproject;

import java.io.Serializable;

public class User implements Serializable {

    private String name;
    private String username;
    private String password;
    private boolean hasProfileImg;

    public User(){}

    public User(String name, String username, String password, boolean hasProfileImg) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.hasProfileImg = hasProfileImg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public boolean isHasProfileImg() {
        return hasProfileImg;
    }

    public void setHasProfileImg(boolean hasProfileImg) {
        this.hasProfileImg = hasProfileImg;
    }
}
