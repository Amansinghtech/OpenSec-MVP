package com.newklio.opensec

import com.newklio.opensec.plugin.PluginRegistry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class PluginRegistryTest
    @Autowired
    constructor(
        private val pluginRegistry: PluginRegistry,
    ) {
        @Test
        fun `registry lists built-in detectors and defense executors`() {
            val plugins = pluginRegistry.listAll()
            assertTrue(plugins.any { it.type == "detector" })
            assertTrue(plugins.any { it.type == "defense" })
        }
    }
