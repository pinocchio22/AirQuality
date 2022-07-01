package com.p2glet.airquality

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.p2glet.airquality.databinding.ActivityFavoriteLocationBinding
import com.p2glet.airquality.databinding.ActivityMainBinding
import com.p2glet.airquality.favorite.FavoriteItem
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
    private lateinit var binding : ActivityFavoriteLocationBinding

    private val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    val db = FirebaseFirestore.getInstance()

    lateinit var getGPSPermissionLauncher : ActivityResultLauncher<Intent>
    lateinit var locationProvider : LocationProvider

    var latitude = 0.0
    var longitude = 0.0

    var favorite_itemlist = arrayListOf<FavoriteItem>()
    var favorite_click = false

    val startMapActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
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
        binding = ActivityFavoriteLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        favorite_click = intent.getBooleanExtra("bool", false)


        checkAllPermissions()
        updateUI()
        setRefreshButton()
        setFab()
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
                Log.d("ads log","배너 광고가 로드되었습니다.")
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                Log.d("ads log","배너 광고가 로드 실패했습니다. ${adError.responseInfo} ${adError.code}")
            }

            override fun onAdOpened() {
                Log.d("ads log","배너 광고를 열었습니다.")
            }

            override fun onAdClicked() {
                Log.d("ads log","배너 광고를 클릭했습니다.")
            }

            override fun onAdClosed() {
                Log.d("ads log", "배너 광고를 닫았습니다.")
            }
        }
    }

    private fun setInterstitialAds(){
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-3610848843940754/9052433671", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ads log", "전면 광고가 로드 실패했습니다. ${adError.responseInfo}")
                setTestInterstitialAds()
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("ads log", "전면 광고가 로드되었습니다.")
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun setTestInterstitialAds(){
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ads log", "테스트 전면 광고가 로드 실패했습니다. ${adError.responseInfo}")
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("ads log", "테스트 전면 광고가 로드되었습니다.")
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

    private fun setFab() {
        binding.fab.setOnClickListener {
            if (mInterstitialAd != null) {
                mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("ads log", "전면 광고가 닫혔습니다.")

                        val intent = Intent(this@FavoriteLocation, MapActivity::class.java)
                        intent.putExtra("currentLat", latitude)
                        intent.putExtra("currentLng", longitude)
                        startMapActivityResult.launch(intent)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d("ads log", "전면 광고가 열리는데 실패했습니다.")
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("ads log", "전면 광고가 성공적으로 열렸습니다.")
                        mInterstitialAd = null
                    }
                }
                mInterstitialAd!!.show(this@FavoriteLocation)
            } else {
                Log.d("InterstitialAd", "전면 광고가 로딩되지 않았습니다.")
                Toast.makeText(this@FavoriteLocation, "잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
            }
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

        //위도와 경도 정보를 가져옴
        if (latitude == 0.0 || longitude == 0.0) {
            latitude = intent.getDoubleExtra("latitude",0.0)
            longitude = intent.getDoubleExtra("longitude",0.0)
        }

        if (latitude != 0.0 || longitude != 0.0) {
            // 1. 현재 위치를 가져오고 UI 업데이트
            val address = getCurrentAddress(latitude, longitude)
            address?.let {
                binding.tvLocationTitle.text = it.thoroughfare
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}"
            }
            getAirQualityData(latitude, longitude)
            // 2. 현재 미세먼지 농도를 가져오고 UI 업데이트
        } else {
            Toast.makeText(this@FavoriteLocation, "위도, 경도 정보를 가져올 수 없었습니다. 새로고침을 눌러주세요.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAirQualityData(latitude: Double, longitude: Double) {
        val retrofitAPI = RetrofitConnection.getInstance().create(AirQualityService::class.java)

        retrofitAPI.getAirQualityData(latitude.toString(), longitude.toString(), "5e42faae-d8c8-4db0-b4a6-46f20a4899f5").enqueue(object :
            Callback<AirQualityResponse> {
            override fun onResponse(
                call : Call<AirQualityResponse>, response: Response<AirQualityResponse>
            ) {
                // 정삭적인 response 가 왔다면 UI 업데이트
                if (response.isSuccessful) {
                    Toast.makeText(this@FavoriteLocation, "최신 정보 업데이트 완료.", Toast.LENGTH_SHORT).show()
                    // response.body()가 null 이 아니면 updateAirUI()
                    response.body()?.let { updateAirUI(it) }
                } else {
                    Toast.makeText(this@FavoriteLocation, "업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@FavoriteLocation, "업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
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
                binding.tvTitle.text = "좋음"
                binding.imgBg.setImageResource(R.drawable.bg_good)
            }
            in 51..150 -> {
                binding.tvTitle.text = "보통"
                binding.imgBg.setImageResource(R.drawable.bg_soso)
            }
            in 151..200 -> {
                binding.tvTitle.text = "나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_bad)
            }
            else -> {
                binding.tvTitle.text = "매우 나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_worst)
            }
        }
    }

    fun getCurrentAddress(latitude : Double, longitude : Double) : Address? {
        val geocoder = Geocoder(this, Locale.getDefault())
        // Address 객체는 주소와 관련된 여러 정보를 가지고 있습니다.
        // android.location.Address 패키지 참고.

        val addresses : List<Address>? = try {
            // Geocoder 객체를 이용하여 위도와 경도로부터 리스트를 가져옵니다.
            geocoder.getFromLocation(latitude, longitude, 7)
        } catch (ioException : IOException) {
            Toast.makeText(this, "지오코더 서비스 사용불가합니다.", Toast.LENGTH_LONG).show()
            return null
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(this, "잘못된 위도, 경도 입니다.", Toast.LENGTH_LONG).show()
            return null
        }

        // 에러는 아니지만 주소가 발견되지 않은 경우
        if (addresses == null || addresses.size == 0) {
            Toast.makeText(this, "주소가 발견되지 않았습니다.", Toast.LENGTH_LONG).show()
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
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER))
    }

    fun isRunTimePermissionsGranted() {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this@FavoriteLocation, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this@FavoriteLocation, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@FavoriteLocation, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
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
                Toast.makeText(this@FavoriteLocation, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this@FavoriteLocation, "위치 서비스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
        val builder : AlertDialog.Builder = AlertDialog.Builder(this@FavoriteLocation)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("위치 서비스가 꺼져 있습니다. 설정해야 앱을 사용할 수 있습니다.")
        builder.setCancelable(true)
        builder.setPositiveButton("설정") { _, _ ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            getGPSPermissionLauncher.launch(callGPSSettingIntent)
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.cancel()
            Toast.makeText(this@FavoriteLocation, "기기에서 위치서비스 설정 후 사용해주세요.", Toast.LENGTH_SHORT).show()
            finish()
        }
        builder.create().show()
    }

    fun FavoriteClick() {
        // 즐겨찾기 해제

//        binding.addFavorite.setOnClickListener {
//            val builder = AlertDialog.Builder(this)
//            val tvName = TextView(this)
//            tvName.text = "이름"
//            val etName = EditText(this)
//            etName.isSingleLine = true
//            val mLayout = LinearLayout(this)
//            mLayout.orientation = LinearLayout.VERTICAL
//            mLayout.setPadding(15)
//            mLayout.addView(tvName)
//            mLayout.addView(etName)
//            builder.setView(mLayout)
//
//            builder.setTitle("즐겨찾기로 저장하시겠습니까?")
//            builder.setPositiveButton("확인") { dialog , which ->
//                val data = hashMapOf("name" to etName.text.toString(), "location" to binding.tvLocationTitle.text, "favorite" to favorite_click, "lat" to latitude, "lng" to longitude )
//                db.collection("Favorite_Place")
//                    .add(data)
//                    .addOnSuccessListener {
//                        // 성공
//                        Toast.makeText(this, "즐겨찾기가 추가되었습니다.", Toast.LENGTH_SHORT).show()
//                    }
//                    .addOnFailureListener { exception ->
//                        Log.w("MainActivity", "Error getting documents: $exception")
//                    }
//            }
//            builder.setNegativeButton("취소") { dialog , which ->
//            }
//            builder.show()
//        }
    }

    fun setFavoriteLocation() {

    }
}