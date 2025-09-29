package org.projects.app.orchestrator.handler

import mu.KLogging
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.*
import java.time.Instant
import kotlin.io.encoding.Base64

@Component
class LambdaFunctionHandler(
    val lambdaClient: LambdaClient,
) {
    companion object : KLogging()

    fun invoke(functionName: String, jsonPayload: String): InvokeResult {
        val invokeRequest = InvokeRequest.builder()
            .functionName(functionName)
            .payload(SdkBytes.fromUtf8String(jsonPayload))
            .build()

        val result = lambdaClient.invoke(invokeRequest)

        return InvokeResult(
            result = result.payload().asUtf8String(),
            error = result.functionError(),
        )
    }

    fun invoke(lambda: AwsLambda, jsonPayload: String): InvokeResult = invoke(lambda.name, jsonPayload)

    fun create(awsLambda: AwsLambda) {
        val role = System.getenv("AWS_LAMBDA_ROLE_ARN")
            ?: throw IllegalArgumentException("AWS_LAMBDA_ROLE_ARN not set")
        val createRequest = CreateFunctionRequest.builder()
            .functionName(awsLambda.name)
            .runtime(awsLambda.runtime)
            .handler(awsLambda.handler)
            .role(role)
            .packageType(awsLambda.packageType)
            .architectures(Architecture.ARM64)
            .code { it.zipFile(SdkBytes.fromByteArray(Base64.decode(awsLambda.packageFileBase64))) }
            .timeout(60)
            .build()

        val response = lambdaClient.createFunction(createRequest)
        if (!response.sdkHttpResponse().isSuccessful) throw RuntimeException("Failed to create function")
        waitForLambdaActivation(awsLambda.name)
    }

    fun update(name: String, archive: String) {
        if (get(name) == null) throw IllegalArgumentException("Function does not exist")
        val updateRequest = UpdateFunctionCodeRequest.builder()
            .functionName(name)
            .zipFile(SdkBytes.fromByteArray(Base64.decode(archive)))
            .build()

        val response = lambdaClient.updateFunctionCode(updateRequest)
        if (!response.sdkHttpResponse().isSuccessful) throw RuntimeException("Failed to update function")
        waitForLambdaActivation(name)
    }


    fun remove(name: String) {
        val request = DeleteFunctionRequest.builder().functionName(name).build()
        lambdaClient.deleteFunction(request)
    }

    fun get(name: String): FunctionConfiguration? {
        val request = GetFunctionRequest.builder().functionName(name).build()
        val response = try {
            lambdaClient.getFunction(request)
        } catch (_: ResourceNotFoundException) {
            return null
        }
        if (response.sdkHttpResponse().isSuccessful) return response.configuration()
        return null
    }

    private fun waitForLambdaActivation(name: String) {
        val waitTimeMilliseconds = 60000L
        val waitUntilTime = Instant.now().plusMillis(waitTimeMilliseconds)
        while (Instant.now().isBefore(waitUntilTime)) {
            val function = get(name)
            if (function != null && function.state() == State.ACTIVE) return
            Thread.sleep(100)
        }
        throw IllegalStateException("Function $name never became active")
    }
}

data class InvokeResult(
    val result: String,
    val error: String? = null
)

data class AwsLambda(
    val name: String,
    val runtime: Runtime,
    val packageType: PackageType,
    val packageFileBase64: String,
    val handler: String = "index.handler",
)