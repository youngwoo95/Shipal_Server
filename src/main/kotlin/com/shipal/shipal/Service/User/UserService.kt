package com.shipal.shipal.Service.User

import com.shipal.shipal.common.ResponseModel
import com.shipal.shipal.Dto.AddUserDto
import com.shipal.shipal.Model.UserInfo
import com.shipal.shipal.Repository.UserInfoRepository
import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserService (
    private val userRepo: UserInfoRepository,
    private val passwordEncoder: PasswordEncoder
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

}