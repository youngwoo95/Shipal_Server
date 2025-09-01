package com.shipal.shipal.member.controller

import com.shipal.shipal.common.dto.UserDtoRequest
import com.shipal.shipal.member.service.UserService
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
    fun signUp(@RequestBody userDtoRequest: UserDtoRequest) : String{
        return userService.signUp(userDtoRequest)
    }
}