package com.p2glet.airquality.favorite

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.p2glet.airquality.FavoriteActivity
import com.p2glet.airquality.FavoriteLocation
import com.p2glet.airquality.MainActivity
import com.p2glet.airquality.R

/**
* @author CHOI
* @email vviian.2@gmail.com
* @created 2022-06-30
* @desc
*/
class FavoriteAdapter(val itemList: ArrayList<FavoriteItem>): RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.favorite_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = itemList[position].name
        holder.location.text = itemList[position].location

        if (itemList[position].favorite) {
            holder.favorite.setBackgroundResource(R.drawable.ic_star_clicked)
        }
        holder.itemView.setOnClickListener {
            var intent = Intent(it.context, FavoriteLocation::class.java)
            intent.putExtra("bool", itemList[position].favorite)
            intent.putExtra("latitude", itemList[position].lat)
            intent.putExtra("longitude", itemList[position].lng)
            it.context.startActivity(intent)
        }
//        // 즐겨찾기 수정
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("즐겨찾기를 해제하시겠습니까?")
//        builder.setPositiveButton("확인") { dialog, which ->
//            db.collection("Favorite_Place")
//                .document("$latitude+$longitude")
//                .update("favorite", false)
//                .addOnSuccessListener {
//                    // 성공
//                    Toast.makeText(this, "즐겨찾기가 해제되었습니다.", Toast.LENGTH_SHORT)
//                        .show()
//                    // 메인으로 이동
//                    val intent = Intent(this@FavoriteLocation, MainActivity::class.java)
//                    startActivity(intent)
//                }
//                .addOnFailureListener { exception ->
//                    Log.w("MainActivity", "Error getting documents: $exception")
//                }
//        }
//        builder.setNegativeButton("취소") { dialog, which ->
//            finish()
//        }
//        builder.show()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val name : TextView = itemView.findViewById(R.id.name)
        val location : TextView = itemView.findViewById(R.id.location)
        val favorite : Button = itemView.findViewById(R.id.item_favorite)
    }
}