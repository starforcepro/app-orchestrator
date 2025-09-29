package org.projects.app.orchestrator.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executors
import java.util.function.Supplier

class NoOpResponseErrorHandler : ResponseErrorHandler {
    override fun hasError(response: ClientHttpResponse): Boolean = false

    override fun handleError(url: URI, method: HttpMethod, response: ClientHttpResponse) {
        //noop
    }
}

@Configuration
class RestTemplateConfig {
    @Bean
    fun restTemplate(
        builder: RestTemplateBuilder,
    ): RestTemplate {
        val httpClient = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        return builder
            .readTimeout(Duration.ofSeconds(30))
            .requestFactory(Supplier { JdkClientHttpRequestFactory(httpClient) })
            .errorHandler(NoOpResponseErrorHandler())
            .build()
    }
}
