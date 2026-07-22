package com.newklio.opensec.model

/**
 * Severity band derived from a 0–100 risk score.
 */
enum class SignalSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    ;

    companion object {
        fun fromScore(score: Int): SignalSeverity =
            when {
                score >= 90 -> CRITICAL
                score >= 70 -> HIGH
                score >= 40 -> MEDIUM
                else -> LOW
            }
    }
}
