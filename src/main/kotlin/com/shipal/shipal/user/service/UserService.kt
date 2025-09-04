package com.shipal.shipal.user.service

import com.shipal.shipal.common.exception.InvalidInputException
import com.shipal.shipal.user.dto.UserDtoRequest
import com.shipal.shipal.user.entity.User
import com.shipal.shipal.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
@Service
class UserService (
    private val userRepository: UserRepository
)
{
    /* 회원가입 */
    fun signUp(userDtoRequest: UserDtoRequest ) : String{
        // ID 중복 검사
        var user: User? = userRepository.findByLoginId(userDtoRequest.loginId)
        if(user != null)
        {
            throw InvalidInputException("loginId","이미 등록된 ID 입니다.")
        }

        /*
        var newUser = User(
            null,
            userDtoRequest.loginId,
            userDtoRequest.loginPw,
            userDtoRequest.phone,
            userDtoRequest.name,
            userDtoRequest.address,
            nickname = userDtoRequest.nickName,
            createDt = LocalDateTime.now(),
            createUser = "시스템관리자",
            updateDt = LocalDateTime.now(),
            updateUser = "시스템관리자",
            gender = "남"
        )
        */

        user = userDtoRequest.toEntity()
        user.createDt = LocalDateTime.now()
        user.createUser ="시스템관리자"
        user.updateDt = LocalDateTime.now()
        user.updateUser = "시스템관리자"

        userRepository.save(user)

        return "회원가입이 완료되었습니다."
    }
}