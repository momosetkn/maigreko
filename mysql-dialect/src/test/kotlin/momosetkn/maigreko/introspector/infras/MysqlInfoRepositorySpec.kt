package momosetkn.maigreko.introspector.infras

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import momosetkn.JdbcDatabaseContainerDataSource
import momosetkn.MysqlDatabase

class MysqlInfoRepositorySpec : FunSpec({
    lateinit var dataSource: javax.sql.DataSource
    lateinit var repository: MysqlInfoRepository

    beforeSpec {
        // Start MySQL container
        MysqlDatabase.start()

        // Create a test table
        dataSource = JdbcDatabaseContainerDataSource(MysqlDatabase.startedContainer)
        // Create the repository with the data source
        repository = MysqlInfoRepository(dataSource)
        val connection = dataSource.connection

        connection.use { conn ->
            val statement = conn.createStatement()

            // Create parent table for foreign key test
            statement.execute(
                """
                CREATE TABLE parent_table (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL
                )
                """.trimIndent()
            )

            // Insert data into parent table
            statement.execute(
                """
                INSERT INTO parent_table (name) VALUES ('Parent 1'), ('Parent 2')
                """.trimIndent()
            )

            // Create main test table with various column types and a foreign key
            statement.execute(
                """
                CREATE TABLE test_table (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    description TEXT,
                    unique_col VARCHAR(50) UNIQUE,
                    parent_id INT,
                    CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES parent_table(id) ON DELETE CASCADE ON UPDATE CASCADE
                )
                """.trimIndent()
            )
        }
    }

    afterSpec {
        // Clean up and stop the container
        MysqlDatabase.clear()
        MysqlDatabase.stop()
    }

    test("getTableList should return the test table") {

        // Get the table list
        val tables = repository.getTableList("non_existent_table")

        // Verify the test table is in the list
        tables shouldNotBe null
        tables shouldContain "test_table"
    }

    test("getConstraintDetails should return foreign key details") {
        // Get constraint details
        val constraints = repository.getConstraintDetails("non_existent_table")

        // Verify the constraint details
        constraints.find {
            it.constraintName == "fk_parent" &&
                it.tableName == "test_table" &&
                it.columnName == "parent_id" &&
                it.foreignTableName == "parent_table" &&
                it.foreignColumnName == "id" &&
                it.onUpdate == "CASCADE" &&
                it.onDelete == "CASCADE"
        } shouldNotBe null
    }

    test("getColumnDetails should return column information") {
        // Get column details
        val columns = repository.getColumnDetails("non_existent_table")

        // Find the test_table columns
        val testTableColumns = columns.filter { it.tableName == "test_table" }

        // Verify id column (primary key, auto increment)
        val idColumn = testTableColumns.find { it.columnName == "id" }
        idColumn shouldNotBe null
        idColumn?.primaryKey shouldBe "YES"
        idColumn?.notNull shouldBe "YES"
        idColumn?.generatedKind shouldBe "AUTO_INCREMENT"

        // Verify name column (not null)
        val nameColumn = testTableColumns.find { it.columnName == "name" }
        nameColumn shouldNotBe null
        nameColumn?.notNull shouldBe "YES"
        nameColumn?.type shouldBe "varchar(100)"

        // Verify unique column
        val uniqueColumn = testTableColumns.find { it.columnName == "unique_col" }
        uniqueColumn shouldNotBe null
        uniqueColumn?.unique shouldBe "YES"

        // Verify foreign key column
        val fkColumn = testTableColumns.find { it.columnName == "parent_id" }
        fkColumn shouldNotBe null
        fkColumn?.foreignTable shouldBe "parent_table"
        fkColumn?.foreignColumn shouldBe "id"
    }
})
