package com.example.coupon.application

import com.example.coupon.domain.IssuanceDltLog
import com.example.coupon.domain.IssuanceDltLogRepository
import com.example.coupon.domain.IssuanceDltStatus
import com.example.coupon.infrastructure.messaging.IssuanceRequestProducer
import com.example.coupon.infrastructure.messaging.IssuanceRequested
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper


@Service
class IssuanceDltReplayService(
    private val issuanceDltLogRepository: IssuanceDltLogRepository,
    private val issuanceRequestProducer: IssuanceRequestProducer,
) {
    private val jsonMapper = JsonMapper.builder().findAndAddModules().build()

    @Transactional
    fun replay(ids: List<Long>): Int {
        val selectedIds = ids.distinct()
        require(selectedIds.isNotEmpty()) { "There must be at least one issue request" }
        require(selectedIds.size <= MAX_REPLAY_COUNT) { "There must be at least $MAX_REPLAY_COUNT issue requests" }

        val logs = issuanceDltLogRepository.findAllByIdForUpdate(selectedIds).sortedBy { it.receivedAt }
        check(logs.size == selectedIds.size) { "There must be at least ${selectedIds.size} logs" }

        val events = logs.map { log ->
            check(log.status == IssuanceDltStatus.PENDING) {
                "Only pending logs can be published: ${log.id}"
            }
            log.toEvent()
        }
        logs.zip(events).forEach { (log, event) ->
            issuanceRequestProducer.publishAndWait(event)
            log.status = IssuanceDltStatus.REPLAYED
        }

        return logs.size
    }

    private fun IssuanceDltLog.toEvent(): IssuanceRequested =
        jsonMapper.readValue(payload, IssuanceRequested::class.java)

    private companion object {
        const val MAX_REPLAY_COUNT = 100
    }
}