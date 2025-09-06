package com.shipal.shipal.Repository

import com.shipal.shipal.Model.UserInfo
import org.apache.ibatis.annotations.*

@Mapper
interface UserInfoRepository {

    @Insert("""
        INSERT INTO USER_INFO (LOGIN_ID, PHONE, LOGIN_PW, NAME, ADDRESS, NICKNAME, CREATE_DT, CREATE_USER, UPDATE_DT, UPDATE_USER)
        VALUES (#{loginId}, #{phone}, #{loginPw}, #{name}, #{address}, #{nickname}, #{createDt}, #{createUser}, #{updateDt}, #{updateUser})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "userSeq")
    fun AddUserInfo(userInfo: UserInfo ): Int

    @Select("SELECT COUNT(1) FROM USER_INFO WHERE LOGIN_ID = #{loginId}")
    fun existsByLoginId(loginId: String): Int
}