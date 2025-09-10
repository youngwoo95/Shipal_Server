package com.shipal.shipal.common

import org.springframework.beans.factory.annotation.Value   // ✅ 여기!
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths


@Configuration
class WebConfig(@Value("\${fileshare.root:}") private val root: String?) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val base = (root?.takeIf { it.isNotBlank() }
            ?: Paths.get(System.getProperty("user.dir")).resolve("FileShare").toString())
        registry.addResourceHandler("/files/**")
            .addResourceLocations("file:$base/")
    }
}