package com.newklio.opensec.repository

import com.newklio.opensec.entity.NormalizedEventRecord
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NormalizedEventRepository : JpaRepository<NormalizedEventRecord, UUID>
