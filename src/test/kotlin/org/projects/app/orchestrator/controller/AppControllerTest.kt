package org.projects.app.orchestrator.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.projects.app.orchestrator.TestBase
import org.projects.app.orchestrator.expressJsFileContent
import org.projects.app.orchestrator.model.AppControllerResponse
import org.projects.app.orchestrator.model.NodeAppStatus
import org.projects.app.orchestrator.toZipByteArray
import org.projects.app.orchestrator.uniqueName
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import kotlin.io.encoding.Base64

class AppControllerTest : TestBase() {

    @Test
    fun `deploys app when request is valid`() {
        val name = uniqueName()
        val archivedApp = expressJsFileContent.toByteArray().toZipByteArray()

        val response = sendDeployRequest(name, archivedApp)
        val appInfo = response.body!!

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(appInfo.name).isEqualTo(name)
        assertThat(appInfo.status).isEqualTo(NodeAppStatus.DEPLOYED)
    }

    @Test
    fun `bad request when archive is empty`() {
        val name = uniqueName()
        val emptyArchivedApp = ByteArray(0)

        val response = sendDeployRequest(name, emptyArchivedApp)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    fun sendDeployRequest(name: String, archivedApp: ByteArray): ResponseEntity<AppControllerResponse?> {
        val request = DeployRequest(name, Base64.encode(archivedApp))
        val response =
            restTemplate.postForEntity(baseUrl() + "/apps/deploy", request, AppControllerResponse::class.java)
        return response
    }
}
