package io.zeebe.zeeqs.data.entity

import jakarta.persistence.*

@Entity
data class UserTask(
    @Id @Column(name = "key_") val key: Long,
    val position: Long,
    val processInstanceKey: Long,
    val processDefinitionKey: Long,
    val elementInstanceKey: Long,
    val assignee: String?,
    val candidateGroups: String?,
    val formKey: String?
) {
    constructor() : this(0, 0, 0, 0, 0, null, null, null)

    @Enumerated(EnumType.STRING)
    var state: UserTaskState = UserTaskState.CREATED
    var timestamp: Long = -1

    var startTime: Long? = null
    var endTime: Long? = null

}
