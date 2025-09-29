package org.projects.app.orchestrator.model

import java.time.LocalDateTime


data class ApplicationInfo(
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
