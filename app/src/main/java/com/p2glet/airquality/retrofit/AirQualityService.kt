package com.p2glet.airquality.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author CHOI
 * @email vviian.2@gmail.com
 * @created 2022-06-09
 * @desc
 */
interface AirQualityService {
    @GET("nearest_city")
    fun getAirQualityData(@Query("lat") lat : String, @Query("lon") lon : String, @Query("key") key : String) : Call<AirQualityResponse>
}