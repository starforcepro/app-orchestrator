package org.projects.app.orchestrator.controller

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.projects.app.orchestrator.TestBase
import org.projects.app.orchestrator.expressJsFileContent
import org.projects.app.orchestrator.getLambdaCode
import org.projects.app.orchestrator.model.AppControllerResponse
import org.projects.app.orchestrator.model.AppServiceResponse
import org.projects.app.orchestrator.model.AppServiceResponseStatus
import org.projects.app.orchestrator.model.NodeAppStatus
import org.projects.app.orchestrator.service.NodeAppService
import org.projects.app.orchestrator.toZipByteArray
import org.projects.app.orchestrator.uniqueName
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import kotlin.io.encoding.Base64

class AppControllerTest : TestBase() {

    @SpykBean
    private lateinit var nodeAppService: NodeAppService

    @Test
    fun `deploys new app`() {
        val name = uniqueName()
        val response = sendDeployRequest(name, getLambdaCode())
        val appInfo = response.body!!

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(appInfo.name).isEqualTo(name)
        assertThat(appInfo.status).isEqualTo(NodeAppStatus.ACTIVE)
    }

    @Test
    fun `deploys existing app`() {
        val name = uniqueName()
        sendDeployRequest(name, getLambdaCode())
        val response = sendDeployRequest(name, getLambdaCode())
        val appInfo = response.body!!

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(appInfo.name).isEqualTo(name)
        assertThat(appInfo.status).isEqualTo(NodeAppStatus.ACTIVE)
    }

    @Test
    fun `bad request when archive is empty`() {
        val name = uniqueName()
        val emptyArchivedApp = ""

        val response = sendDeployRequest(name, emptyArchivedApp)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns conflict when app is already being deployed`() {
        every {
            nodeAppService.deploy(
                any(),
                any()
            )
        } returns AppServiceResponse(AppServiceResponseStatus.CONCURRENT_UPDATE)
        val response = sendDeployRequest(uniqueName(), getLambdaCode())

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    fun sendDeployRequest(name: String, archivedApp: String): ResponseEntity<AppControllerResponse?> {
        val request = DeployRequest(name, archivedApp)
        val response =
            restTemplate.postForEntity(baseUrl() + "/apps/deploy", request, AppControllerResponse::class.java)
        return response
    }

    @Test
    fun `returns status of an app`() {
        val name = uniqueName()
        sendDeployRequest(name, getLambdaCode())
        val response = restTemplate.getForEntity(baseUrl() + "/apps/$name/status", AppControllerResponse::class.java)
        val appInfo = response.body!!

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(appInfo.name).isEqualTo(name)
        assertThat(appInfo.status).isEqualTo(NodeAppStatus.ACTIVE)
    }

    @Test
    fun `status is not found when app is not deployed`() {
        val name = uniqueName()
        val response = restTemplate.getForEntity(baseUrl() + "/apps/$name/status", AppControllerResponse::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

}
