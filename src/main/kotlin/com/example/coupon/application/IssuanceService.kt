package com.example.coupon.application

import com.example.coupon.domain.Issuance
import com.example.coupon.domain.IssuanceRepository
import com.example.coupon.domain.IssuanceStatus
import com.example.coupon.support.AlreadyUsedException
import com.example.coupon.support.ExpiredException
import com.example.coupon.support.IssuanceNotFountException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class IssuanceService(
    private val issuanceRepository: IssuanceRepository
) {

    @Transactional
    fun use(issuanceId: Long, userId: Long): Issuance {
        val issuance = issuanceRepository.findById(issuanceId)
            .orElseThrow { IssuanceNotFountException() }

        when (issuance.status) {
            IssuanceStatus.USED -> throw AlreadyUsedException()
            IssuanceStatus.EXPIRED -> throw ExpiredException()
            IssuanceStatus.ISSUED -> Unit
        }

        val now = LocalDateTime.now()
        if (issuance.isExpired(now)) {
            throw ExpiredException()
        }

        issuance.markUsed(now)
        return issuance
    }

    fun findByUser(userId: Long): List<Issuance> {
        return issuanceRepository.findByUserIdOrderByIssuedAtDesc(userId)
    }
}