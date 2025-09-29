package org.projects.app.orchestrator.service

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.projects.app.orchestrator.TestBase
import org.projects.app.orchestrator.getArchivedAppBase64
import org.projects.app.orchestrator.handler.LambdaFunctionHandler
import org.projects.app.orchestrator.model.AppServiceResponseStatus
import org.projects.app.orchestrator.model.NodeAppStatus
import org.projects.app.orchestrator.repository.AppRepository
import org.projects.app.orchestrator.uniqueName
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration
import software.amazon.awssdk.services.lambda.model.ResourceConflictException
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException

class NodeAppServiceTest : TestBase() {

    @SpykBean
    lateinit var nodeAppService: NodeAppService

    @SpykBean
    lateinit var lambdaFunctionHandler: LambdaFunctionHandler

    @Autowired
    lateinit var appRepository: AppRepository

    @Test
    fun `deploy stores app`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()

        val response = nodeAppService.deploy(name, archivedAppBase64)

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
        val app = appRepository.findByName(name)
        assertThat(app).isNotNull
        assertThat(app?.name).isEqualTo(name)
        assertThat(app?.status).isEqualTo(NodeAppStatus.ACTIVE)
    }

    @Test
    fun `start runs node app`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()
        nodeAppService.deploy(name, archivedAppBase64)
        nodeAppService.deactivate(name)

        val nodeAppServiceResult = nodeAppService.activate(name)

        assertThat(nodeAppServiceResult.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
        val app = appRepository.findByName(name)
        assertThat(app).isNotNull
        assertThat(app?.name).isEqualTo(name)
        assertThat(app?.status).isEqualTo(NodeAppStatus.ACTIVE)
    }

    @Test
    fun `stop stops node app`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()

        nodeAppService.deploy(name, archivedAppBase64)
        nodeAppService.activate(name)

        val nodeAppServiceResponse = nodeAppService.deactivate(name)

        assertThat(nodeAppServiceResponse.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
        val app = appRepository.findByName(name)
        assertThat(app).isNotNull
        assertThat(app?.name).isEqualTo(name)
        assertThat(app?.status).isEqualTo(NodeAppStatus.INACTIVE)
    }

    @Test
    fun `getByName returns NOT_FOUND when app does not exist`() {
        val name = uniqueName()

        val response = nodeAppService.getByName(name)

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.NOT_FOUND)
        assertThat(response.applicationInfo).isNull()
    }

    @Test
    fun `getByName returns app when exists`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()
        nodeAppService.deploy(name, archivedAppBase64)

        val response = nodeAppService.getByName(name)

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
        assertThat(response.applicationInfo?.name).isEqualTo(name)
    }

    @Test
    fun `remove marks app as REMOVED`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()
        nodeAppService.deploy(name, archivedAppBase64)

        val response = nodeAppService.remove(name)

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
        val app = appRepository.findByName(name)
        assertThat(app).isNotNull
        assertThat(app?.status).isEqualTo(NodeAppStatus.REMOVED)
    }

    @Test
    fun `remove returns NOT_FOUND when app missing`() {
        val response = nodeAppService.remove(uniqueName())
        assertThat(response.status).isEqualTo(AppServiceResponseStatus.NOT_FOUND)
    }

    @Test
    fun `deploy uses update path when lambda exists`() {
        val name = uniqueName()
        val archivedAppBase64 = getArchivedAppBase64()
        every { lambdaFunctionHandler.get(name) } returns null
        justRun { lambdaFunctionHandler.create(any()) }
        val createResponse = nodeAppService.deploy(name, archivedAppBase64)
        assertThat(createResponse.status).isEqualTo(AppServiceResponseStatus.SUCCESS)

        every { lambdaFunctionHandler.get(name) } returns mockk<FunctionConfiguration>()
        justRun { lambdaFunctionHandler.update(name, archivedAppBase64) }

        val updateResponse = nodeAppService.deploy(name, archivedAppBase64)

        assertThat(updateResponse.status).isEqualTo(AppServiceResponseStatus.SUCCESS)
        val app = appRepository.findByName(name)
        assertThat(app).isNotNull
        assertThat(app?.status).isEqualTo(NodeAppStatus.ACTIVE)
    }

    @Test
    fun `create returns CONCURRENT_UPDATE on conflict`() {
        val name = uniqueName()
        val archive = getArchivedAppBase64()
        every { lambdaFunctionHandler.create(any()) } throws ResourceConflictException.builder().message("conflict").build()

        val response = nodeAppService.create(name, archive)

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.CONCURRENT_UPDATE)
    }

    @Test
    fun `create returns UNKNOWN_ERROR on generic exception`() {
        val name = uniqueName()
        val archive = getArchivedAppBase64()
        every { lambdaFunctionHandler.create(any()) } throws RuntimeException("boom")

        val response = nodeAppService.create(name, archive)

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.UNKNOWN_ERROR)
    }

    @Test
    fun `update returns NOT_FOUND when lambda does not exist`() {
        val name = uniqueName()
        val archive = getArchivedAppBase64()
        every { lambdaFunctionHandler.update(name, archive) } throws ResourceNotFoundException.builder().message("nope").build()

        val response = nodeAppService.update(name, archive)

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.NOT_FOUND)
    }

    @Test
    fun `update returns CONCURRENT_UPDATE on conflict`() {
        val name = uniqueName()
        val archive = getArchivedAppBase64()
        every { lambdaFunctionHandler.update(name, archive) } throws ResourceConflictException.builder().message("conflict").build()

        val response = nodeAppService.update(name, archive)

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.CONCURRENT_UPDATE)
    }

    @Test
    fun `update returns UNKNOWN_ERROR on generic exception`() {
        val name = uniqueName()
        val archive = getArchivedAppBase64()
        every { lambdaFunctionHandler.update(name, archive) } throws RuntimeException("boom")

        val response = nodeAppService.update(name, archive)

        assertThat(response.status).isEqualTo(AppServiceResponseStatus.UNKNOWN_ERROR)
    }

    @Test
    fun `activate returns NOT_FOUND for missing app`() {
        val response = nodeAppService.activate(uniqueName())
        assertThat(response.status).isEqualTo(AppServiceResponseStatus.NOT_FOUND)
    }

    @Test
    fun `deactivate returns NOT_FOUND for missing app`() {
        val response = nodeAppService.deactivate(uniqueName())
        assertThat(response.status).isEqualTo(AppServiceResponseStatus.NOT_FOUND)
    }
}
