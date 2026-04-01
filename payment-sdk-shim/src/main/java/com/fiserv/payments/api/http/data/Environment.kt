package com.fiserv.payments.api.http.data

enum class Environment(val value: String) {
    PRODUCTION("production"),
    SANDBOX("sandbox");
    override fun toString(): String = value
}
