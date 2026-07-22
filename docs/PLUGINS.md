# OPENSEC Plugin Architecture (Phase 17)

OPENSEC uses a **plugin SPI** so detection rules and defense actions can be added without modifying core services.

## Plugin types

| SPI | Interface | Discovery |
|-----|-----------|-----------|
| Detection | `Detector` | Spring `@Component` beans auto-wired into `DetectionEngine` |
| Defense | `DefenseExecutor` | Spring `@Component` beans auto-wired into `DefenseEngine` |
| General | `OpensecPlugin` | Lifecycle hooks (`onLoad`, `onShutdown`) |

## Listing loaded plugins

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8001/api/v1/admin/plugins
```

## Authoring a detection plugin

```kotlin
@Component
class MyCustomDetector : Detector {
    override fun onEvent(event: NormalizedEvent): List<DetectionSignal> {
        // return signals when your rule matches
        return emptyList()
    }

    override fun onSession(session: SessionSummary): List<DetectionSignal> = emptyList()
}
```

Register the class in a package scanned by Spring Boot (`com.newklio.opensec.*`). No core code changes required.

## Authoring a defense plugin

```kotlin
@Component
class MyDefenseAction : DefenseExecutor {
    override val actionType = DefenseActionType.BLOCK_IP

    override fun execute(alert: Alert, dryRun: Boolean): String {
        return if (dryRun) "Would block ${alert.entityId}" else "Blocked ${alert.entityId}"
    }
}
```

## Future: isolated plugin JARs

The `OpensecPlugin` interface is the hook for PF4J or `URLClassLoader`-based loading from a `plugins/` directory. Core built-in detectors/executors already follow the same SPI.
