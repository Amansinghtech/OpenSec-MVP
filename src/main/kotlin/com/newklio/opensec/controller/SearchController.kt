package com.newklio.opensec.controller

import com.newklio.opensec.entity.AuthenticatedUser
import com.newklio.opensec.search.SearchDocumentType
import com.newklio.opensec.search.SearchIndexer
import com.newklio.opensec.search.SearchResult
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
class SearchController(
    private val searchIndexer: SearchIndexer,
) {
    @GetMapping
    fun search(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @RequestParam query: String,
        @RequestParam(required = false) type: SearchDocumentType?,
        @RequestParam(defaultValue = "0") from: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): SearchResult {
        val tenantId =
            user.tenantId
                ?: throw IllegalStateException("User has no tenant")
        return searchIndexer.search(tenantId, query, type, from, size)
    }
}
