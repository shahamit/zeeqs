package io.zeebe.zeeqs

import io.zeebe.zeeqs.importer.hazelcast.HazelcastImporter
import io.zeebe.zeeqs.importer.hazelcast.HazelcastIncidentImporter
import io.zeebe.zeeqs.importer.hazelcast.HazelcastProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(HazelcastProperties::class)
class ZeeqsApplication(
        val hazelcastProperties: HazelcastProperties,
        val hazelcastImporter: HazelcastImporter,
        val hazelcastIncidentImporter: HazelcastIncidentImporter
) {
    val logger = LoggerFactory.getLogger(ZeeqsApplication::class.java)

    @PostConstruct
    fun init() {
        logger.info("Connecting to Hazelcast: '$hazelcastProperties'")
        hazelcastImporter.start(hazelcastProperties)
        logger.info("Connected to Hazelcast!")
        hazelcastIncidentImporter.start(hazelcastProperties)
        logger.info("Connected to Incident Hazelcast!")
    }
}

fun main(args: Array<String>) {
    runApplication<ZeeqsApplication>(*args)
}