package momosetkn.maigreko.introspector

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import momosetkn.JdbcDatabaseContainerDataSource
import momosetkn.MysqlDatabase
import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.change.AddUniqueConstraint
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.introspector.MysqlChangeGenerator
import momosetkn.maigreko.introspector.MysqlInfoService
import momosetkn.maigreko.introspector.MysqlTableInfo
import momosetkn.maigreko.sql.MysqlMigrateEngine
import java.sql.Connection
import javax.sql.DataSource

class MysqlChangeGeneratorIntegrationSpec : FunSpec({
    val logger = org.slf4j.LoggerFactory.getLogger(MysqlChangeGeneratorIntegrationSpec::class.java)

    lateinit var connection: Connection
    lateinit var dataSource: DataSource
    lateinit var mysqlInfoService: MysqlInfoService
    lateinit var changeGenerator: MysqlChangeGenerator

    beforeSpec {
        MysqlDatabase.start()
        val container = MysqlDatabase.startedContainer
        dataSource = JdbcDatabaseContainerDataSource(container)
        logger.info("Connected to MySQL: ${container.jdbcUrl}")
        connection = dataSource.connection
        mysqlInfoService = MysqlInfoService(dataSource)
        changeGenerator = MysqlChangeGenerator()
    }

    afterSpec {
        connection.close()
        MysqlDatabase.stop()
    }

    beforeTest {
        MysqlDatabase.clear()
    }

    afterTest {
        logger.info("Generated DDL:")
        logger.info(MysqlDatabase.generateDdl())
    }

    fun subject(
        connection: Connection
    ): Triple<List<Change>, String, String> {
        val tableInfos = mysqlInfoService.fetchAll()

        // Generate changes
        val changes = changeGenerator.generateChanges(
            tableInfos = tableInfos
        )

        // Store the original DDL for comparison
        val originalDdl = MysqlDatabase.generateDdl()

        // Clear the database
        MysqlDatabase.clear()

        // Apply the changes using MysqlMigrateEngine
        changes.forEach { change ->
            val ddl = MysqlMigrateEngine.forwardDdl(change)
            connection.createStatement().use { statement ->
                statement.execute(ddl)
            }
        }

        // Get the resulting DDL
        val resultingDdl = MysqlDatabase.generateDdl()
        return Triple(changes, originalDdl, resultingDdl)
    }

    fun String.removeDumpCompletedLine(): String {
        val regex = Regex("\r?\n-- Dump completed on .*$")
        return this.replace(regex, "")
    }

    context("Single table schema") {
        test("should generate changes for a simple table") {
            // Create a test table
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE users (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(255) NOT NULL UNIQUE,
                        email VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """
                )
            }

            val (changes, originalDdl, resultingDdl) = subject(connection)

            // Verify changes
            changes.filterIsInstance<CreateTable>().size shouldBe 1

            val createTable = changes.filterIsInstance<CreateTable>().first()
            createTable.tableName shouldBe "users"
            createTable.columns.size shouldBe 4

            // Verify column names
            val columnNames = createTable.columns.map { it.name }
            columnNames shouldContainAll listOf("id", "username", "email", "created_at")

            // Verify constraints
            changes.filterIsInstance<AddNotNullConstraint>().size shouldBe 0 // include CreateTable change
            changes.filterIsInstance<AddUniqueConstraint>().size shouldBe 0 // include CreateTable change

            resultingDdl.removeDumpCompletedLine() shouldBe originalDdl.removeDumpCompletedLine()
        }
    }

    context("Tables with foreign key relationships") {
        test("should generate changes with proper dependency ordering") {
            // Create test tables with foreign key relationship
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE departments (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE
                    )
                """
                )

                statement.execute(
                    """
                    CREATE TABLE employees (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        department_id INT NOT NULL,
                        CONSTRAINT fk_department FOREIGN KEY (department_id) 
                            REFERENCES departments(id) ON DELETE CASCADE
                    )
                """
                )
            }

            val (changes, originalDdl, resultingDdl) = subject(connection)

            // Verify the order - departments should come before employees
            val deptTableIndex = changes.indexOfFirst {
                it is CreateTable && it.tableName == "departments"
            }
            val empTableIndex = changes.indexOfFirst {
                it is CreateTable && it.tableName == "employees"
            }

            // Departments should come before employees
            (deptTableIndex < empTableIndex) shouldBe true

            // Check if foreign keys are present in the sorted changes
            val foreignKeys = changes.filterIsInstance<AddForeignKey>()

            // If foreign keys are present, verify their details
            if (foreignKeys.isNotEmpty()) {
                val fkIndex = changes.indexOfFirst {
                    it is AddForeignKey && (it as AddForeignKey).tableName == "employees"
                }

                // Foreign key should come after both tables
                if (fkIndex >= 0) {
                    (fkIndex > deptTableIndex && fkIndex > empTableIndex) shouldBe true

                    val fk = foreignKeys.first { it.tableName == "employees" }
                    fk.referencedTableName shouldBe "departments"
                    fk.columnNames shouldBe listOf("department_id")
                    fk.referencedColumnNames shouldBe listOf("id")
                }
            }

            changes.filterIsInstance<AddNotNullConstraint>().size shouldBe 0 // include CreateTable change
            changes.filterIsInstance<AddUniqueConstraint>().size shouldBe 0 // include CreateTable change

            resultingDdl.removeDumpCompletedLine() shouldBe originalDdl.removeDumpCompletedLine()
        }
    }

    context("Validate changes by applying them to database") {
        test("should generate and apply changes for a simple table") {
            // Create a test table
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE users (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(255) NOT NULL UNIQUE,
                        email VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """
                )
            }

            val (changes, originalDdl, resultingDdl) = subject(connection)

            // Verify the DDL contains expected elements
            resultingDdl shouldContain "CREATE TABLE `users`"
            resultingDdl shouldContain "`id`"
            resultingDdl shouldContain "`username`"
            resultingDdl shouldContain "`email`"
            resultingDdl shouldContain "`created_at`"

            changes.filterIsInstance<AddNotNullConstraint>().size shouldBe 0 // include CreateTable change
            changes.filterIsInstance<AddUniqueConstraint>().size shouldBe 0 // include CreateTable change

            resultingDdl.removeDumpCompletedLine() shouldBe originalDdl.removeDumpCompletedLine()
        }

        test("should generate and apply changes for tables with relationships") {
            // Create test tables with foreign key relationship
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE departments (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE
                    )
                """
                )

                statement.execute(
                    """
                    CREATE TABLE employees (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        department_id INT NOT NULL,
                        CONSTRAINT fk_department FOREIGN KEY (department_id) 
                            REFERENCES departments(id) ON DELETE CASCADE
                    )
                """
                )
            }

            val (changes, originalDdl, resultingDdl) = subject(connection)

            // Verify the DDL contains expected elements
            resultingDdl shouldContain "CREATE TABLE `departments`"
            resultingDdl shouldContain "CREATE TABLE `employees`"
            resultingDdl shouldContain "`department_id`"

            changes.filterIsInstance<AddNotNullConstraint>().size shouldBe 0 // include CreateTable change
            changes.filterIsInstance<AddUniqueConstraint>().size shouldBe 0 // include CreateTable change

            resultingDdl.removeDumpCompletedLine() shouldBe originalDdl.removeDumpCompletedLine()
        }
    }

    context("Complex schema with multiple relationships") {
        test("should generate, apply, and validate changes for a complex schema") {
            // Create a more complex schema with multiple tables and relationships
            connection.createStatement().use { statement ->
                // Categories table
                statement.execute(
                    """
                    CREATE TABLE categories (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE
                    )
                """
                )

                // Products table with category foreign key
                statement.execute(
                    """
                    CREATE TABLE products (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        price DECIMAL(10,2) NOT NULL,
                        category_id INT NOT NULL,
                        CONSTRAINT fk_category FOREIGN KEY (category_id) 
                            REFERENCES categories(id) ON DELETE RESTRICT
                    )
                """
                )

                // Customers table
                statement.execute(
                    """
                    CREATE TABLE customers (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        email VARCHAR(255) NOT NULL UNIQUE
                    )
                """
                )

                // Orders table with customer foreign key
                statement.execute(
                    """
                    CREATE TABLE orders (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        customer_id INT NOT NULL,
                        CONSTRAINT fk_customer FOREIGN KEY (customer_id) 
                            REFERENCES customers(id) ON DELETE CASCADE
                    )
                """
                )

                // Order items table with order and product foreign keys
                statement.execute(
                    """
                    CREATE TABLE order_items (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        order_id INT NOT NULL,
                        product_id INT NOT NULL,
                        quantity INT NOT NULL,
                        CONSTRAINT fk_order FOREIGN KEY (order_id) 
                            REFERENCES orders(id) ON DELETE CASCADE,
                        CONSTRAINT fk_product FOREIGN KEY (product_id) 
                            REFERENCES products(id) ON DELETE RESTRICT
                    )
                """
                )
            }
            val (changes, originalDdl, resultingDdl) = subject(connection)

            // Verify the DDL contains expected elements for all tables
            resultingDdl shouldContain "CREATE TABLE `categories`"
            resultingDdl shouldContain "CREATE TABLE `products`"
            resultingDdl shouldContain "CREATE TABLE `customers`"
            resultingDdl shouldContain "CREATE TABLE `orders`"
            resultingDdl shouldContain "CREATE TABLE `order_items`"

            // Verify column names
            resultingDdl shouldContain "`category_id`"
            resultingDdl shouldContain "`customer_id`"
            resultingDdl shouldContain "`order_id`"
            resultingDdl shouldContain "`product_id`"

            changes.filterIsInstance<AddNotNullConstraint>().size shouldBe 0 // include CreateTable change
            changes.filterIsInstance<AddUniqueConstraint>().size shouldBe 0 // include CreateTable change

            resultingDdl.removeDumpCompletedLine() shouldBe originalDdl.removeDumpCompletedLine()
        }
    }

    context("AUTO_INCREMENT column types") {
        test("should handle MySQL AUTO_INCREMENT column types and constraints") {
            // Create tables with different column types and constraints
            connection.createStatement().use { statement ->
                // 1. Create a table with AUTO_INCREMENT types
                statement.execute(
                    """
                    CREATE TABLE auto_increment_types (
                        id_tinyint TINYINT AUTO_INCREMENT PRIMARY KEY,
                        id_smallint SMALLINT NOT NULL,
                        id_int INT NOT NULL,
                        id_bigint BIGINT NOT NULL
                    )
                    """
                )

                // 2. Create tables with foreign keys
                statement.execute(
                    """
                    CREATE TABLE departments (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE
                    )
                    """
                )

                statement.execute(
                    """
                    CREATE TABLE employees (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        department_id INT NOT NULL,
                        CONSTRAINT fk_department FOREIGN KEY (department_id) 
                            REFERENCES departments(id) ON DELETE CASCADE
                    )
                    """
                )

                statement.execute(
                    """
                    CREATE TABLE projects (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        department_id INT NOT NULL,
                        manager_id INT NOT NULL,
                        CONSTRAINT fk_dept FOREIGN KEY (department_id) 
                            REFERENCES departments(id) ON DELETE RESTRICT,
                        CONSTRAINT fk_manager FOREIGN KEY (manager_id) 
                            REFERENCES employees(id) ON DELETE RESTRICT
                    )
                    """
                )
            }

            val (changes, originalDdl, resultingDdl) = subject(connection)

            // Verify AUTO_INCREMENT types
            val autoIncrementTable =
                changes.filterIsInstance<CreateTable>().first { it.tableName == "auto_increment_types" }

            // Check column types
            val tinyintColumn = autoIncrementTable.columns.first { it.name == "id_tinyint" }
            val smallintColumn = autoIncrementTable.columns.first { it.name == "id_smallint" }
            val intColumn = autoIncrementTable.columns.first { it.name == "id_int" }
            val bigintColumn = autoIncrementTable.columns.first { it.name == "id_bigint" }

            tinyintColumn.type shouldBe "tinyint"
            smallintColumn.type shouldBe "smallint"
            intColumn.type shouldBe "int"
            bigintColumn.type shouldBe "bigint"

            // Check auto-increment properties
            tinyintColumn.autoIncrement shouldBe true
            smallintColumn.autoIncrement shouldBe false
            intColumn.autoIncrement shouldBe false
            bigintColumn.autoIncrement shouldBe false

            resultingDdl.removeDumpCompletedLine() shouldBe originalDdl.removeDumpCompletedLine()
        }
    }
})
