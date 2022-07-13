package com.p2glet.airquality.favorite

/**
 * @author CHOI
 * @email vviian.2@gmail.com
 * @created 2022-06-30
 * @desc
 */
data class FavoriteItem(val name : String, val location : String, var favorite : Boolean, var lat : Double, var lng : Double, var uid : String)
