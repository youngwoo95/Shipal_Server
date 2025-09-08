package com.shipal.shipal.Controller

import com.shipal.shipal.Service.Redis.RedisService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RedisPingController(
        private val redisService: RedisService
)

{
    @GetMapping("/redis/ping")
    fun ping(): Map<String, String?> {
        return mapOf("pong" to redisService.ping())
    }
}