package org.projects.app.orchestrator.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.http.HttpServletRequest
import mu.KLogging
import org.projects.app.orchestrator.handler.InvokeResult
import org.projects.app.orchestrator.handler.LambdaFunctionHandler
import org.projects.app.orchestrator.model.AppServiceResponseStatus
import org.projects.app.orchestrator.model.NodeAppStatus
import org.projects.app.orchestrator.service.NodeAppService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ProxyController(
    private val nodeAppService: NodeAppService,
    private val lambdaFunctionHandler: LambdaFunctionHandler,
    private val objectMapper: ObjectMapper,
) {
    companion object : KLogging()

    @RequestMapping("/**")
    fun proxyByHost(
        request: HttpServletRequest
    ): ResponseEntity<ByteArray> {
        val host = request.getHeader(HttpHeaders.HOST) ?: request.serverName
        val appName = host.substringBefore(".")

        val appServiceResponse = nodeAppService.getByName(appName)
        if (appServiceResponse.status != AppServiceResponseStatus.SUCCESS ||
            appServiceResponse.applicationInfo == null ||
            appServiceResponse.applicationInfo.status != NodeAppStatus.ACTIVE
        ) return ResponseEntity.notFound().build()

        val payload = buildLambdaEventPayload(request)
        val invokeResult = lambdaFunctionHandler.invoke(appName, payload)
        if (invokeResult.error != null) {
            logger.error { "Error invoking lambda $appName function: ${invokeResult.error}" }
            return ResponseEntity.badRequest().build()
        }

        return buildHttpResponse(invokeResult)
    }

    private fun buildHttpResponse(invokeResult: InvokeResult): ResponseEntity<ByteArray> {
        val lambdaResponse = objectMapper.readValue<ApiGatewayProxyResponse>(invokeResult.result)
        val headers = HttpHeaders()
        lambdaResponse.headers?.forEach { (k, v) -> headers.add(k, v) }
        val status = HttpStatus.valueOf(lambdaResponse.statusCode)
        val bodyBytes = (lambdaResponse.body ?: "").toByteArray()
        return ResponseEntity.status(status).headers(headers).body(bodyBytes)
    }

    private fun buildLambdaEventPayload(request: HttpServletRequest): String {
        val queryParams = request.parameterMap.mapValues { (_, v) -> v.firstOrNull() }
        val bodyBytes = request.inputStream.readAllBytes()
        val body = if (bodyBytes.isNotEmpty()) String(bodyBytes) else null
        val event = mapOf(
            "path" to request.requestURI,
            "httpMethod" to request.method,
            "queryStringParameters" to queryParams.ifEmpty { null },
            "headers" to request.headerNames.asSequence().associateWith { request.getHeader(it) },
            "body" to body
        )
        return objectMapper.writeValueAsString(event)
    }
}

private data class ApiGatewayProxyResponse(
    val statusCode: Int,
    val headers: Map<String, String>?,
    val body: String?
)
