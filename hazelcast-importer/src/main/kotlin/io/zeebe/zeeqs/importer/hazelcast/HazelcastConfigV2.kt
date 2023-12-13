package io.zeebe.zeeqs.importer.hazelcast

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class HazelcastConfigV2(
        val hzConnectionString: String,
        @Id val ringBufferName: String,
        var sequence: Long
)
