package com.p2glet.airquality

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.lang.Exception

/**
 * @author CHOI
 * @email vviian.2@gmail.com
 * @created 2022-06-09
 * @desc
 */
class LocationProvider(val context : Context) {
    private var location : Location?= null
    private var locationManager : LocationManager ?= null

    init {
        getLocation()
    }

    private fun getLocation() : Location? {
        try {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            var gpsLocation : Location ?= null
            var networkLocation : Location ?= null
            val isGPSEnabled : Boolean = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled : Boolean = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGPSEnabled && !isNetworkEnabled) {
                return null
            } else {
                // 정밀한 위치
                val hasFineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                // 도시 Block 단위의 위치
                val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                // 위 두개 권한이 없다면 null을 반환
                if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) return null
                // Network 를 통한 위치 파악이 가능한 경우에 위치를 가져옴
                if (isNetworkEnabled) networkLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                // GPS 를 통한 위치 파악이 가능한 경우에 위치를 가져옴
                if (isGPSEnabled) gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (gpsLocation != null && networkLocation != null) {
                    // 두개의 위치가 있다면 정확도 높은 것으로 선택
                    if (gpsLocation.accuracy > networkLocation.accuracy) {
                        location = gpsLocation
                        return gpsLocation
                    } else {
                        location = networkLocation
                        return networkLocation
                    }
                } else {
                    // 가능한 위치 정보가 한개인 경우
                    if (gpsLocation != null) location = gpsLocation
                    if (networkLocation != null) location = networkLocation
                }
            }
        } catch (e : Exception) {
            e.printStackTrace() // 에러 출력
        }
        return location
    }

    // 위도 정보 가져옴
    fun getLocationLatitude() : Double {
        return location?.latitude ?: 0.0    // null이면 0.0 반환
    }

    // 경도 정보 가져옴
    fun getLocationLongitude() : Double {
        return location?.longitude ?: 0.0   // null이면 0.0 반환
    }
}