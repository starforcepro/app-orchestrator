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
            return AppServiceResponse(AppServiceResponseStatus.ALREADY_EXISTS)
        } catch (e: RuntimeException) {
            logger.warn { "Failed to create function $name: ${e.message}" }
            return AppServiceResponse(AppServiceResponseStatus.UNKNOWN_ERROR)
        } catch (e: Exception) {
            logger.error(e) { "Failed to deploy lambda $name" }
            return AppServiceResponse(AppServiceResponseStatus.UNKNOWN_ERROR)
        }
        val now = LocalDateTime.now()
        val applicationInfo = ApplicationInfo(
            name = name,
            status = NodeAppStatus.DEPLOYED,
            createdAt = now,
            updatedAt = now
        )
        appRepository.upsert(applicationInfo)
        return applicationInfo.toAppServiceResponse()
    }

    fun update(name: String, archive: String): AppServiceResponse {
        try {
            lambdaFunctionHandler.update(name, archive)
        } catch (_: ResourceNotFoundException) {
            logger.debug { "Lambda $name does not exist" }
            return AppServiceResponse(AppServiceResponseStatus.DOES_NOT_EXIST)
        } catch (_: ResourceConflictException) {
            logger.debug { "another update is in progress $name " }
            return AppServiceResponse(AppServiceResponseStatus.CONCURRENT_UPDATE)
        } catch (e: RuntimeException) {
            logger.warn { "Failed to create function $name: ${e.message}" }
            return AppServiceResponse(AppServiceResponseStatus.UNKNOWN_ERROR)
        } catch (e: Exception) {
            logger.error(e) { "Failed to update lambda $name" }
            return AppServiceResponse(AppServiceResponseStatus.UNKNOWN_ERROR)
        }

        val now = LocalDateTime.now()
        val applicationInfo = ApplicationInfo(
            name = name,
            status = NodeAppStatus.DEPLOYED,
            createdAt = now,
            updatedAt = now
        )
        appRepository.upsert(applicationInfo)
        return applicationInfo.toAppServiceResponse()
    }

    fun start(name: String): AppServiceResponse {
        val nodeApp = appRepository.findByName(name)
            ?: return AppServiceResponse(AppServiceResponseStatus.NOT_FOUND)
        val startedApp = appRepository.upsert(
            nodeApp.copy(
                status = NodeAppStatus.RUNNING,
                updatedAt = LocalDateTime.now()
            )
        )
        appRepository.upsert(startedApp)
        return startedApp.toAppServiceResponse()
    }

    fun stop(name: String): AppServiceResponse {
        val nodeApp = appRepository.findByName(name)
            ?: return AppServiceResponse(AppServiceResponseStatus.NOT_FOUND)

        val stoppedApp = appRepository.upsert(
            nodeApp.copy(
                status = NodeAppStatus.STOPPED,
                updatedAt = LocalDateTime.now()
            )
        )
        appRepository.upsert(stoppedApp)
        return stoppedApp.toAppServiceResponse()
    }

    fun remove(name: String): AppServiceResponse {
        val nodeApp = appRepository.findByName(name)
            ?: return AppServiceResponse(AppServiceResponseStatus.NOT_FOUND)
        lambdaFunctionHandler.remove(name)
        val removedApp = appRepository.upsert(
            nodeApp.copy(
                status = NodeAppStatus.REMOVED,
                updatedAt = LocalDateTime.now()
            )
        )
        appRepository.upsert(removedApp)
        return removedApp.toAppServiceResponse()
    }
}

