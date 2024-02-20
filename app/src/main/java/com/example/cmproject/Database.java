package com.example.cmproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.util.ArrayList;

public class Database extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CM_Project";
    private static final String POST_TABLE_NAME = "Post";
    private static final String PROFILE_POST_TABLE_NAME = "ProfilePost";
    private Context context;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + POST_TABLE_NAME + " (name TEXT, username TEXT, hasProfileImg INTEGER, post TEXT)");
        db.execSQL("CREATE TABLE " + PROFILE_POST_TABLE_NAME + " (name TEXT, username TEXT, hasProfileImg INTEGER, post TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + POST_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PROFILE_POST_TABLE_NAME);
        onCreate(db);
    }

    public void insertPosts(ArrayList<Post> postArrayList) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + POST_TABLE_NAME);

        for(Post post : postArrayList) {
            ContentValues values = new ContentValues();
            values.put("name", post.getUser().getName());
            values.put("username", post.getUser().getUsername());
            if (post.getUser().isHasProfileImg()) {
                values.put("hasProfileImg", 1);
            } else {
                values.put("hasProfileImg", 0);
            }
            values.put("post", post.getText());
            db.insert(POST_TABLE_NAME, null, values);
        }
    }

    public ArrayList<Post> getPosts() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + POST_TABLE_NAME, null);
        cursor.moveToFirst();
        ArrayList<Post> postsArrayList = new ArrayList<>();

        if (cursor.getCount() > 0) {
            do {
                String name = cursor.getString(0);
                String username = cursor.getString(1);
                int hasProfileImgString = cursor.getInt(2);
                boolean hasProfileImg = false;
                if(hasProfileImgString == 0) {
                    hasProfileImg = false;
                }
                else if(hasProfileImgString == 1) {
                    hasProfileImg = true;
                }
                String postText = cursor.getString(3);
                User user = new User(name, username, "", hasProfileImg);
                Post post = new Post(user, postText);
                postsArrayList.add(post);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return postsArrayList;
    }

    public void insertProfilePosts(ArrayList<Post> postArrayList) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + PROFILE_POST_TABLE_NAME);

        for(Post post : postArrayList) {
            ContentValues values = new ContentValues();
            values.put("name", post.getUser().getName());
            values.put("username", post.getUser().getUsername());
            if (post.getUser().isHasProfileImg()) {
                values.put("hasProfileImg", 1);
            } else {
                values.put("hasProfileImg", 0);
            }
            values.put("post", post.getText());
            db.insert(PROFILE_POST_TABLE_NAME, null, values);
        }
    }

    public ArrayList<Post> getProfilePosts() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + PROFILE_POST_TABLE_NAME, null);
        cursor.moveToFirst();
        ArrayList<Post> postsArrayList = new ArrayList<>();

        if (cursor.getCount() > 0) {
            do {
                String name = cursor.getString(0);
                String username = cursor.getString(1);
                int hasProfileImgString = cursor.getInt(2);
                boolean hasProfileImg = false;
                if(hasProfileImgString == 0) {
                    hasProfileImg = false;
                }
                else if(hasProfileImgString == 1) {
                    hasProfileImg = true;
                }
                String postText = cursor.getString(3);
                User user = new User(name, username, "", hasProfileImg);
                Post post = new Post(user, postText);
                postsArrayList.add(post);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return postsArrayList;
    }
}
