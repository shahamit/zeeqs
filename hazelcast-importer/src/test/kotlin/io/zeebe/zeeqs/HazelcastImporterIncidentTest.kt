package io.zeebe.zeeqs

import io.camunda.zeebe.client.ZeebeClient
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent
import io.zeebe.containers.ZeebeContainer
import io.zeebe.zeeqs.data.repository.IncidentRepository
import io.zeebe.zeeqs.importer.hazelcast.HazelcastConfigRepositoryV2
import io.zeebe.zeeqs.importer.hazelcast.HazelcastIncidentImporter
import io.zeebe.zeeqs.importer.hazelcast.HazelcastProperties
import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration

@SpringBootTest
@Testcontainers
@Transactional
class HazelcastImporterIncidentTest(
    @Autowired val incidentImporter: HazelcastIncidentImporter,
    @Autowired val incidentRepository: IncidentRepository,
    @Autowired val hazelcastConfigRepository: HazelcastConfigRepositoryV2
) {


    private val hazelcastPort = 5701

    private lateinit var zeebeClient: ZeebeClient

    @Container
    var zeebe = ZeebeContainer(ZeebeTestcontainerUtil.ZEEBE_DOCKER_IMAGE)
        .withAdditionalExposedPort(hazelcastPort)

    @BeforeEach
    fun `start importer`() {
        val port = zeebe.getMappedPort(hazelcastPort)
        val hazelcastProperties = HazelcastProperties(
            "localhost:$port", "PT10S", "zeebe"
        )
        incidentImporter.start(hazelcastProperties)
    }

    @BeforeEach
    fun `create Zeebe client`() {
        zeebeClient = ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebe.externalGatewayAddress)
            .usePlaintext()
            .build()
    }

    @Test
    fun `should import decision`() {
        // when
        zeebeClient.newDeployResourceCommand()
            .addResourceFromClasspath("demo-http.bpmn")
            .send()
            .join();

        zeebeClient.newCreateInstanceCommand()
            .bpmnProcessId("http")
            .latestVersion()
            .withResult()
            .send()
            .join()



        // then
        await.untilAsserted { assertThat(incidentRepository.findAll()).hasSize(1) }
    }

    @SpringBootApplication
    class TestConfiguration
}

