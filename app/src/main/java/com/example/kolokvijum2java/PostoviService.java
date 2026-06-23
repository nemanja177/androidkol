package com.example.kolokvijum2java;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface PostoviService {

    @Headers({
            "User-Agent: Mobile-Android",
            "Content-Type:application/json"
    })
    @GET("posts")
    Call<ArrayList<Postovi>> getAll();

}
