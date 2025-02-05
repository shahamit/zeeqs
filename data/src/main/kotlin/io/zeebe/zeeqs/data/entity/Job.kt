package io.zeebe.zeeqs.data.entity

import jakarta.persistence.*

@Entity
data class Job(
    @Id @Column(name = "key_") val key: Long,
    val position: Long,
    val jobType: String,
    val processInstanceKey: Long,
    val elementInstanceKey: Long,
    val processDefinitionKey: Long
) {
    constructor() : this(0, 0, "", 0, 0, 0)

    @Enumerated(EnumType.STRING)
    var state: JobState = JobState.ACTIVATABLE
    var timestamp: Long = -1

    var startTime: Long? = null
    var endTime: Long? = null

    var retries: Int? = null
    var worker: String? = null

}