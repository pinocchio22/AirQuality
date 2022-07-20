package com.p2glet.airquality.favorite

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.p2glet.airquality.databinding.ActivityFavoriteBinding
import com.p2glet.airquality.favorite.FavoriteAdapter
import com.p2glet.airquality.favorite.FavoriteItem


/**
 * @author CHOI
 * @email vviian.2@gmail.com
 * @created 2022-06-30
 * @desc
 */
class FavoriteActivity : AppCompatActivity() {
    lateinit var binding: ActivityFavoriteBinding
    val db = FirebaseFirestore.getInstance()
    val itemlist = arrayListOf<FavoriteItem>()
    val adapter = FavoriteAdapter(itemlist)

    var auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerFavorite.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerFavorite.adapter = adapter
        init()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun init() {
        db.collection("Favorite_Place").whereEqualTo("uid", auth.uid)
            .get()
            .addOnSuccessListener { result ->
                // 성공
                itemlist.clear()
                for (document in result) {
                    val item = FavoriteItem(document["name"] as String, document["location"] as String, document["favorite"] as Boolean, document["lat"] as Double, document["lng"] as Double, document["uid"] as String)
                    itemlist.add(item)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // 실패
                Log.d("MainActivity", "Error getting document : $exception")
            }
    }
}























