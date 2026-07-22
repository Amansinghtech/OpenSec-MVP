package com.newklio.opensec.repository

import com.newklio.opensec.entity.DetectionSignalRecord
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DetectionSignalRepository : JpaRepository<DetectionSignalRecord, UUID>
