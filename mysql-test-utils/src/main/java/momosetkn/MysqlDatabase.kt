package momosetkn

import org.testcontainers.containers.Container
import org.testcontainers.containers.MySQLContainer
import java.sql.DriverManager
import kotlin.system.measureTimeMillis

object MysqlDatabase {
    private val logger = org.slf4j.LoggerFactory.getLogger(MysqlDatabase::class.java)

    private var container: MySQLContainer<*>? = null
    val startedContainer
        get() = requireNotNull(container) {
            "momosetkn.Database is not started"
        }

    fun start() {
        val image =
            org.testcontainers.utility.DockerImageName.parse("mysql:latest")
        container = MySQLContainer(image)
        val launchTime = measureTimeMillis { startedContainer.start() }

        logger.info("database started in $launchTime ms")
    }

    fun executeCommand(vararg args: String): Container.ExecResult {
        return startedContainer.execInContainer(*args)
    }

    fun stop() {
        container?.stop()
        logger.info("database stop")
    }

    @Synchronized
    fun clear() {
        getConnection().use {
            val statement = it.createStatement()
            // Get all tables
            val rs = statement.executeQuery("SHOW TABLES")
            val tables = mutableListOf<String>()
            while (rs.next()) {
                tables.add(rs.getString(1))
            }

            // Drop all tables
            if (tables.isNotEmpty()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0")
                tables.forEach { table ->
                    statement.execute("DROP TABLE IF EXISTS `$table`")
                }
                statement.execute("SET FOREIGN_KEY_CHECKS = 1")
            }
        }
    }

    private fun getConnection() =
        DriverManager.getConnection(startedContainer.jdbcUrl, startedContainer.username, startedContainer.password)

    fun generateDdl(): String {
        val commandResult = executeCommand(
            "mysqldump",
            "-h",
            "localhost",
            "-P",
            MySQLContainer.MYSQL_PORT.toString(),
            "-u",
            startedContainer.username,
            "-p${startedContainer.password}",
            "--no-data",
            startedContainer.databaseName
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
