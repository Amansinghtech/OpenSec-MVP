package com.newklio.opensec.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opensec.session")
data class SessionProperties(
    /** Inactivity window; a new event after this gap starts a fresh session. */
    var windowMinutes: Long = 30,
    /** Auth failures within a session before it's flagged as a possible brute force. */
    var bruteForceThreshold: Int = 5,
    /** Auth failures preceding a success before it's flagged as a possible successful brute force. */
    var bruteForceSuccessThreshold: Int = 3,
)
