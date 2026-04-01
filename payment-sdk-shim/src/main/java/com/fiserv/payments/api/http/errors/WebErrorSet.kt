package com.fiserv.payments.api.http.errors

data class WebErrorSet(
    val primaryCode: String? = null,
    val primaryMessage: String? = null,
    val errors: List<WebError> = emptyList()
)
