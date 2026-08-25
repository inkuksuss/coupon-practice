package com.example.coupon.support

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        e: DomainException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(e.httpStatus, e.message ?: "")
        pd.title = humanize(e.code)
        pd.instance = URI.create(request.requestURI)
        pd.setProperty("code", e.code)

        return ResponseEntity.status(e.httpStatus)
            .body(pd)
    }

    private fun humanize(code: String): String =
        code.split("_")
            .joinToString(" ")
            { it.lowercase().replaceFirstChar { c -> c.titlecase() }}
}