package com.p2glet.airquality.favorite

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.p2glet.airquality.LocationProvider
import com.p2glet.airquality.MainActivity
import com.p2glet.airquality.R
import com.p2glet.airquality.databinding.ActivityFavoriteLocationBinding
import com.p2glet.airquality.retrofit.AirQualityResponse
import com.p2glet.airquality.retrofit.AirQualityService
import com.p2glet.airquality.retrofit.RetrofitConnection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class FavoriteLocation : AppCompatActivity() {
    lateinit var binding : ActivityFavoriteLocationBinding

    private val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    val db = FirebaseFirestore.getInstance()

    lateinit var locationProvider : LocationProvider

    var latitude = 0.0
    var longitude = 0.0

    var favorite_click = false

    var mInterstitialAd : InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        favorite_click = intent.getBooleanExtra("bool", false)

        updateUI()
        setRefreshButton()
        setBack()
        setBannerAds()

        setFavorite()
        addFavorite()
        FavoriteClick()
    }

    override fun onResume() {
        super.onResume()
        setInterstitialAds()
    }

    private fun setBannerAds(){
        MobileAds.initialize(this);
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        binding.adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("ads log","?????? ????????? ?????????????????????.")
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                Log.d("ads log","?????? ????????? ?????? ??????????????????. ${adError.responseInfo} ${adError.code}")
            }

            override fun onAdOpened() {
                Log.d("ads log","?????? ????????? ???????????????.")
            }

            override fun onAdClicked() {
                Log.d("ads log","?????? ????????? ??????????????????.")
            }

            override fun onAdClosed() {
                Log.d("ads log", "?????? ????????? ???????????????.")
            }
        }
    }

    private fun setInterstitialAds(){
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-3610848843940754/9052433671", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ads log", "?????? ????????? ?????? ??????????????????. ${adError.responseInfo}")
                setTestInterstitialAds()
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("ads log", "?????? ????????? ?????????????????????.")
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun setTestInterstitialAds(){
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ads log", "????????? ?????? ????????? ?????? ??????????????????. ${adError.responseInfo}")
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("ads log", "????????? ?????? ????????? ?????????????????????.")
                mInterstitialAd = interstitialAd
            }
        })
    }
    private fun setFavorite() {
        if (favorite_click) {
            binding.addFavorite.setBackgroundResource(R.drawable.ic_star_clicked)
        }
    }

    private fun addFavorite() {
        binding.favBtn.setOnClickListener {
            val intent = Intent(this@FavoriteLocation, FavoriteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setBack() {
        binding.backBtn.setOnClickListener {
            val intent = Intent(this@FavoriteLocation, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setRefreshButton() {
        binding.btnRefresh.setOnClickListener {
            updateUI()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        locationProvider = LocationProvider(this@FavoriteLocation)

        //????????? ?????? ????????? ?????????
        if (latitude == 0.0 || longitude == 0.0) {
            latitude = intent.getDoubleExtra("latitude",0.0)
            longitude = intent.getDoubleExtra("longitude",0.0)
        }

        if (latitude != 0.0 || longitude != 0.0) {
//             1. ?????? ????????? ???????????? UI ????????????
            val address = getCurrentAddress(latitude, longitude)
            address?.let {
                binding.tvLocationTitle.text = it.thoroughfare
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}"
            }
            getAirQualityData(latitude, longitude)
            // 2. ?????? ???????????? ????????? ???????????? UI ????????????
        } else {
            Toast.makeText(this@FavoriteLocation, "??????, ?????? ????????? ????????? ??? ???????????????. ??????????????? ???????????????.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAirQualityData(latitude: Double, longitude: Double) {
        val retrofitAPI = RetrofitConnection.getInstance().create(AirQualityService::class.java)

        retrofitAPI.getAirQualityData(latitude.toString(), longitude.toString(), "5e42faae-d8c8-4db0-b4a6-46f20a4899f5").enqueue(object :
            Callback<AirQualityResponse> {
            override fun onResponse(
                call : Call<AirQualityResponse>, response: Response<AirQualityResponse>
            ) {
                // ???????????? response ??? ????????? UI ????????????
                if (response.isSuccessful) {
                    Toast.makeText(this@FavoriteLocation, "?????? ?????? ???????????? ??????.", Toast.LENGTH_SHORT).show()
                    // response.body()??? null ??? ????????? updateAirUI()
                    response.body()?.let { updateAirUI(it) }
                } else {
                    Toast.makeText(this@FavoriteLocation, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@FavoriteLocation, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateAirUI(airQualityData : AirQualityResponse) {
        val pollutionData = airQualityData.data.current.pollution
        binding.tvCount.text = pollutionData.aqius.toString()

        val dateTime = ZonedDateTime.parse(pollutionData.ts).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
        val dateFormatter : DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        binding.tvCheckTime.text = dateTime.format(dateFormatter).toString()

        when (pollutionData.aqius) {
            in 0..50 -> {
                binding.tvTitle.text = "??????"
                binding.imgBg.setImageResource(R.drawable.bg_good)
            }
            in 51..150 -> {
                binding.tvTitle.text = "??????"
                binding.imgBg.setImageResource(R.drawable.bg_soso)
            }
            in 151..200 -> {
                binding.tvTitle.text = "??????"
                binding.imgBg.setImageResource(R.drawable.bg_bad)
            }
            else -> {
                binding.tvTitle.text = "?????? ??????"
                binding.imgBg.setImageResource(R.drawable.bg_worst)
            }
        }
    }

    fun getCurrentAddress(latitude : Double, longitude : Double) : Address? {
        val geocoder = Geocoder(this, Locale.getDefault())
        // Address ????????? ????????? ????????? ?????? ????????? ????????? ????????????.
        // android.location.Address ????????? ??????.

        val addresses : List<Address>? = try {
            // Geocoder ????????? ???????????? ????????? ??????????????? ???????????? ???????????????.
            geocoder.getFromLocation(latitude, longitude, 7)
        } catch (ioException : IOException) {
            Toast.makeText(this, "???????????? ????????? ?????????????????????.", Toast.LENGTH_LONG).show()
            return null
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(this, "????????? ??????, ?????? ?????????.", Toast.LENGTH_LONG).show()
            return null
        }

        // ????????? ???????????? ????????? ???????????? ?????? ??????
        if (addresses == null || addresses.size == 0) {
            Toast.makeText(this, "????????? ???????????? ???????????????.", Toast.LENGTH_LONG).show()
            return null
        }
        val address : Address = addresses[0]
        return address
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size) {
            var checkResult = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    checkResult = false
                    break
                }
            }
            if (checkResult) {
                updateUI()
            } else {
                Toast.makeText(this@FavoriteLocation, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    fun FavoriteClick() {
            // ???????????? ??????
            binding.addFavorite.setOnClickListener {
                if (!favorite_click) {
                    val builder = AlertDialog.Builder(this)
                    val tvName = TextView(this)
                    tvName.text = "??????"
                    val etName = EditText(this)
                    etName.isSingleLine = true
                    val mLayout = LinearLayout(this)
                    mLayout.orientation = LinearLayout.VERTICAL
                    mLayout.setPadding(15)
                    mLayout.addView(tvName)
                    mLayout.addView(etName)
                    builder.setView(mLayout)

                    builder.setTitle("??????????????? ?????????????????????????")
                    builder.setPositiveButton("??????") { dialog, which ->
                        val data = hashMapOf(
                            "name" to etName.text.toString(),
                            "location" to binding.tvLocationTitle.text as String,
                            "favorite" to true,
                            "lat" to latitude,
                            "lng" to longitude
                        )
                        db.collection("Favorite_Place")
                            .document("$latitude+$longitude")
                            .set(data)
                            .addOnSuccessListener {
                                // ??????
                                Toast.makeText(this, "??????????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { exception ->
                                Log.w("MainActivity", "Error getting documents: $exception")
                            }
                    }
                    builder.setNegativeButton("??????") { dialog, which ->
                    }
                    builder.show()
                } else {
                    // ???????????? ??????
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("??????????????? ?????????????????????????")
                        builder.setPositiveButton("??????") { dialog, which ->
                            db.collection("Favorite_Place")
                                    //??????
                                // .document("$latitude+$longitude").update("favorite", false)
                                .document("$latitude+$longitude")
                                .delete()
                                .addOnCompleteListener {
                                    // ??????
                                    Toast.makeText(this, "??????????????? ?????????????????????.", Toast.LENGTH_SHORT)
                                        .show()
                                    // ???????????? ??????
                                    val intent = Intent(this@FavoriteLocation, MainActivity::class.java)
                                    startActivity(intent)
                                }
                                .addOnFailureListener { exception ->
                                    Log.w("MainActivity", "Error getting documents: $exception")
                                }
                        }
                        builder.setNegativeButton("??????") { dialog, which ->
                            finish()
                        }
                        builder.show()
                    }
                }

    }
}