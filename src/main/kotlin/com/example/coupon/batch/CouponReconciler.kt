package com.example.coupon.batch

import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.IssuanceRepository
import com.example.coupon.infrastructure.cache.ReconcileRedisRepository
import com.example.coupon.infrastructure.cache.SoldOutProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.math.abs

private val log = KotlinLogging.logger {}

@Component
class CouponReconciler(
    private val couponRepository: CouponRepository,
    private val issuanceRepository: IssuanceRepository,
    private val reconcileRedisRepository: ReconcileRedisRepository,
    private val soldOutProperties: SoldOutProperties
) {

    fun reconcile(couponId: Long): CouponReconcileOutcome {
        val coupon = couponRepository.findById(couponId).orElse(null)
            ?: return alert(couponId, "coupon not found")

        val stock = reconcileRedisRepository.stock(couponId)
            ?: return alert(couponId, "Redis stock not found")

        val dbDrift = coupon.totalQuantity - (coupon.issuedQuantity + stock)
        val stockNegative = stock < 0

        if (stockNegative) log.warn { "stock can be negative couponId=$couponId stock=$stock" }
        if (dbDrift != 0L) log.warn { "DB 불일치 감지 coupon=$coupon residual=$dbDrift (자동 보정 불가, 사람 확인)" }
        if (stockNegative || dbDrift != 0L) {
            return CouponReconcileOutcome(redisDbDrift = abs(dbDrift).toInt(), driftAlerts = true)
        }

        var autoFixed = reconcileSoldOut(couponId, stock)
        val userDrift = coupon.totalQuantity - (reconcileRedisRepository.userCount(couponId) + stock)
        var driftAlert = false
        if (userDrift > 0) {
            val redisUserIds = reconcileRedisRepository.userIds(couponId).mapNotNull { it.toLongOrNull() }
            val missingUserIds = issuanceRepository.findUserIdsByCouponId(couponId) - redisUserIds
            if (missingUserIds.size.toLong() == userDrift) {
                reconcileRedisRepository.addUsers(couponId, missingUserIds)
                autoFixed++
                log.info { "auto-fix: 사용자 목록 SADD ${missingUserIds.size}건 coupon=$couponId" }
            }
            else {
                driftAlert = true
                log.warn { "Redis 사용자 목록을 안전하게 복구할 수 없음 coupon=$couponId" }
            }

            return CouponReconcileOutcome(autoFixed = autoFixed, driftAlerts = driftAlert)
        } else if (userDrift < 0) {
            driftAlert = true
            log.warn { "Redis 사용자 목록 초과 감지 coupon=$couponId residual=$userDrift" }
        }
    }

    private fun reconcileSoldOut(couponId: Long, stock: Long): Int {
        val soldOut = reconcileRedisRepository.soldOutExists(couponId)
        return when {
            stock > 0 && soldOut -> {
                reconcileRedisRepository.deleteSoldOut(couponId)
                log.info { "auto-fix: 매진 플래그 해제 coupon=$couponId" }
                1
            }
            stock == 0L && !soldOut -> {
                reconcileRedisRepository.setSoldOut(couponId, soldOutProperties.ttlSeconds)
                log.info { "auto-fix 매진 플래그 설정 coupon=$couponId" }
                1
            }
            else -> 0
        }
    }

    private fun alert(couponId: Long, reason: String): CouponReconcileOutcome {
        log.warn { "대상 확인 불가 coupon $couponId reason=$reason (수동확인필요)" }
        return CouponReconcileOutcome(driftAlerts = true)
    }
}

class CouponReconcileOutcome(
    val autoFixed: Int = 0,
    val redisDbDrift: Int = 0,
    val driftAlerts: Boolean = false,
) {

}