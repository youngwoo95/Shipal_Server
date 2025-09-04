package com.shipal.shipal.user.controller

import com.shipal.shipal.common.dto.BaseResponse
import com.shipal.shipal.user.dto.UserDtoRequest
import com.shipal.shipal.user.service.UserService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/User")
@RestController
class UserController(
    private val userService: UserService
)
{
    /* 회원가입 */
    @PostMapping("/signUp")
    fun signUp(@RequestBody @Valid userDtoRequest: UserDtoRequest) : BaseResponse<Unit>{
        var resultMsg: String = userService.signUp(userDtoRequest)
        return BaseResponse(message = resultMsg)
    }
}