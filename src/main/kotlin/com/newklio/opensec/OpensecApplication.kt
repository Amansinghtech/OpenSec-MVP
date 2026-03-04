package com.newklio.opensec

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpensecApplication

fun main(args: Array<String>) {
	runApplication<OpensecApplication>(*args)
}
