package org.projects.app.orchestrator

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [AppOrchestratorApplication::class]
)
class TestBase {
    @Autowired
    lateinit var restTemplate: RestTemplate

    @LocalServerPort
    var port: Int = 0

    fun baseUrl() = "http://localhost:$port"
    fun appUrl(appName: String): String = "http://$appName.localhost:${port}"
}