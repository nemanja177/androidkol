package com.example.kolokvijum2java;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Kolokvijum.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_POSTS = "posts";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_LINK = "link";
    public static final String COLUMN_COMMENT_COUNT = "comment_count";
//    private SQLiteDatabase database;

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_POSTS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_BODY + " TEXT, " +
                COLUMN_LINK + " TEXT, " +
                COLUMN_COMMENT_COUNT + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        onCreate(db);
    }

    public void save(Postovi post) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, post.getId());
        values.put(COLUMN_USER_ID, post.getUserId());
        values.put(COLUMN_TITLE, post.getTitle());
        values.put(COLUMN_BODY, post.getBody());
        values.put(COLUMN_LINK, post.getLink());
        values.put(COLUMN_COMMENT_COUNT, post.getComment_count());
        db.insert(TABLE_POSTS, null, values);
        db.close();
    }

    public int izbrojPostove() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_POSTS, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public Postovi pronadjiPrvi() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_POSTS + " LIMIT 1", null);

        if (cursor != null && cursor.moveToFirst()) {
            Postovi post = new Postovi();
            post.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            post.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
            post.setBody(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY)));
            post.setLink(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LINK)));
            post.setComment_count(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMMENT_COUNT)));
            cursor.close();
            return post;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public boolean obrisiPrvi() {
        SQLiteDatabase db = this.getWritableDatabase();
        Postovi prviPost = pronadjiPrvi();
        if (prviPost != null) {
            db.delete(TABLE_POSTS, COLUMN_ID + " = ?", new String[]{String.valueOf(prviPost.getId())});
            db.close();
            return true;
        }
        db.close();
        return false;
    }
}


/*

package com.example.kolokvijum2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Kolokvijum.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_POSTS = "posts";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_LINK = "link";
    public static final String COLUMN_COMMENT_COUNT = "comment_count";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_POSTS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_BODY + " TEXT, " +
                COLUMN_LINK + " TEXT, " +
                COLUMN_COMMENT_COUNT + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        onCreate(db);
    }

    // Ubacivanje posta u tabelu
    public void insertPost(Post post) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, post.getId());
        values.put(COLUMN_USER_ID, post.getUserId());
        values.put(COLUMN_TITLE, post.getTitle());
        values.put(COLUMN_BODY, post.getBody());
        values.put(COLUMN_LINK, post.getLink());
        values.put(COLUMN_COMMENT_COUNT, post.getComment_count());
        db.insert(TABLE_POSTS, null, values);
        db.close();
    }

    // Brojanje redova u tabeli da znamo da li je prazna
    public int getPostsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_POSTS, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // Zadatak 6: Uzimanje PRVOG posta koji se fizički nalazi u tabeli (LIMIT 1)
    public Post getFirstPostInTable() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Ne sortiramo po ID-u, nego uzimamo onaj koji je prvi upisan u bazu
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_POSTS + " LIMIT 1", null);

        if (cursor != null && cursor.moveToFirst()) {
            Post post = new Post();
            post.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            post.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
            post.setBody(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY)));
            cursor.close();
            return post;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    // Zadatak 7: Brisanje posta na prvoj poziciji u tabeli
    public boolean deleteFirstPost() {
        SQLiteDatabase db = this.getWritableDatabase();
        Post prviPost = getFirstPostInTable();
        if (prviPost != null) {
            // Brišemo ga preko njegovog ID-ja koji smo izvukli iz prve pozicije
            db.delete(TABLE_POSTS, COLUMN_ID + " = ?", new String[]{String.valueOf(prviPost.getId())});
            db.close();
            return true;
        }
        db.close();
        return false;
    }
}
 */
