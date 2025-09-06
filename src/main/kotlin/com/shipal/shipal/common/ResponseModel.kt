package com.shipal.shipal.common

data class ResponseModel<T> (
    val message: String,
    val data: T?,
    val code: Int
)
