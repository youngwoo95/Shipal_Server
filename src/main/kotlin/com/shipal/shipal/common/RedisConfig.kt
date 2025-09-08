package com.shipal.shipal.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun redisObjectMapper(): ObjectMapper =
        ObjectMapper().registerModule(KotlinModule.Builder().build())

    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper,
    ): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(connectionFactory)

        val keySer = StringRedisSerializer()
        val valSer = GenericJackson2JsonRedisSerializer(objectMapper)

        template.keySerializer = keySer
        template.valueSerializer = valSer
        template.hashKeySerializer = keySer
        template.hashValueSerializer = valSer

        template.afterPropertiesSet()
        return template
    }

    // 문자열만 다룰 때 편한 템플릿 (옵션)
    @Bean
    fun stringRedisTemplate(connectionFactory: RedisConnectionFactory): StringRedisTemplate =
        StringRedisTemplate(connectionFactory).apply {
            setEnableTransactionSupport(true) // 트랜잭션 지원 ON
        }

}