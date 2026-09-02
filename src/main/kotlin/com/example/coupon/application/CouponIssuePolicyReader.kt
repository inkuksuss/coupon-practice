package com.example.coupon.application

import com.example.coupon.domain.CouponRepository
import com.example.coupon.infrastructure.cache.CacheProperties
import com.example.coupon.infrastructure.cache.CouponCacheRepository
import com.example.coupon.support.CouponNotFoundException
import org.springframework.stereotype.Component

@Component
class CouponIssuePolicyReader(
    private val couponRepository: CouponRepository,
    private val couponCacheRepository: CouponCacheRepository,
    private val cacheProperties: CacheProperties,
) {

    fun get(id: Long): CouponPolicy = couponCacheRepository.getIssuePolicyOrLoad(id) {
        Thread.sleep(cacheProperties.simulatedLoadLatencyMs)
        couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
            .let(CouponPolicy::from)
    }
}