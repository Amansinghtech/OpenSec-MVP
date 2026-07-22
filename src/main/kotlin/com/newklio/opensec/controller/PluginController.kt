package com.newklio.opensec.controller

import com.newklio.opensec.plugin.PluginDescriptor
import com.newklio.opensec.plugin.PluginRegistry
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/plugins")
@PreAuthorize("hasRole('ADMIN')")
class PluginController(
    private val pluginRegistry: PluginRegistry,
) {
    @GetMapping
    fun listPlugins(): List<PluginDescriptor> = pluginRegistry.listAll()
}
