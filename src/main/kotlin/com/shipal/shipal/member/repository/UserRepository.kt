package com.shipal.shipal.member.repository

import com.shipal.shipal.member.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>{

    fun findByLoginId(loginId: String): User?


}