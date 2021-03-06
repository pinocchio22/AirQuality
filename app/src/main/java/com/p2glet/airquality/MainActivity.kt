package com.p2glet.airquality

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.p2glet.airquality.databinding.ActivityMainBinding
import com.p2glet.airquality.favorite.FavoriteActivity
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


class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    private val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    val db = FirebaseFirestore.getInstance()

    var auth = FirebaseAuth.getInstance()

    lateinit var getGPSPermissionLauncher : ActivityResultLauncher<Intent>
    lateinit var locationProvider : LocationProvider

    var latitude = 0.0
    var longitude = 0.0

    var favorite_click = false

    private var fab_open : Animation ?= null
    private  var fab_close :Animation ?= null
    private var isFabOpen = false

    val startMapActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if ((result?.resultCode ?: 0) == Activity.RESULT_OK) {
            latitude = result?.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            longitude = result?.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            updateUI()
        }
    }

    var mInterstitialAd : InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fab_open = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_open)
        fab_close = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_close)

        binding.fab.setOnClickListener {
            anim()
            setFabLogout()
            setFabMap()
            setFabFav()
        }

        checkAllPermissions()
        updateUI()
        setRefreshButton()
        setBannerAds()
        FavoriteClick()
    }

    override fun onResume() {
        super.onResume()
        setInterstitialAds()
    }

    fun anim() {
        if (isFabOpen) {
            binding.fab.setImageResource(R.drawable.ic_plus)
            binding.fabFav.startAnimation(fab_close)
            binding.fabMap.startAnimation(fab_close)
            binding.fabLogout.startAnimation(fab_close)
            binding.fabFav.isClickable = false
            binding.fabMap.isClickable = false
            binding.fabLogout.isClickable = false
            isFabOpen = false
        } else {
            binding.fab.setImageResource(R.drawable.ic_close)
            binding.fabFav.startAnimation(fab_open)
            binding.fabMap.startAnimation(fab_open)
            binding.fabLogout.startAnimation(fab_open)
            binding.fabFav.isClickable = true
            binding.fabMap.isClickable = true
            binding.fabLogout.isClickable = true
            isFabOpen = true
        }
    }

    fun setFabLogout() {
        binding.fabLogout.setOnClickListener {
            finish()
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            auth.signOut()
        }
    }

    private fun setFabFav() {
        binding.fabFav.setOnClickListener {
            val intent = Intent(this@MainActivity, FavoriteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setFabMap() {
        binding.fabMap.setOnClickListener {
            if (mInterstitialAd != null) {
                mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("ads log", "?????? ????????? ???????????????.")

                        val intent = Intent(this@MainActivity, MapActivity::class.java)
                        intent.putExtra("currentLat", latitude)
                        intent.putExtra("currentLng", longitude)
                        startMapActivityResult.launch(intent)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d("ads log", "?????? ????????? ???????????? ??????????????????.")
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("ads log", "?????? ????????? ??????????????? ???????????????.")
                        mInterstitialAd = null
                    }
                }
                mInterstitialAd!!.show(this@MainActivity)
            } else {
                Log.d("InterstitialAd", "?????? ????????? ???????????? ???????????????.")
                Toast.makeText(this@MainActivity, "?????? ??? ?????? ??????????????????.", Toast.LENGTH_LONG).show()
            }
        }
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

    private fun setRefreshButton() {
        binding.btnRefresh.setOnClickListener {
            updateUI()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        locationProvider = LocationProvider(this@MainActivity)

        //????????? ?????? ????????? ?????????
        if (latitude == 0.0 || longitude == 0.0) {
            latitude = locationProvider.getLocationLatitude()
            longitude = locationProvider.getLocationLongitude()
        }

        if (latitude != 0.0 || longitude != 0.0) {
            // 1. ?????? ????????? ???????????? UI ????????????
            val address = getCurrentAddress(latitude, longitude)
            address?.let {
                binding.tvLocationTitle.text = it.thoroughfare
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}"
            }
            getAirQualityData(latitude, longitude)
            // 2. ?????? ???????????? ????????? ???????????? UI ????????????
        } else {
            Toast.makeText(this@MainActivity, "??????, ?????? ????????? ????????? ??? ???????????????. ??????????????? ???????????????.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAirQualityData(latitude: Double, longitude: Double) {
        val retrofitAPI = RetrofitConnection.getInstance().create(AirQualityService::class.java)

        retrofitAPI.getAirQualityData(latitude.toString(), longitude.toString(), "5e42faae-d8c8-4db0-b4a6-46f20a4899f5").enqueue(object : Callback<AirQualityResponse> {
            override fun onResponse(
                call : Call<AirQualityResponse>, response: Response<AirQualityResponse>
            ) {
                // ???????????? response ??? ????????? UI ????????????
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "?????? ?????? ???????????? ??????.", Toast.LENGTH_SHORT).show()
                    // response.body()??? null ??? ????????? updateAirUI()
                    response.body()?.let { updateAirUI(it) }
                } else {
                    Toast.makeText(this@MainActivity, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@MainActivity, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show()
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

    private fun checkAllPermissions() {
        if (!isLocationServicesAvailable()) {
            showDialogForLocationServiceSetting()
        } else {
            isRunTimePermissionsGranted()
        }
    }

    fun isLocationServicesAvailable() : Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    fun isRunTimePermissionsGranted() {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        }
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
                Toast.makeText(this@MainActivity, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun showDialogForLocationServiceSetting() {
        getGPSPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (isLocationServicesAvailable()) {
                    isRunTimePermissionsGranted()
                } else {
                    Toast.makeText(this@MainActivity, "?????? ???????????? ????????? ??? ????????????.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
        val builder : AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("?????? ????????? ????????????")
        builder.setMessage("?????? ???????????? ?????? ????????????. ???????????? ?????? ????????? ??? ????????????.")
        builder.setCancelable(true)
        builder.setPositiveButton("??????") { _, _ ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            getGPSPermissionLauncher.launch(callGPSSettingIntent)
        }
        builder.setNegativeButton("??????") { dialog, _ ->
            dialog.cancel()
            Toast.makeText(this@MainActivity, "???????????? ??????????????? ?????? ??? ??????????????????.", Toast.LENGTH_SHORT).show()
            finish()
        }
        builder.create().show()
    }

    fun FavoriteClick() {
        if (!favorite_click) {
            // ???????????? ??????
            binding.addFavorite.setOnClickListener {
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
                        "lng" to longitude,
                        "uid" to auth.uid
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
            }
        }
    }
}