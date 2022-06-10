package com.example.airquality.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * @author CHOI
 * @email vviian.2@gmail.com
 * @created 2022-06-09
 * @desc
 */
class RetrofitConnection {

    //singleton pattern
    companion object {
        private const val BASE_URL = "https://api.airvisual.com/v2/"
        private var INSTANCE : Retrofit?= null

        fun getInstance() : Retrofit {
            if (INSTANCE == null) {
                INSTANCE = Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
            }
            return INSTANCE!!
        }
    }
}