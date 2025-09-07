package momosetkn.maigreko.dialect

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import momosetkn.JdbcDatabaseContainerDataSource
import momosetkn.PostgresqlDatabase
import java.sql.Connection
import java.sql.DatabaseMetaData
import javax.sql.DataSource

/**
 * Integration test for DialectFactory.
 * Verifies that the ServiceLoader mechanism correctly loads the PostgresqlIntrospector implementation.
 */
class DialectFactoryIntegrationSpec : FunSpec({
    val logger = org.slf4j.LoggerFactory.getLogger(DialectFactoryIntegrationSpec::class.java)

    context("postgresql") {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        val jdbcDatabaseContainerDataSource = JdbcDatabaseContainerDataSource(container)
        logger.info("Connected to PostgreSQL: ${container.jdbcUrl}")

        afterSpec {
            PostgresqlDatabase.stop()
        }

        test("DialectFactory should load Dialect for 'postgresql' dialect") {
            // Act
            val dialect = DialectFactory.create(jdbcDatabaseContainerDataSource)

            // Assert
            dialect.shouldBeInstanceOf<PostgresqlDialect>()
        }

        test("DialectFactory.createFirst should load the first available Introspector") {
            // Act
            val dialect = DialectFactory.createFirst()

            // Assert
            dialect.shouldBeInstanceOf<PostgresqlDialect>()
            // Note: We can't assert which specific implementation will be loaded first,
            // as it depends on the order of discovery by ServiceLoader
        }
    }

    context("other dialects load by ServiceLoader") {
        val mockDataSource = mockk<DataSource>()
        val mockConnection = mockk<Connection>()
        val mockDatabaseMetaData = mockk<DatabaseMetaData>()
        every {
            mockDataSource.connection
        } answers { mockConnection }
        every {
            mockConnection.metaData
        } answers { mockDatabaseMetaData }
        every {
            mockConnection.close()
        } returns Unit
        every {
            mockDatabaseMetaData.databaseProductName
        } answers { "unknown" }
        every {
            mockDatabaseMetaData.driverName
        } answers { "unknown" }
        every {
            mockDatabaseMetaData.url
        } answers { "unknown" }

        test("DialectFactory should load any dialect") {
            // Act & Assert
            val dialect = DialectFactory.create(mockDataSource)

            dialect.shouldBeInstanceOf<Dialect>()
        }
    }
})
