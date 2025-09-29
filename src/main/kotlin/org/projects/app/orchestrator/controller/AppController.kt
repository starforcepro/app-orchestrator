package org.projects.app.orchestrator.controller

import org.projects.app.orchestrator.model.AppControllerResponse
import org.projects.app.orchestrator.model.AppServiceResponseStatus
import org.projects.app.orchestrator.model.toAppControllerResponse
import org.projects.app.orchestrator.service.NodeAppService
import org.springframework.http.HttpStatus
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
            AppServiceResponseStatus.UNKNOWN_ERROR -> ResponseEntity.badRequest().build()
            AppServiceResponseStatus.NOT_FOUND -> ResponseEntity.badRequest().build()
            AppServiceResponseStatus.ALREADY_EXISTS -> ResponseEntity.status(HttpStatus.CONFLICT).build()
            AppServiceResponseStatus.SUCCESS -> ResponseEntity.ok()
                .body(response.applicationInfo?.toAppControllerResponse())

            else -> ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{name}/activate")
    fun activate(@PathVariable name: String): ResponseEntity<AppControllerResponse> {
        val response = nodeAppService.activate(name)
        val result = response.status
        return when (result) {
            AppServiceResponseStatus.NOT_FOUND -> ResponseEntity.notFound().build()
            AppServiceResponseStatus.SUCCESS -> ResponseEntity.ok()
                .body(response.applicationInfo?.toAppControllerResponse())

            else -> ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{name}/deactivate")
    fun deactivate(@PathVariable name: String): ResponseEntity<AppControllerResponse> {
        val response = nodeAppService.deactivate(name)
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