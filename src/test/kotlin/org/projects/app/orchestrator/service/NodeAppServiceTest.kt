package org.projects.app.orchestrator.service

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projects.app.orchestrator.*
import org.projects.app.orchestrator.handler.LambdaFunctionHandler
import org.projects.app.orchestrator.model.AppServiceResponseStatus
import org.projects.app.orchestrator.model.NodeAppStatus
import org.projects.app.orchestrator.repository.AppRepository
import org.springframework.beans.factory.annotation.Autowired

class NodeAppServiceTest : TestBase() {

    @SpykBean
    lateinit var nodeAppService: NodeAppService

    @SpykBean
    lateinit var lambdaFunctionHandler: LambdaFunctionHandler

    @Autowired
    lateinit var appRepository: AppRepository

    private val createdAppsNames = mutableListOf<String>()

    @BeforeEach
    fun cleanup() {
        createdAppsNames.forEach { name -> nodeAppService.remove(name) }
        createdAppsNames.clear()
    }

    @Test
    fun `deploy stores app`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()

        nodeAppService.deploy(name, archivedAppBase64)
        val response = nodeAppService.deploy(name, archivedAppBase64)
        createdAppsNames += name

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
        val app = response.applicationInfo!!
        assertThat(app.name).isEqualTo(name)
        assertThat(app.status).isEqualTo(NodeAppStatus.DEPLOYED)
    }

    @Test
    fun `deploy calls stop before deploy`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()

        nodeAppService.deploy(name, archivedAppBase64)
        createdAppsNames += name

        verify { nodeAppService.deactivate(any()) }
    }

    @Test
    fun `start runs node app and sets port and pid`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()

        nodeAppService.deploy(name, archivedAppBase64)
        createdAppsNames += name

        val nodeAppServiceResult = nodeAppService.activate(name)

        assertThat(nodeAppServiceResult.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
        val appInfo = nodeAppServiceResult.applicationInfo!!
        assertThat(appInfo.status).isEqualTo(NodeAppStatus.ACTIVE)
    }

    @Test
    fun `start returns NOT_FOUND if app is not deployed`() {
        val name = uniqueName()

        val nodeAppServiceResult = nodeAppService.activate(name)

        assertThat(nodeAppServiceResult.status).isEqualTo(AppServiceResponseStatus.NOT_FOUND)
    }

    @Test
    fun `start does not attempt to start twice if already running`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()

        nodeAppService.deploy(name, archivedAppBase64)
        createdAppsNames += name
        nodeAppService.activate(name)

        val nodeAppServiceResult = nodeAppService.activate(name)

        assertThat(nodeAppServiceResult.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
    }

    @Test
    fun `stop stops node app and removes app from repository`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()

        nodeAppService.deploy(name, archivedAppBase64)
        nodeAppService.activate(name)

        val nodeAppServiceResponse = nodeAppService.deactivate(name)

        assertThat(nodeAppServiceResponse.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
        assertThat(nodeAppServiceResponse.applicationInfo!!.status).isEqualTo(NodeAppStatus.INACTIVE)
        assertThat(appRepository.findByName(name)).isNull()

    }

    @Test
    fun `stop returns NOT_FOUND if app is not deployed`() {
        val name = uniqueName()

        val nodeAppServiceResult = nodeAppService.deactivate(name)

        assertThat(nodeAppServiceResult.status).isEqualTo(AppServiceResponseStatus.NOT_FOUND)
    }
}
