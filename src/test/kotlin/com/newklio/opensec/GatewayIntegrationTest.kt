package com.newklio.opensec

import com.newklio.opensec.context.RequestContext
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GatewayIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) {
        @Test
        fun `generates a correlation id header when none is supplied`() {
            mockMvc
                .perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized)
                .andExpect(
                    header().string(
                        RequestContext.CORRELATION_ID_HEADER,
                        matchesPattern("[0-9a-fA-F-]{36}"),
                    ),
                )
        }

        @Test
        fun `propagates a supplied correlation id`() {
            val supplied = "test-correlation-123"
            mockMvc
                .perform(
                    get("/api/v1/users/me").header(RequestContext.CORRELATION_ID_HEADER, supplied),
                ).andExpect(status().isUnauthorized)
                .andExpect(header().string(RequestContext.CORRELATION_ID_HEADER, supplied))
        }
    }
