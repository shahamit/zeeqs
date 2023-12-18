package io.zeebe.zeeqs.graphql.resolvers.query

import io.zeebe.zeeqs.data.entity.Incident
import io.zeebe.zeeqs.data.repository.IncidentRepository
import io.zeebe.zeeqs.data.repository.VariableRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class IncidentVariableQueryResolver(
        private val incidentRepository: IncidentRepository,
        private val variableRepository: VariableRepository
) {

    @QueryMapping
    fun incidentsForUUIDVariable(@Argument variableValue: String): Incident? {
        return variableRepository.findFirst500ByNameOrderByTimestampDesc("uuid")
            .firstOrNull { it.value.contains(variableValue) }
            ?.let { incidentRepository.findByProcessInstanceKey(it.processInstanceKey) }
            ?.getOrNull(0) // Get the incident at the 0th index
            ?.let { incidentRepository.findByIdOrNull(it.key) }
    }
}