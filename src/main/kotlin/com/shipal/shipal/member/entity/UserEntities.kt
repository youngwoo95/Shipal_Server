package com.shipal.shipal.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.net.InetAddress
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_info",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_login_id", columnNames = ["loginId"])]
)
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    @Column(name = "USER_SEQ")
    var userSeq: Int? = null,

    @Column(name = "LOGIN_ID", nullable = false, length = 50)
    var loginId: String,

    @Column(name = "LOGIN_PW", nullable = false, length = 50)
    var loginPw: String,

    @Column(name = "PHONE", nullable = false, length = 50)
    var phone: String,

    @Column(name = "NAME", nullable = false, length = 50)
    var name: String,

    @Column(name = "ADDRESS", nullable = false, length = 255)
    var address: String,

    @Column(name = "NICKNAME", length = 255)
    var nickname: String? = null,

    @Column(name = "CREATE_DT", nullable = false)
    var createDt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "CREATE_USER", nullable = false, length = 255)
    var createUser: String,

    @Column(name = "UPDATE_DT", nullable = false)
    var updateDt: LocalDateTime = LocalDateTime.now(),

    @Column(name ="UPDATE_USER", nullable = false, length = 255)
    var updateUser: String,

    /* 삭제 플래그 */
    @Column(name = "DEL_YN")
    var delYn: Boolean? = null,

    @Column(name = "DEL_DT")
    var delDt: LocalDateTime? = null,

    @Column(name = "DEL_USER", length = 255)
    var delUesr: String? = null,

    @Column(name = "GENDER", length = 45)
    var gender: String

)