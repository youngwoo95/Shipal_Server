package com.shipal.shipal.user.repository

import com.shipal.shipal.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>{

    fun findByLoginId(loginId: String): User?


}