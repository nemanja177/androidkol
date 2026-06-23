package com.example.kolokvijum2java;

public class Postovi {

    private int id;
    private int userId;
    private String title;
    private String body;
    private String link;
    private int comment_count;

    public Postovi() {
        super();
    }
    public Postovi(int id, int userId, String title, String body, String link, int comment_count) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.link = link;
        this.comment_count = comment_count;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getComment_count() {
        return comment_count;
    }

    public void setComment_count(int comment_count) {
        this.comment_count = comment_count;
    }

    @Override
    public String toString() {
        return "Postovi{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", link='" + link + '\'' +
                ", comment_count=" + comment_count +
                '}';
    }
}
