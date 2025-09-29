package org.projects.app.orchestrator.repository

import org.projects.app.orchestrator.model.ApplicationInfo
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class AppRepository() {
    private val repository = ConcurrentHashMap<String, ApplicationInfo>()

    fun upsert(appInfo: ApplicationInfo): ApplicationInfo {
        repository[appInfo.name] = appInfo
        return appInfo
    }

    fun findByName(name: String): ApplicationInfo? {
        return repository[name]
    }

    fun findAll(): List<ApplicationInfo> {
        return repository.values.toList()
    }

    fun deleteByName(name: String) {
        repository.remove(name)
    }
}
