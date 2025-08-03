package momosetkn.maigreko.db

import org.testcontainers.containers.Container
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import kotlin.use

object PostgresqlDatabase {
    private var container: PostgreSQLContainer<*>? = null
    val startedContainer
        get() = requireNotNull(container) {
            "momosetkn.Database is not started"
        }

    fun start() {
        val image =
            org.testcontainers.utility.DockerImageName.parse("postgres:15.8")
        container = PostgreSQLContainer(image)
        val launchTime =
            kotlin.system.measureTimeMillis { startedContainer.start() }

        println("database started in $launchTime ms")
    }

    fun executeCommand(vararg args: String): Container.ExecResult {
        return startedContainer.execInContainer(*args)
    }

    fun stop() {
        container?.stop()
        println("database stop")
    }

    @Synchronized
    fun clear() {
        getConnection().use {
            val statement = it.createStatement()
            val ddl = "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
            statement.execute(ddl)
        }
    }

    private fun getConnection() =
        DriverManager.getConnection(startedContainer.jdbcUrl, startedContainer.username, startedContainer.password)

    fun generateDdl(): String? {
        val commandResult = executeCommand(
            "pg_dump",
            "-h",
            "localhost",
            "-p",
            PostgreSQLContainer.POSTGRESQL_PORT.toString(),
            "-U",
            startedContainer.username,
            "-s",
        )
        check(commandResult.exitCode == 0 && commandResult.stdout.isNotEmpty()) {
            """ 
                exitCode is ${commandResult.exitCode}       
                stdout is ${commandResult.stdout}       
            """.trimIndent()
        }
        return commandResult.stdout
    }
}
