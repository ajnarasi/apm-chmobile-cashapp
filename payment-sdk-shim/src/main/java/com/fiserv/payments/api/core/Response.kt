package com.fiserv.payments.api.core

interface Response<T> {
    fun success(response: T)
    fun error(exception: Throwable?)
}
