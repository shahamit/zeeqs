package io.zeebe.zeeqs.importer.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import io.zeebe.exporter.proto.Schema
import io.zeebe.exporter.proto.Schema.RecordMetadata.RecordType
import io.zeebe.zeeqs.data.entity.Incident
import io.zeebe.zeeqs.data.entity.IncidentState
import io.zeebe.zeeqs.data.entity.Variable
import io.zeebe.zeeqs.data.entity.VariableUpdate
import io.zeebe.zeeqs.data.reactive.DataUpdatesPublisher
import io.zeebe.zeeqs.data.repository.IncidentRepository
import io.zeebe.zeeqs.data.repository.VariableRepository
import io.zeebe.zeeqs.data.repository.VariableUpdateRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class HazelcastIncidentImporter(
        val hazelcastConfigRepository: HazelcastConfigRepository,
        val incidentRepository: IncidentRepository,
        val variableRepository: VariableRepository,
        val variableUpdateRepository: VariableUpdateRepository,
        private val dataUpdatesPublisher: DataUpdatesPublisher
) {

    var zeebeHazelcast: ZeebeHazelcastIncidentProcessor? = null
    var logger: Logger = LoggerFactory.getLogger(HazelcastIncidentImporter::class.java)

    fun start(hazelcastProperties: HazelcastProperties) {

        val hazelcastConnection = hazelcastProperties.connection
        val hazelcastConnectionTimeout = Duration.parse(hazelcastProperties.connectionTimeout)
        val hazelcastIncidentRingbuffer = hazelcastProperties.incidentRingBuffer
        val hazelcastConnectionInitialBackoff =
                Duration.parse(hazelcastProperties.connectionInitialBackoff)
        val hazelcastConnectionBackoffMultiplier = hazelcastProperties.connectionBackoffMultiplier
        val hazelcastConnectionMaxBackoff = Duration.parse(hazelcastProperties.connectionMaxBackoff)

        val hazelcastConfig = hazelcastConfigRepository.findById(hazelcastConnection)
                .orElse(
                        HazelcastConfig(
                                id = hazelcastConnection, //todo - should accept ring buffer name
                                sequence = -1
                        )
                )

        val updateSequence: ((Long) -> Unit) = {
            hazelcastConfig.sequence = it
            hazelcastConfigRepository.save(hazelcastConfig)
        }

        val clientConfig = ClientConfig()
        val networkConfig = clientConfig.networkConfig
        networkConfig.addresses = listOf(hazelcastConnection)

        val connectionRetryConfig = clientConfig.connectionStrategyConfig.connectionRetryConfig
        connectionRetryConfig.clusterConnectTimeoutMillis = hazelcastConnectionTimeout.toMillis()
        // These retry configs can be user-configured in application.yml
        connectionRetryConfig.initialBackoffMillis =
                hazelcastConnectionInitialBackoff.toMillis().toInt()
        connectionRetryConfig.multiplier = hazelcastConnectionBackoffMultiplier
        connectionRetryConfig.maxBackoffMillis = hazelcastConnectionMaxBackoff.toMillis().toInt()

        val hazelcast = HazelcastClient.newHazelcastClient(clientConfig)

        val builder = ZeebeHazelcastIncidentProcessor.newBuilder(hazelcast).name(hazelcastIncidentRingbuffer)
                .addIncidentListener {
                    it.takeIf { it.metadata.recordType == RecordType.EVENT }
                            ?.let(this::importIncidentRecord)
                }
                .addVariableListener {
                    it.takeIf { it.metadata.recordType == RecordType.EVENT }
                            ?.let(this::importVariableRecord)
                }
                .postProcessListener(updateSequence)

        if (hazelcastConfig.sequence >= 0) {
            builder.readFrom(hazelcastConfig.sequence)
        } else {
            builder.readFromHead()
        }

        zeebeHazelcast = builder.build()
    }

    fun stop() {
        zeebeHazelcast?.close()
    }


    private fun importIncidentRecord(record: Schema.IncidentRecord) {
        val entity = incidentRepository
                .findById(record.metadata.key)
                .orElse(createIncident(record))

        when (record.metadata.intent) {
            "CREATED" -> {
                entity.state = IncidentState.CREATED
                entity.creationTime = record.metadata.timestamp
            }

            "RESOLVED" -> {
                entity.state = IncidentState.RESOLVED
                entity.resolveTime = record.metadata.timestamp
            }
        }

        incidentRepository.save(entity)
        dataUpdatesPublisher.onIncidentUpdated(entity)
    }

    private fun createIncident(record: Schema.IncidentRecord): Incident {
        return Incident(
                key = record.metadata.key,
                position = record.metadata.position,
                errorType = record.errorType,
                errorMessage = record.errorMessage,
                processInstanceKey = record.processInstanceKey,
                processDefinitionKey = record.processDefinitionKey,
                elementInstanceKey = record.elementInstanceKey,
                jobKey = record.jobKey.takeIf { it > 0 }
        )
    }

    private fun importVariableRecord(record: Schema.VariableRecord) {
        importVariable(record)
        importVariableUpdate(record)
    }

    private fun importVariable(record: Schema.VariableRecord) {

        val entity = variableRepository
                .findById(record.metadata.key)
                .orElse(createVariable(record))

        entity.value = record.value
        entity.timestamp = record.metadata.timestamp

        variableRepository.save(entity)
        dataUpdatesPublisher.onVariableUpdated(entity)
    }

    private fun createVariable(record: Schema.VariableRecord): Variable {
        return Variable(
                key = record.metadata.key,
                position = record.metadata.position,
                name = record.name,
                value = record.value,
                processInstanceKey = record.processInstanceKey,
                processDefinitionKey = record.processDefinitionKey,
                scopeKey = record.scopeKey,
                timestamp = record.metadata.timestamp
        )
    }

    private fun importVariableUpdate(record: Schema.VariableRecord) {

        val partitionIdWithPosition = getPartitionIdWithPosition(record.metadata)
        val entity = variableUpdateRepository
                .findById(partitionIdWithPosition)
                .orElse(
                        VariableUpdate(
                                partitionIdWithPosition = partitionIdWithPosition,
                                variableKey = record.metadata.key,
                                name = record.name,
                                value = record.value,
                                processInstanceKey = record.processInstanceKey,
                                scopeKey = record.scopeKey,
                                timestamp = record.metadata.timestamp
                        )
                )

        variableUpdateRepository.save(entity)
    }

    private fun getPartitionIdWithPosition(metadata: Schema.RecordMetadata) =
            "${metadata.partitionId}-${metadata.position}"

}
