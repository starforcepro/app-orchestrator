package org.projects.app.orchestrator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AppOrchestratorApplication

fun main(args: Array<String>) {
    runApplication<AppOrchestratorApplication>(*args)
}
