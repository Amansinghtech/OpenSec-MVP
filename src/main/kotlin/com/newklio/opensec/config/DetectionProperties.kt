package com.newklio.opensec.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Risk weights and thresholds for the detection engine. Config-driven so they can be tuned per
 * deployment; centralized management + Redis caching is formalized in Phase 15 (Config Service).
 */
@ConfigurationProperties(prefix = "opensec.detection")
data class DetectionProperties(
    /** Minimum risk score for a signal to be emitted. */
    var emitThreshold: Int = 30,
    /** Wazuh-style severity level at/above which the anomaly detector fires. */
    var anomalySeverityThreshold: Int = 10,
    var weightAuthFailure: Int = 20,
    var weightMalware: Int = 90,
    var weightIntrusion: Int = 80,
    var weightWebAttack: Int = 70,
    var weightPrivilegeEscalation: Int = 75,
    var weightSuspiciousSession: Int = 70,
    var weightSuccessfulBruteForce: Int = 95,
)
