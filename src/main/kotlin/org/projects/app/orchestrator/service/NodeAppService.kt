package org.projects.app.orchestrator.service

import mu.KLogging
import org.projects.app.orchestrator.handler.AwsLambda
import org.projects.app.orchestrator.handler.LambdaFunctionHandler
import org.projects.app.orchestrator.model.*
import org.projects.app.orchestrator.repository.AppRepository
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.lambda.model.PackageType
import software.amazon.awssdk.services.lambda.model.ResourceConflictException
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException
import software.amazon.awssdk.services.lambda.model.Runtime
import java.time.LocalDateTime

@Service
class NodeAppService(
    private val appRepository: AppRepository,
    private val lambdaFunctionHandler: LambdaFunctionHandler,
) {
    companion object : KLogging()

    fun getByName(name: String): AppServiceResponse {
        return appRepository.findByName(name).toAppServiceResponse()
    }

    fun deploy(name: String, archive: String): AppServiceResponse {
        if (lambdaFunctionHandler.get(name) == null) {
            return create(name, archive)
        }
        return update(name, archive)
    }

    fun create(name: String, archive: String): AppServiceResponse {
        val lambda = AwsLambda(
            name = name,
            handler = "index.handler",
            packageType = PackageType.ZIP,
            runtime = Runtime.NODEJS20_X,
            packageFileBase64 = archive
        )
        try {
            lambdaFunctionHandler.create(lambda)
        } catch (_: ResourceConflictException) {
            logger.debug { "Lambda $name already exists" }
            return AppServiceResponse(AppServiceResponseStatus.CONCURRENT_UPDATE)
        } catch (e: Exception) {
            logger.error(e) { "Failed to deploy lambda $name" }
            return AppServiceResponse(AppServiceResponseStatus.UNKNOWN_ERROR)
        }
        val now = LocalDateTime.now()
        val applicationInfo = ApplicationInfo(
            name = name,
            status = NodeAppStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )
        appRepository.create(applicationInfo)
        return applicationInfo.toAppServiceResponse()
    }

    fun update(name: String, archive: String): AppServiceResponse {
        try {
            lambdaFunctionHandler.update(name, archive)
        } catch (_: ResourceNotFoundException) {
            logger.debug { "Lambda $name does not exist" }
            return AppServiceResponse(AppServiceResponseStatus.NOT_FOUND)
        } catch (_: ResourceConflictException) {
            logger.debug { "another update is in progress $name " }
            return AppServiceResponse(AppServiceResponseStatus.CONCURRENT_UPDATE)
        } catch (e: Exception) {
            logger.error(e) { "Failed to update lambda $name" }
            return AppServiceResponse(AppServiceResponseStatus.UNKNOWN_ERROR)
        }

        val now = LocalDateTime.now()
        val applicationInfo = ApplicationInfo(
            name = name,
            status = NodeAppStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )
        appRepository.update(applicationInfo)
        return applicationInfo.toAppServiceResponse()
    }

    fun activate(name: String): AppServiceResponse {
        val nodeApp = appRepository.findByName(name)
            ?: return AppServiceResponse(AppServiceResponseStatus.NOT_FOUND)
        val activatedApp = appRepository.update(
            nodeApp.copy(
                status = NodeAppStatus.ACTIVE,
                updatedAt = LocalDateTime.now()
            )
        )
        return activatedApp.toAppServiceResponse()
    }

    fun deactivate(name: String): AppServiceResponse {
        val nodeApp = appRepository.findByName(name)
            ?: return AppServiceResponse(AppServiceResponseStatus.NOT_FOUND)

        val deactivatedApp = appRepository.update(
            nodeApp.copy(
                status = NodeAppStatus.INACTIVE,
                updatedAt = LocalDateTime.now()
            )
        )
        return deactivatedApp.toAppServiceResponse()
    }

    fun remove(name: String): AppServiceResponse {
        val nodeApp = appRepository.findByName(name)
            ?: return AppServiceResponse(AppServiceResponseStatus.NOT_FOUND)
        lambdaFunctionHandler.remove(name)
        val removedApp = appRepository.update(
            nodeApp.copy(
                status = NodeAppStatus.REMOVED,
                updatedAt = LocalDateTime.now()
            )
        )
        return removedApp.toAppServiceResponse()
    }
}

