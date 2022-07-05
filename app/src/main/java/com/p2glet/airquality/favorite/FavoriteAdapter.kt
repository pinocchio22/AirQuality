package com.p2glet.airquality.favorite

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
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

    val db = FirebaseFirestore.getInstance()

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
        // 즐겨찾기 수정
        holder.setting.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            val tvName = TextView(it.context)
            tvName.text = "이름"
            val etName = EditText(it.context)
            etName.isSingleLine = true
            val mLayout = LinearLayout(it.context)
            mLayout.orientation = LinearLayout.VERTICAL
            mLayout.setPadding(15)
            mLayout.addView(tvName)
            mLayout.addView(etName)
            builder.setView(mLayout)

            builder.setTitle("이름을 수정하시겠습니까?")
            builder.setPositiveButton("확인") { dialog, which ->
                db.collection("Favorite_Place")
                    .document("${itemList[position].lat}+${itemList[position].lng}")
                    .update("name", etName.text.toString())
                    .addOnSuccessListener {
                        // 성공
                    }
                    .addOnFailureListener { exception ->
                        Log.w("FavoriteAdapter", "Error update documents: $exception")
                    }
            }
            builder.setNegativeButton("취소") { dialog, which ->
            }
            builder.show()
        }
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val name : TextView = itemView.findViewById(R.id.name)
        val location : TextView = itemView.findViewById(R.id.location)
        val favorite : Button = itemView.findViewById(R.id.item_favorite)
        val setting : Button = itemView.findViewById(R.id.setting_btn)
    }
}