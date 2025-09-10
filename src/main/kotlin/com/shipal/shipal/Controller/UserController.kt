package com.shipal.shipal.Controller

import com.shipal.shipal.common.ResponseModel
import com.shipal.shipal.Dto.User.AddUserDto
import com.shipal.shipal.Dto.User.LoginDto
import com.shipal.shipal.Dto.User.RefreshDto
import com.shipal.shipal.Dto.User.ResponseTokenDto
import com.shipal.shipal.Service.User.UserService
import com.shipal.shipal.Service.comm.AuthUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user")
class UserController (
    private val userService: UserService
)

{
    @PostMapping("/addUser")
    fun addUserInfo(@RequestBody @Valid req: AddUserDto): ResponseEntity<ResponseModel<Boolean>> {

        val model = userService.addUserService(req)

        val status = when (model.code)
        {
            200 -> HttpStatus.OK
            400 -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_GATEWAY
        }
        return ResponseEntity.status(status).body(model)

    }

    @PostMapping("/login")
    fun getLogin(@RequestBody req: LoginDto) : ResponseEntity<ResponseModel<ResponseTokenDto>>{

        val model = userService.loginService(req)

        val status = when(model.code){
            200 -> HttpStatus.OK
            400 -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_GATEWAY
        }
        return ResponseEntity.status(status).body(model)
    }

    // 리프레쉬 토큰 발급
    @PostMapping("/refresh")
    fun refresh(@RequestBody req: RefreshDto): ResponseEntity<ResponseModel<ResponseTokenDto>> {
        val model = userService.refreshService(req)
        val status = when (model.code) {
            200 -> HttpStatus.OK
            400 -> HttpStatus.BAD_REQUEST
            401 -> HttpStatus.UNAUTHORIZED
            else -> HttpStatus.BAD_GATEWAY
        }
        return ResponseEntity.status(status).body(model)
    }

    // 로그아웃
    @PostMapping("/logout")
    fun logout(): ResponseEntity<ResponseModel<Boolean>> {
        val model = userService.logoutService()
        val status = when (model.code) {
            200 -> HttpStatus.OK
            400 -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_GATEWAY
        }
        return ResponseEntity.status(status).body(model)
    }

    /*
    @GetMapping("/test")
    fun getMyInfo(): ResponseEntity<ResponseModel<String>> {
        val model = userService.writePost()
        val status = when (model.code) {
            200 -> HttpStatus.OK
            400 -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_REQUEST
        }
        return ResponseEntity.status(status).body(model)
    }
*/



}