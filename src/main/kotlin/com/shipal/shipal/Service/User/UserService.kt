package com.shipal.shipal.Service.User

import com.shipal.shipal.common.ResponseModel
import com.shipal.shipal.Dto.User.AddUserDto
import com.shipal.shipal.Dto.User.LogOutDto
import com.shipal.shipal.Dto.User.LoginDto
import com.shipal.shipal.Dto.User.RefreshDto
import com.shipal.shipal.Dto.User.ResponseTokenDto
import com.shipal.shipal.Model.UserInfo
import com.shipal.shipal.Repository.UserInfoRepository
import com.shipal.shipal.Service.comm.GetTokenValues
import com.shipal.shipal.Service.comm.JwtService
import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserService (
    private val userRepo: UserInfoRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val redisService: com.shipal.shipal.Service.Redis.RedisService
)

{
    @Transactional
    fun addUserService(req: AddUserDto) : ResponseModel<Boolean> {

        val loginId = req.loginId.trim()
        val phone = req.phone.filter { it.isDigit() } // 숫자만
        val name = req.name.trim()
        val address = req.address.trim()
        val nickname = req.nickname?.trim()

        if(userRepo.existsByLoginId(loginId) > 0) {
            return ResponseModel(message = "이미 사용중인 아이디 입니다.", data = false, code = 400)
        }

        // 비밀번호 BCrypto 암호화
        val bCrypto = passwordEncoder.encode(req.loginPw)

        // 데이터베이스에 전달할 엔티티 생성
        val now = LocalDateTime.now()
        val userParam = UserInfo(
            userSeq = null,
            loginId = loginId,
            phone = phone,
            loginPw = bCrypto,
            name = name,
            address = address,
            nickname = nickname, // 널 가능
            createDt = now,
            createUser = "시스템관리자",
            updateDt = now,
            updateUser = "시스템관리자"
        )

        // 저장
        val affected = userRepo.AddUserInfo(userParam)

        if(affected <= 0 || userParam.userSeq == null)
        {
            return ResponseModel(message = "잘못된 요청입니다.", data = false, code = 400)
        }

        return ResponseModel(message = "요청이 정상 처리되었습니다.", data = true, code = 200)
    }

    fun loginService(req: LoginDto) : ResponseModel<ResponseTokenDto>{

        // 입력 정규화
        val loginId = req.loginId.trim()
        val loginPw = req.loginPw.trim()

        // 사용자 조회
        val user = userRepo.getUserLogin(loginId) ?: return ResponseModel<ResponseTokenDto>(message = "등록되지 않은 사용자입니다.", data = null, code = 400)

        // 비밀번호 암호화 검증
        val passwordChk = passwordEncoder.matches(req.loginPw, user.loginPw)
        if(!passwordChk)
            return ResponseModel<ResponseTokenDto>(message = "비밀번호가 일치하지 않습니다.", data = null, code =400)

        val accessToken = jwtService.generateAccessToken(
            subject = user.loginId,
            roles = listOf("USER"),
            extra = mapOf(
                "uId" to (user.userSeq ?: 0),
                "name" to (user.name)
            )
        )

        // 2) uuid 준비 (LoginDto에 uuid가 없다면 서버에서 생성)
        val uuid = /* req.uuid ?: */ java.util.UUID.randomUUID().toString()

        // 3) Redis에 리프레시 토큰 저장 (TTL 3일, 필요시 변경)
        val pair = redisService.setRefreshToken(
            userSeq = user.userSeq ?: return ResponseModel("잘못된 사용자 정보입니다.", null, 400),
            accessToken = accessToken,
            uuid = uuid
        ) ?: return ResponseModel("세션 생성에 실패했습니다.", null, 500)

        val (_, refreshToken) = pair

        val body = ResponseTokenDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            uuid =uuid
        )

        return ResponseModel<ResponseTokenDto>(message =  "요청이 정상 처리되었습니다.", data = body, code = 200)
    }

    // 토큰 재발급 (Access 재발급 + Refresh 회전)
    fun refreshService(req: RefreshDto): ResponseModel<ResponseTokenDto> {
        val rotated = redisService.rotateRefreshToken(req.userSeq, req.refreshToken, req.uuid)
            ?: return ResponseModel(message = "리프레시 토큰이 유효하지 않습니다.", data = null, code = 401)

        // 2) 사용자 조회 (subject/클레임에 넣을 정보)
        val user = userRepo.getUserBySeq(req.userSeq)
            ?: return ResponseModel(message = "사용자 정보를 찾을 수 없습니다.", data = null, code = 400)

        // 3) Access Token 재발급
        val newAccess = jwtService.generateAccessToken(
            subject = user.loginId,                  // 기존 로직과 동일
            roles = listOf("USER"),
            extra = mapOf(
                "uId" to (user.userSeq ?: 0),
                "name" to user.name
            )
        )

        val body = ResponseTokenDto(
            accessToken = newAccess,
            refreshToken = rotated,                  // 회전되었으면 새 값, 아니면 기존 값
            uuid = req.uuid
        )
        return ResponseModel(message = "요청이 정상 처리되었습니다.", data = body, code = 200)
    }

    // 로그아웃
    fun logoutService(req: LogOutDto): ResponseModel<Boolean> {
        val ok = redisService.logout(req.userSeq)
        return if (ok)
            ResponseModel(message = "요청이 정상 처리되었습니다.", data = true, code = 200)
        else
            ResponseModel(message = "세션이 존재하지 않습니다.", data = false, code = 400)
    }


    /*
    fun writePost(): ResponseModel<String>{
        val me = GetTokenValues.get() ?: throw IllegalStateException("로그인이 필요합니다.")

        val temp = "현재 로그인 사용자: ${me.uId}, ${me.name}, ${me.role}"

        return ResponseModel("내용", data = temp, code = 200)
    }
    */
}