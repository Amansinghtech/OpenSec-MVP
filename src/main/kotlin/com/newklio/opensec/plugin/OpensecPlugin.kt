package com.newklio.opensec.plugin

/**
 * Base contract for OPENSEC plugins (Phase 17). Plugins are discovered via Spring `@Component`
 * scanning or future isolated classloader loading from a `plugins/` directory.
 */
interface OpensecPlugin {
    val id: String
    val name: String
    val version: String
    val description: String

    fun onLoad() = Unit

    fun onShutdown() = Unit
}
