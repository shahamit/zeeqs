package io.zeebe.zeeqs.importer.hazelcast

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class HazelcastConfig(
    val id: String,
    @Id var ringBufferName: String,
    var sequence: Long
)
