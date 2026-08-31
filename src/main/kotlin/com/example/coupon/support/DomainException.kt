package com.example.coupon.support

import org.springframework.http.HttpStatus

sealed class DomainException(
    val code: String,
    val httpStatus: HttpStatus,
    message: String,
): RuntimeException(message)

class CouponNotFoundException(message: String = "쿠폰 정보를 찾을 수 없습니다."):
    DomainException("COUPON NOT_FOUND", HttpStatus.NOT_FOUND, message)

class NotStartedException(message: String = "발급이 아직 시작되지 않았습니다."):
        DomainException("NO STARTED", HttpStatus.CONFLICT, message)

class SoldOutException(message: String = "쿠폰이 매진되었습니다."):
        DomainException("SOLD_OUT", HttpStatus.CONFLICT, message)

class AlreadyIssuedException(message: String = "이미 발급된 쿠폰입니다."):
        DomainException("ALREADY_ISSUED", HttpStatus.CONFLICT, message)

class IssuanceNotFountException(message: String = "발급 내용을 찾을 수 없습니다."):
    DomainException("ISSUING NOT_FOUND", HttpStatus.NOT_FOUND, message)

class NotOwnerException(message: String = "본인의 쿠폰이 아닙니다."):
    DomainException("NOT_OWNER", HttpStatus.FORBIDDEN, message)

class AlreadyUsedException(message: String = "이미 사용된 쿠폰입니다."):
        DomainException("ALREADY_USED", HttpStatus.CONFLICT, message)

class ExpiredException(message: String = "이미 만료된 쿠폰입니다."):
    DomainException("EXPIRED", HttpStatus.CONFLICT, message)

class QueueFullException(message: String = "발급 큐가 일시적으로 가득 찼습니다."):
    DomainException("QUEUE_FULLED", HttpStatus.SERVICE_UNAVAILABLE, message)