package com.newklio.opensec.plugin

import com.newklio.opensec.service.defense.DefenseExecutor
import com.newklio.opensec.service.detector.Detector
import org.springframework.stereotype.Component

data class PluginDescriptor(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val type: String,
)

@Component
class PluginRegistry(
    plugins: List<OpensecPlugin>,
    detectors: List<Detector>,
    defenseExecutors: List<DefenseExecutor>,
) {
    init {
        plugins.forEach { it.onLoad() }
    }

    private val pluginDescriptors =
        plugins.map {
            PluginDescriptor(it.id, it.name, it.version, it.description, "plugin")
        }

    private val detectorDescriptors =
        detectors.map {
            PluginDescriptor(
                id = it.javaClass.simpleName,
                name = it.javaClass.simpleName,
                version = "core",
                description = "Built-in detection plugin",
                type = "detector",
            )
        }

    private val defenseDescriptors =
        defenseExecutors.map {
            PluginDescriptor(
                id = it.actionType.name,
                name = it.javaClass.simpleName,
                version = "core",
                description = "Built-in defense action plugin",
                type = "defense",
            )
        }

    fun listAll(): List<PluginDescriptor> = pluginDescriptors + detectorDescriptors + defenseDescriptors
}
