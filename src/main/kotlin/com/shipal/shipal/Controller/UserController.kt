package com.shipal.shipal.Controller

import com.shipal.shipal.common.ResponseModel
import com.shipal.shipal.Dto.AddUserDto
import com.shipal.shipal.Service.User.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
            else -> HttpStatus.OK
        }
        return ResponseEntity.status(status).body(model)

    }
}