package com.newklio.opensec.context

import java.util.UUID

/**
 * Request-scoped ambient context propagated via a ThreadLocal. Holds the correlation id and the
 * resolved tenant so services and repositories can access them without threading parameters
 * through every call. Populated by the gateway filters and cleared at the end of each request.
 */
object RequestContext {
    const val CORRELATION_ID_HEADER = "X-Correlation-Id"
    const val MDC_CORRELATION_ID = "correlationId"
    const val MDC_TENANT_ID = "tenantId"

    private data class Holder(
        var correlationId: String? = null,
        var tenantId: UUID? = null,
    )

    private val threadLocal = ThreadLocal.withInitial { Holder() }

    fun getCorrelationId(): String? = threadLocal.get().correlationId

    fun setCorrelationId(value: String?) {
        threadLocal.get().correlationId = value
    }

    fun getTenantId(): UUID? = threadLocal.get().tenantId

    fun setTenantId(value: UUID?) {
        threadLocal.get().tenantId = value
    }

    fun clear() = threadLocal.remove()
}
