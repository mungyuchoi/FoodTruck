package com.mungyu.foodtruck.model

import androidx.annotation.Keep

@Keep
data class Location(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var title: String = "",
    var description: String? = "",
    var registerKey: String? = "",
    var typeImageId : Int = 0,
    var registerDate: Long = 0,
    var updateDate: Long = 0
)