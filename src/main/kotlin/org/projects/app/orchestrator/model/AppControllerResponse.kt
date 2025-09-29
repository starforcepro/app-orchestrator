package org.projects.app.orchestrator.model

data class AppControllerResponse(
    val name: String,
    val status: NodeAppStatus,
)