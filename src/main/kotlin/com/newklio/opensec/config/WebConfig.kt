package com.newklio.opensec.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

const val API_PREFIX = "/api/v1"

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix(
            API_PREFIX,
            HandlerTypePredicate.forAnnotation(RestController::class.java),
        )
    }
}
