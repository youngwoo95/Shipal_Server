package com.shipal.shipal.Repository

import com.shipal.shipal.Model.UserInfo
import com.shipal.shipal.VO.UserVO
import org.apache.ibatis.annotations.*

@Mapper
interface UserInfoRepository {

    /*
    * 회원가입
    * */
    @Insert("""
        INSERT INTO USER_INFO (LOGIN_ID, PHONE, LOGIN_PW, NAME, NICKNAME, CREATE_DT, ATTACH)
        VALUES (#{loginId}, #{phone}, #{loginPw}, #{name}, #{nickname}, #{createDt}, #{attach})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "userSeq")
    fun AddUserInfo(userInfo: UserInfo ): Int

    /*
    * 회원가입 로그인 아이디 중복체크
    * */
    @Select("SELECT COUNT(1) FROM USER_INFO WHERE LOGIN_ID = #{loginId}")
    fun existsByLoginId(loginId: String): Int

    /*
    * 로그인 확인
    * */
    @Select("""
      SELECT
        USER_SEQ as userSeq,
        LOGIN_ID as loginId,
        PHONE as phone,
        LOGIN_PW as loginPw,
        NAME as name,
        NICKNAME as nickname,
        CREATE_DT as createDt,
        ATTACH as attach
       FROM USER_INFO
       WHERE LOGIN_ID = #{loginId} AND DEL_YN = FALSE
    """)
    fun getUserLogin(
        @Param("loginId") loginId: String
    ) : UserInfo?

    /*
    *  유저 테이블 조회
    * */
    @Select("""
      SELECT
        USER_SEQ as userSeq,
        LOGIN_ID as loginId,
        PHONE as phone,
        LOGIN_PW as loginPw,
        NAME as name,
        NICKNAME as nickname,
        CREATE_DT as createDt,
        ATTACH as attach
       FROM USER_INFO
       WHERE USER_SEQ = #{userSeq} AND DEL_YN = FALSE
    """)
    fun getUserInfo(
        @Param("userSeq") userSeq: Int
    ) : UserInfo?

    /*
    * 유저 프로필 조회
    * */
    @Select("""
        SELECT
        USER_SEQ as userSeq,
        LOGIN_ID as loginId,
        PHONE as phone,
        LOGIN_PW as loginPw,
        NAME as name,
        NICKNAME as nickname,
        CREATE_DT as createDt
       FROM USER_INFO
       WHERE USER_SEQ = #{userSeq} AND DEL_YN = FALSE
    """)
    fun getUserBySeq(@Param("userSeq") userSeq: Int) : UserInfo?

    /*
    * 유저 프로필 조회
    * */
    @Select("""
        SELECT
            ui.USER_SEQ as userSeq,
            ui.LOGIN_ID as loginId,
            ui.PHONE as phone,
            ui.NAME as name,
            ui.NICKNAME as nickname,
            ui.ATTACH as images
        FROM USER_INFO as ui
        WHERE ui.DEL_YN = FALSE AND ui.USER_SEQ = #{userSeq}
    """)
    fun getUserProfile(@Param("userSeq") userSeq: Int) : UserVO?

    /*
    * 유저 PATCH 수정
    * */
    @Update("""
        <script>
         UPDATE USER_INFO
         <set>
            <if test="name != null">        NAME = #{name},</if>
            <if test="loginPw != null">     LOGIN_PW = #{loginPw},</if>
            <if test="phone != null">       PHONE = #{phone},</if>
            <if test="nickname != null">    NICKNAME = #{nickname},</if>
            <if test="attach != null">      ATTACH = #{attach},</if>
          </set>
        WHERE USER_SEQ = #{userSeq}
        </script>
    """)
    fun updateUser(user: UserInfo): Int
}