package com.example.coupon.batch

import com.example.coupon.domain.CouponRepository
import com.example.coupon.infrastructure.cache.ReconcileProperties
import com.example.coupon.infrastructure.cache.ReconcileRedisRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Reconciler(
    private val couponRepository: CouponRepository,
    private val reconcileRedisRepository: ReconcileRedisRepository,
    private val reconcileProperties: ReconcileProperties,
    private val couponReconciler: CouponReconciler,
) {

    @Scheduled(fixedRateString = "\${coupon.reconcile.interval-ms}")
    fun scheduledRecent() {
        val cutOffMs = System.currentTimeMillis() - reconcileProperties.gracePeriodMs
        val fromMs = cutOffMs - reconcileProperties.intervalMs * 2
        val couponIds = reconcileRedisRepository.couponIdsIssuedBetween(fromMs, cutOffMs)
        reconcileCoupons(couponIds)
    }

    private fun reconcileCoupons(couponIds: List<Long>) {}
}