package org.projects.app.orchestrator.repository

import org.projects.app.orchestrator.model.ApplicationInfo
import org.projects.app.orchestrator.model.NodeAppStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
class AppRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    fun <T> fetchList(mapper: (ResultSet) -> T): (ResultSet) -> List<T> = { rs: ResultSet ->
        val result = mutableListOf<T>()
        while (rs.next()) {
            result.add(mapper.invoke(rs))
        }
        result
    }

    fun <T> fetchSingle(mapper: (ResultSet) -> T): (ResultSet) -> T? = { rs: ResultSet ->
        if (rs.next()) {
            mapper.invoke(rs)
        } else {
            null
        }
    }

    fun ResultSet.toApplicationInfo() = ApplicationInfo(
        id = UUID.fromString(this.getString("id")),
        name = this.getString("name"),
        status = NodeAppStatus.valueOf(this.getString("status")),
        createdAt = this.getTimestamp("created_at").toLocalDateTime(),
        updatedAt = this.getTimestamp("updated_at").toLocalDateTime()
    )

    fun create(appInfo: ApplicationInfo): ApplicationInfo {
        val sql = """
            INSERT INTO application_info (id, name, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        jdbcTemplate.update(
            sql,

                appInfo.id,
                appInfo.name,
                appInfo.status.name,
                appInfo.createdAt,
                appInfo.updatedAt,
        )
        return appInfo
    }

    fun update(appInfo: ApplicationInfo): ApplicationInfo {
        val sql = """
            UPDATE application_info 
            SET status = ?, updated_at = ?
            WHERE name = ?
        """.trimIndent()
        jdbcTemplate.update(
            sql,
                appInfo.status.name,
            appInfo.updatedAt,
            appInfo.name,
        )
        return appInfo
    }

    fun findByName(name: String): ApplicationInfo? {
        val sql = """
            SELECT * FROM application_info WHERE name = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, fetchSingle { it.toApplicationInfo() }, name)
    }

    fun findAll(): List<ApplicationInfo> {
        val sql = """
            SELECT * FROM application_info
        """.trimIndent()
        return jdbcTemplate.query(sql, fetchList { it.toApplicationInfo() })
    }
}
