package org.projects.app.orchestrator.model

import java.time.LocalDateTime
import java.util.UUID


data class ApplicationInfo(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val status: NodeAppStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

fun ApplicationInfo.toAppControllerResponse(): AppControllerResponse {
    return AppControllerResponse(
        name = this.name,
        status = this.status,
    )
}
