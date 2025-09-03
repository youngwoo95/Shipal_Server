package com.shipal.shipal.user.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.shipal.shipal.common.annotation.ValidEnum
import com.shipal.shipal.common.status.Gender
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UserDtoRequest
(
    @field:NotBlank
    @JsonProperty("loginId")
    private val _loginId : String?,

    @field:NotBlank
    @field:Pattern(
        regexp="^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#\$%^&*])[a-zA-Z0-9!@#\$%^&*]{8,20}\$",
        message = "영문, 숫자, 특수문자를 포함한 8~20자리로 입력해주세요"
    )
    @JsonProperty("loginPw")
    private val _loginPw : String?,

    @field:NotBlank
    @JsonProperty("phone")
    private val _phone : String?,

    @field:NotBlank
    @JsonProperty("name")
    private val _name : String?,

    @field:NotBlank
    @JsonProperty("address")
    private val _address : String?,

    @field:NotBlank
    @JsonProperty("nickName")
    private val _nickName : String?,

    @field:NotBlank
    @field:ValidEnum(
        enumClass = Gender::class,
        message = "MAN 또는 WOMAN만 입력가능합니다."
    )
    @JsonProperty("gender")
    private val _gender: String?
){
    val loginId : String
        get() = _loginId!!

    val loginPw : String
        get() = _loginPw!!

    val phone : String
        get() = _phone!!

    val name : String
        get() = _name!!

    val address : String
        get() = _address!!

    val nickName : String
        get() = _nickName!!

    val gender: Gender
        get() = Gender.valueOf(_gender!!)
}