package com.shipal.shipal.Dto.User

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class UpdateUserDto (

    val userSeq: Int?,

    /*
    * 로그인 비밀번호
    * */
    @field:Size(max= 255)
    val loginPw: String?,

    /*
    * 휴대폰 번호
    * */
    @field:Size(max =50)
    val phone: String?,

    /*
    * 이름
    * */
    @field:Size(max = 50)
    val name: String?,

    /**
     * 주소
     */
    @field:Size(max = 255)
    var address: String?,

    @field:Size(max = 255)
    val nickname: String? = null,

    val images: MultipartFile? = null

)
