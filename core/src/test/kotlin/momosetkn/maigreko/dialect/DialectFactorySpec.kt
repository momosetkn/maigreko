package momosetkn.maigreko.dialect

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.sql.Connection
import java.sql.DatabaseMetaData
import javax.sql.DataSource

class DialectFactorySpec : FunSpec({
    context("other dialects and nothing load by ServiceLoader") {
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

        test("DialectFactory should throw exception for unknown dialect") {
            // Act & Assert
            val e = shouldThrow<IllegalArgumentException> {
                DialectFactory.create(mockDataSource)
            }
            e.message shouldBe "No Dialect implementation found"
        }
    }
})
