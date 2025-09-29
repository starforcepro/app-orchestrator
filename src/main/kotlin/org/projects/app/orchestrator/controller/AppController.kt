package org.projects.app.orchestrator.controller

import org.projects.app.orchestrator.model.AppControllerResponse
import org.projects.app.orchestrator.model.AppServiceResponseStatus
import org.projects.app.orchestrator.model.toAppControllerResponse
import org.projects.app.orchestrator.service.NodeAppService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apps")
class AppController(
    private val nodeAppService: NodeAppService
) {

    @GetMapping("/{name}/status")
    fun status(@PathVariable name: String): ResponseEntity<AppControllerResponse> {
        val response = nodeAppService.getByName(name)
        val result = response.status
        return when (result) {
            AppServiceResponseStatus.NOT_FOUND -> ResponseEntity.notFound().build()
            AppServiceResponseStatus.SUCCESS -> ResponseEntity.ok(response.applicationInfo?.toAppControllerResponse())
            else -> ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/deploy")
    fun deploy(
        @RequestBody request: DeployRequest
    ): ResponseEntity<AppControllerResponse> {
        val response = nodeAppService.deploy(request.name, request.archive)
        val result = response.status
        return when (result) {
            AppServiceResponseStatus.INVALID_ARCHIVE -> ResponseEntity.badRequest().build()
            AppServiceResponseStatus.SUCCESS -> ResponseEntity.ok()
                .body(response.applicationInfo?.toAppControllerResponse())

            else -> ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{name}/start")
    fun start(@PathVariable name: String): ResponseEntity<AppControllerResponse> {
        val response = nodeAppService.start(name)
        val result = response.status
        return when (result) {
            AppServiceResponseStatus.NOT_FOUND -> ResponseEntity.notFound().build()
            AppServiceResponseStatus.SUCCESS -> ResponseEntity.ok()
                .body(response.applicationInfo?.toAppControllerResponse())

            else -> ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{name}/stop")
    fun stop(@PathVariable name: String): ResponseEntity<AppControllerResponse> {
        val response = nodeAppService.stop(name)
        val result = response.status
        return when (result) {
            AppServiceResponseStatus.NOT_FOUND -> ResponseEntity.notFound().build()
            AppServiceResponseStatus.SUCCESS -> {
                ResponseEntity.ok().body(response.applicationInfo?.toAppControllerResponse())
            }

            else -> ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{name}/remove")
    fun remove(@PathVariable name: String): ResponseEntity<AppControllerResponse> {
        val response = nodeAppService.remove(name)
        val result = response.status
        return when (result) {
            AppServiceResponseStatus.NOT_FOUND -> ResponseEntity.notFound().build()
            AppServiceResponseStatus.SUCCESS -> {
                ResponseEntity.ok().body(response.applicationInfo?.toAppControllerResponse())
            }

            else -> ResponseEntity.internalServerError().build()
        }
    }
}

data class DeployRequest(
    val name: String,
    val archive: String
)