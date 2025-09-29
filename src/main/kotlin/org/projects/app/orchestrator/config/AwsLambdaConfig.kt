package org.projects.app.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient


@Configuration
class AwsLambdaConfig {

    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider? {
        return DefaultCredentialsProvider.create()
    }

    @Bean
    fun lambdaClient(awsCredentialsProvider: AwsCredentialsProvider?): LambdaClient? {
        return LambdaClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .build()
    }
}
