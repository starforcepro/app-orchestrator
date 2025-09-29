package org.projects.app.orchestrator.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projects.app.orchestrator.TestBase
import org.projects.app.orchestrator.getArchivedAppBase64
import org.projects.app.orchestrator.service.NodeAppService
import org.projects.app.orchestrator.uniqueName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.*


class ProxyControllerTest : TestBase() {

    @Autowired
    private lateinit var nodeAppService: NodeAppService

    private val createdAppsNames = mutableListOf<String>()

    @BeforeEach
    fun cleanup() {
        createdAppsNames.forEach { name -> nodeAppService.deactivate(name) }
        createdAppsNames.clear()
    }

    @Test
    fun `returns 404 when app is not found`() {
        val name = uniqueName()
        val hostBase = appUrl(name)

        val response = restTemplate.getForEntity("$hostBase/", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `returns 404 when app is not running`() {
        val name = uniqueName()
        deploy(name)
        nodeAppService.deactivate(name)
        createdAppsNames += name
        val hostBase = appUrl(name)

        val response = restTemplate.getForEntity("$hostBase/", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `successfully returns status code 400 when app returned 400`() {
        val name = uniqueName()
        deployAndRun(name)
        createdAppsNames += name
        val hostBase = appUrl(name)

        val response = restTemplate.getForEntity("$hostBase/notFound", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).isEqualTo("bad request text")
    }

    @Test
    fun `proxies get request to entry point`() {
        val name = uniqueName()
        deployAndRun(name)
        createdAppsNames += name
        val appUrl = appUrl(name)

        val response = restTemplate.getForEntity(appUrl, String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo("success")
    }

    @Test
    fun `proxies get request with query params`() {
        val name = uniqueName()
        deployAndRun(name)
        createdAppsNames += name
        val appUrl = appUrl(name)
        val expectedText = UUID.randomUUID().toString()

        val url = "$appUrl/testProxyingWithQueryParams?text=$expectedText"
        val response = restTemplate.getForEntity(url, String::class.java)

        assertThat(response.body).isEqualTo(expectedText)
    }

    @Test
    fun `proxies post request with body`() {
        val name = uniqueName()
        deployAndRun(name)
        createdAppsNames += name
        val appUrl = appUrl(name)
        val expectedText = UUID.randomUUID().toString()

        val url = "$appUrl/testProxyingWithBody"
        val response = restTemplate.postForEntity(url, mapOf("text" to expectedText), String::class.java)

        assertThat(response.body).isEqualTo(expectedText)
    }

    private fun deployAndRun(name: String) {
        deploy(name)
        nodeAppService.activate(name)
    }

    private fun deploy(name: String) {
        val archivedApp = getArchivedAppBase64()
        nodeAppService.deploy(name, archivedApp)
    }
}
