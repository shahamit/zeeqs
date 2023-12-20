package io.zeebe.zeeqs.graphql.resolvers.query

import io.zeebe.zeeqs.data.entity.BpmnElementType
import io.zeebe.zeeqs.data.repository.ElementInstanceRepository
import io.zeebe.zeeqs.data.repository.IncidentRepository
import io.zeebe.zeeqs.data.repository.VariableRepository
import io.zeebe.zeeqs.data.service.BpmnElementInfo
import io.zeebe.zeeqs.data.service.IncidentRingbufferForUuid
import io.zeebe.zeeqs.data.service.ProcessService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class IncidentUuidVariableQueryResolver(
        private val incidentRepository: IncidentRepository,
        private val variableRepository: VariableRepository,
        private val elementInstanceRepository: ElementInstanceRepository,
        private val processService: ProcessService
) {

    @QueryMapping
    fun incidentsForUUIDVariable(@Argument uuidVariableValue: String): List<IncidentRingbufferForUuid>{
        return variableRepository.findFirst500ByNameOrderByTimestampDesc("uuid")
            .filter { it.value.contains(uuidVariableValue) }
            .flatMap { incidentRepository.findByProcessInstanceKey(it.processInstanceKey) }
            .flatMap { incident ->
                val elementInstanceKey = incident.elementInstanceKey
                val elementInstance = elementInstanceRepository.findById(elementInstanceKey)
                val elementId = elementInstance.map { it.elementId }.orElse("")
                val processInstanceKey = incident.processInstanceKey
                val processDefinitionKey = incident.processDefinitionKey
                val elementInfoMap: Map<String, BpmnElementInfo>? = processService.getBpmnElementInfo(processDefinitionKey)
                val bpmnElementInfo: BpmnElementInfo? = elementId.let { elementInfoMap?.get(it) }
                val importedIncident = IncidentRingbufferForUuid(
                    key = incident.key,
                    elementId = elementId ?: "",
                    elementName = bpmnElementInfo?.elementName ?: "",
                    processInstanceKey = processInstanceKey,
                    elementType = bpmnElementInfo?.elementType ?: BpmnElementType.UNKNOWN,
                    creationTime = incident.creationTime,
                    errorType = incident.errorType,
                    errorMessage = incident.errorMessage
                )
                listOf(importedIncident).takeIf { it.isNotEmpty() } ?: emptyList()
            }
    }

}