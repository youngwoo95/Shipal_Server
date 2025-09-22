package com.shipal.shipal.Controller

import com.shipal.shipal.Dto.User.*
import com.shipal.shipal.Service.User.UserService
import com.shipal.shipal.VO.UserVO
import com.shipal.shipal.common.ResponseModel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "User API", description = "회원가입 / 로그인 / 토큰")
class UserController (
    private val userService: UserService
)

{
    /*
    * 회원가입
    * */
    @PostMapping("/addUser", consumes = ["multipart/form-data"])
    @Operation(summary = "회원가입")
    fun addUserInfo(@ModelAttribute @Valid req: AddUserDto): ResponseEntity<ResponseModel<Boolean>> {

        val model = userService.addUserService(req)

        val status = when (model.code)
        {
            200 -> HttpStatus.OK
            400 -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_GATEWAY
        }
        return ResponseEntity.status(status).body(model)
    }

    @RequestMapping(value = ["/updateUser"],method = [RequestMethod.PATCH],consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "회원정보 수정")
     fun updateUserInfo(@ModelAttribute @Valid dto: UpdateUserDto, req: HttpServletRequest): ResponseEntity<ResponseModel<Boolean>> {

        val model =  userService.updateUserService(dto,req)

        val status = when (model.code)
        {
            200 -> HttpStatus.OK
            400 -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_GATEWAY
        }
        return ResponseEntity.status(status).body(model)
    }


    @Operation(summary = "로그인")
    @RequestMapping(
        value = ["/login"],
        method = [RequestMethod.POST],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getLogin(@RequestBody req: LoginDto) : ResponseEntity<ResponseModel<ResponseTokenDto>>{

        val model = userService.loginService(req)

        val status = when(model.code){
            200 -> HttpStatus.OK
            400 -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_GATEWAY
        }
        return ResponseEntity.status(status).body(model)
    }

    /*
     *   리프레쉬 토큰 발급
     */
    @PostMapping("/refresh")
    @Operation(summary = "리프레쉬 토큰")
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

    /*
     *   로그아웃
     */
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

    @GetMapping("/profile")
    fun getProfile(req: HttpServletRequest): ResponseEntity<ResponseModel<UserVO>>{
        val model = userService.getProfile(req)
        val status = when (model.code){
            200 -> HttpStatus.OK
            400 -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_GATEWAY
        }

        return ResponseEntity.status(status).body(model)
    }

}