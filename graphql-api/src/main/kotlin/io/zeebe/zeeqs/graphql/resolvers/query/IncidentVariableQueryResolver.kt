package io.zeebe.zeeqs.graphql.resolvers.query

import io.zeebe.zeeqs.data.entity.Incident
import io.zeebe.zeeqs.data.repository.IncidentRepository
import io.zeebe.zeeqs.data.repository.VariableRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class IncidentVariableQueryResolver(
        private val incidentRepository: IncidentRepository,
        private val variableRepository: VariableRepository
) {

    @QueryMapping
    fun incidentsForUUIDVariable(@Argument variableValue: String): List<Incident> {
        return variableRepository.findFirst500ByNameOrderByTimestampDesc("uuid")
                .firstOrNull { it.value.contains(variableValue) }
                ?.let { incidentRepository.findByProcessInstanceKey(it.processInstanceKey) }
                ?: emptyList()
    }
}