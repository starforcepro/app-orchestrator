package org.projects.app.orchestrator.model

class AppServiceResponse(
    val status: AppServiceResponseStatus,
    val applicationInfo: ApplicationInfo? = null
)

enum class AppServiceResponseStatus {
    NOT_FOUND,
    CONCURRENT_UPDATE,
    SUCCESS,
    UNKNOWN_ERROR
}

fun ApplicationInfo?.toAppServiceResponse(): AppServiceResponse {
    return if (this == null) {
        AppServiceResponse(AppServiceResponseStatus.NOT_FOUND)
    } else {
        AppServiceResponse(AppServiceResponseStatus.SUCCESS, this)
    }
}