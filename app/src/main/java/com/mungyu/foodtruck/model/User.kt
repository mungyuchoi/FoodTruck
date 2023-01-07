package com.mungyu.foodtruck.model

import androidx.annotation.Keep

@Keep
data class User(
    var name: String? = null,
    var imageUrl: String? = null,
    var permission: Int = 0,
    var email: String? = null
)