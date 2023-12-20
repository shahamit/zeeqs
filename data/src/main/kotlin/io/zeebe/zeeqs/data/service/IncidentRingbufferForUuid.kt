package io.zeebe.zeeqs.data.service

import io.zeebe.zeeqs.data.entity.BpmnElementType
import jakarta.persistence.Lob

data class IncidentRingbufferForUuid(
        val key: Long,
        val elementId: String,
        val elementName: String,
        val processInstanceKey: Long,
        val elementType: BpmnElementType?,
        var creationTime: Long?,
        val errorType: String,
        @Lob val errorMessage: String
)