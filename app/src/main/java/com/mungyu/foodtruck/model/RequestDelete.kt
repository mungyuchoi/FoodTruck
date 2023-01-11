package com.mungyu.foodtruck.model

import androidx.annotation.Keep

@Keep
data class RequestDelete(
    var count: Int = 0,
    var locationKey : String? ="",
    var firstRegisterKey: String? = "",
    var secondRegisterKey: String? = "",
)