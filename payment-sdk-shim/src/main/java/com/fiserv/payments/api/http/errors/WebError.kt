package com.fiserv.payments.api.http.errors

import kotlinx.serialization.Serializable

@Serializable
data class WebError(
    val primaryCode: String? = null,
    val primaryMessage: String? = null
)
