package com.example.coupon.domain

import io.lettuce.core.dynamic.annotation.Param
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface IssuanceDltLogRepository: JpaRepository<IssuanceDltLog, Long> {

    fun existsByMessageKey(key: String): Boolean

    fun findTop100ByOrderByReceivedAtDesc(): List<IssuanceDltLog>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT log FROM IssuanceDltLog log WHERE log.id = :ids")
    fun findAllByIdForUpdate(@Param("id") ids: Collection<Long>): List<IssuanceDltLog>
}