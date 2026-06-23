package com.example.kolokvijum2java;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ClientUtils {
    public static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://dummy-json.mock.beeceptor.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public static PostoviService postoviService = retrofit.create(PostoviService.class);
}

