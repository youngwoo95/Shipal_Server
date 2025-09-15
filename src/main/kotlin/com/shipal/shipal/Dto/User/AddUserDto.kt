package com.shipal.shipal.Dto.User

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class AddUserDto (
    /*
    * 로그인 아이디
    * */
    @field:NotBlank
    @field:Size(max = 50)
    val loginId: String,

    /*
    * 로그인 비밀번호
    * */
    @field:NotBlank
    @field:Size(max= 255)
    val loginPw: String,

    /*
    * 휴대폰 번호
    * */
    @field:NotBlank
    @field:Size(max =50)
    val phone: String,

    /*
    * 이름
    * */
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,


    @field:Size(max = 255)
    val nickname: String? = null,

    val images: MultipartFile? = null
)