package momosetkn.maigreko.introspector

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import momosetkn.JdbcDatabaseContainerDataSource
import momosetkn.PostgresqlDatabase
import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Integration test for IntrospectorFactory.
 * Verifies that the ServiceLoader mechanism correctly loads the PostgresqlIntrospector implementation.
 */
class IntrospectorFactoryIntegrationSpec : FunSpec({
    val logger = org.slf4j.LoggerFactory.getLogger(IntrospectorFactoryIntegrationSpec::class.java)

    context("postgresql") {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        val jdbcDatabaseContainerDataSource = JdbcDatabaseContainerDataSource(container)
        logger.info("Connected to PostgreSQL: ${container.jdbcUrl}")

        afterSpec {
            PostgresqlDatabase.stop()
        }

        test("IntrospectorFactory should load PostgresqlIntrospector for 'postgresql' dialect") {
            // Act
            val introspector = IntrospectorFactory.create("postgresql", jdbcDatabaseContainerDataSource)

            // Assert
            introspector.shouldBeInstanceOf<PostgresqlIntrospector>()
        }

        test("IntrospectorFactory should load PostgresqlIntrospector for 'postgre' dialect (partial match)") {
            // Act
            val introspector = IntrospectorFactory.create("postgre", jdbcDatabaseContainerDataSource)

            // Assert
            introspector.shouldBeInstanceOf<PostgresqlIntrospector>()
        }

        test("IntrospectorFactory should automatically detect PostgreSQL dialect from database metadata") {
            // Act
            val introspector = IntrospectorFactory.create(jdbcDatabaseContainerDataSource)

            // Assert
            introspector.shouldBeInstanceOf<PostgresqlIntrospector>()
        }

        test("IntrospectorFactory.createFirst should load the first available Introspector") {
            // Act
            val introspector = IntrospectorFactory.createFirst(jdbcDatabaseContainerDataSource)

            // Assert
            introspector.shouldBeInstanceOf<Introspector>()
            // Note: We can't assert which specific implementation will be loaded first,
            // as it depends on the order of discovery by ServiceLoader
        }
    }

    context("other dialects") {
        test("IntrospectorFactory should throw exception for unknown dialect") {
            // Act & Assert
            val e = shouldThrow<IllegalArgumentException> {
                IntrospectorFactory.create("unknown", DummyDataSource)
            }
            e.message shouldBe "No Introspector found for dialect: unknown"
        }
    }
})

object DummyDataSource : DataSource {
    override fun getConnection(): Connection = TODO()
    override fun getConnection(username: String, password: String): Connection = TODO()
    override fun getLogWriter(): PrintWriter = TODO()
    override fun setLogWriter(out: PrintWriter) = TODO()
    override fun setLoginTimeout(seconds: Int) = TODO()
    override fun getLoginTimeout(): Int = TODO()
    override fun getParentLogger(): Logger = TODO()
    override fun <T : Any> unwrap(iface: Class<T>): T = TODO()
    override fun isWrapperFor(iface: Class<*>): Boolean = TODO()
}
