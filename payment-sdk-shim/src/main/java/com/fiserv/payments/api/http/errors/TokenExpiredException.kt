package com.fiserv.payments.api.http.errors

class TokenExpiredException(message: String = "Client token expired") : Exception(message)
