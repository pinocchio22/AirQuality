package com.p2glet.airquality.favorite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.p2glet.airquality.R

/**
* @author CHOI
* @email vviian.2@gmail.com
* @created 2022-06-30
* @desc
*/
class FavoriteAdapter(val itemList: ArrayList<FavoriteItem>): RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.favorite_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: FavoriteAdapter.ViewHolder, position: Int) {
        holder.name.text = itemList[position].name
        holder.location.text = itemList[position].location.toString()

        if (itemList[position].favorite) {
            holder.favorite.setBackgroundResource(R.drawable.ic_star_clicked)
        }
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val name : TextView = itemView.findViewById(R.id.name)
        val location : TextView = itemView.findViewById(R.id.location)
        val favorite : Button = itemView.findViewById(R.id.item_favorite)
    }
}