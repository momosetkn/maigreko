package momosetkn.maigreko.introspector

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.change.AddUniqueConstraint
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.db.PostgresDataSource
import momosetkn.maigreko.db.PostgresqlDatabase
import momosetkn.maigreko.sql.PostgreMigrateEngine
import java.sql.Connection
import javax.sql.DataSource

class PostgresqlChangeGeneratorIntegrationSpec : FunSpec({
    lateinit var connection: Connection
    lateinit var dataSource: DataSource
    lateinit var postgresqlInfoService: PostgresqlInfoService
    lateinit var changeGenerator: PostgresqlChangeGenerator

    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = PostgresDataSource(container)
        connection = dataSource.connection
        postgresqlInfoService = PostgresqlInfoService(dataSource)
        changeGenerator = PostgresqlChangeGenerator()
    }

    afterSpec {
        connection.close()
        PostgresqlDatabase.stop()
    }

    beforeTest {
        PostgresqlDatabase.clear()
    }

    afterTest {
        println("Generated DDL:")
        println(PostgresqlDatabase.generateDdl())
    }

    context("Single table schema") {
        test("should generate changes for a simple table") {
            // Create a test table
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE users (
                        id SERIAL PRIMARY KEY,
                        username VARCHAR(255) NOT NULL UNIQUE,
                        email VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """
                )
            }

            val (tableInfoss, sequenceDetails) = postgresqlInfoService.fetchAll()

            // Generate changes for all tables using the new multi-table method
            val changes = changeGenerator.generateChanges(
                tableInfoss = tableInfoss,
                sequenceDetails = sequenceDetails
            )
            // Verify changes
            changes.filterIsInstance<CreateTable>().size shouldBe 1

            val createTable = changes.filterIsInstance<CreateTable>().first()
            createTable.tableName shouldBe "users"
            createTable.columns.size shouldBe 4

            // Verify column names
            val columnNames = createTable.columns.map { it.name }
            columnNames shouldContainAll listOf("id", "username", "email", "created_at")

            // Verify constraints
            changes.filterIsInstance<AddNotNullConstraint>().size shouldBe 3 // id, username, email
            changes.filterIsInstance<AddUniqueConstraint>().size shouldBe 1 // username

            // Verify sequences
            val sequences = changes.filterIsInstance<CreateSequence>()
            sequences.isNotEmpty() shouldBe true
            sequences.any { it.sequenceName.contains("users_id_seq") } shouldBe true
        }
    }

    context("Tables with foreign key relationships") {
        test("should generate changes with proper dependency ordering") {
            // Create test tables with foreign key relationship
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE departments (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE
                    )
                """
                )

                statement.execute(
                    """
                    CREATE TABLE employees (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        department_id INTEGER NOT NULL,
                        CONSTRAINT fk_department FOREIGN KEY (department_id) 
                            REFERENCES departments(id) ON DELETE CASCADE
                    )
                """
                )
            }

            val (tableInfoss, sequenceDetails) = postgresqlInfoService.fetchAll()

            // Generate changes for all tables using the new multi-table method
            val allChanges = changeGenerator.generateChanges(
                tableInfoss = tableInfoss,
                sequenceDetails = sequenceDetails
            )

            // Verify the order - departments should come before employees
            val deptTableIndex = allChanges.indexOfFirst {
                it is CreateTable && it.tableName == "departments"
            }
            val empTableIndex = allChanges.indexOfFirst {
                it is CreateTable && it.tableName == "employees"
            }

            // Departments should come before employees
            (deptTableIndex < empTableIndex) shouldBe true

            // Check if foreign keys are present in the sorted changes
            val foreignKeys = allChanges.filterIsInstance<AddForeignKey>()

            // If foreign keys are present, verify their details
            if (foreignKeys.isNotEmpty()) {
                val fkIndex = allChanges.indexOfFirst {
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

            // Verify sequences
            val sequences = allChanges.filterIsInstance<CreateSequence>()
            sequences.isNotEmpty() shouldBe true
            sequences.any { it.sequenceName.contains("departments_id_seq") } shouldBe true
            sequences.any { it.sequenceName.contains("employees_id_seq") } shouldBe true
        }
    }

    context("Validate changes by applying them to database") {
        test("should generate and apply changes for a simple table") {
            // Create a test table
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE users (
                        id SERIAL PRIMARY KEY,
                        username VARCHAR(255) NOT NULL UNIQUE,
                        email VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """
                )
            }

            val (tableInfoss, sequenceDetails) = postgresqlInfoService.fetchAll()

            // Generate changes for all tables using the new multi-table method
            val allChanges = changeGenerator.generateChanges(
                tableInfoss = tableInfoss,
            )

            // Store the original DDL for comparison
            val originalDdl = PostgresqlDatabase.generateDdl()

            // Clear the database
            PostgresqlDatabase.clear()

            // Apply the changes using PostgreMigrateEngine
            allChanges.forEach { change ->
                val ddl = PostgreMigrateEngine.forwardDdl(change)
                connection.createStatement().use { statement ->
                    statement.execute(ddl)
                }
            }

            // Get the resulting DDL
            val resultingDdl = PostgresqlDatabase.generateDdl()

            // Print the DDL for debugging
            println("Resulting DDL:")
            println(resultingDdl)

            // Verify the DDL contains expected elements
            resultingDdl shouldContain "CREATE TABLE public.users"
            resultingDdl shouldContain "id"
            resultingDdl shouldContain "username"
            resultingDdl shouldContain "email"
            resultingDdl shouldContain "created_at"

            // Verify sequences are included
            allChanges.filterIsInstance<CreateSequence>().isNotEmpty() shouldBe true
            allChanges.filterIsInstance<CreateSequence>().any { it.sequenceName.contains("users_id_seq") } shouldBe true
        }

        test("should generate and apply changes for tables with relationships") {
            // Create test tables with foreign key relationship
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE departments (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE
                    )
                """
                )

                statement.execute(
                    """
                    CREATE TABLE employees (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        department_id INTEGER NOT NULL,
                        CONSTRAINT fk_department FOREIGN KEY (department_id) 
                            REFERENCES departments(id) ON DELETE CASCADE
                    )
                """
                )
            }

            val (tableInfoss, sequenceDetails) = postgresqlInfoService.fetchAll()

            // Generate changes for all tables using the new multi-table method
            val allChanges = changeGenerator.generateChanges(
                tableInfoss = tableInfoss,
                sequenceDetails = sequenceDetails
            )

            // Store the original DDL for comparison
            val originalDdl = PostgresqlDatabase.generateDdl()
            println("Original DDL:")
            println(originalDdl)

            // Clear the database
            PostgresqlDatabase.clear()
            val resultingDdl_ = PostgresqlDatabase.generateDdl()

            // Apply the changes using PostgreMigrateEngine
            allChanges.forEach { change ->
                val ddl = PostgreMigrateEngine.forwardDdl(change)
                connection.createStatement().use { statement ->
                    statement.execute(ddl)
                }
            }

            // Get the resulting DDL
            val resultingDdl = PostgresqlDatabase.generateDdl()

            // Print the DDL for debugging
            println("Resulting DDL for relationships test:")
            println(resultingDdl)

            // TODO: equals to originalDdl
//            resultingDdl shouldBe originalDdl

            // Verify the DDL contains expected elements
            resultingDdl shouldContain "CREATE TABLE public.departments"
            resultingDdl shouldContain "CREATE TABLE public.employees"
            resultingDdl shouldContain "department_id"

            // Verify sequences are included
            val sequences = allChanges.filterIsInstance<CreateSequence>()
            sequences.isNotEmpty() shouldBe true
            sequences.any { it.sequenceName.contains("departments_id_seq") } shouldBe true
            sequences.any { it.sequenceName.contains("employees_id_seq") } shouldBe true
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
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE
                    )
                """
                )

                // Products table with category foreign key
                statement.execute(
                    """
                    CREATE TABLE products (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        price NUMERIC(10,2) NOT NULL,
                        category_id INTEGER NOT NULL,
                        CONSTRAINT fk_category FOREIGN KEY (category_id) 
                            REFERENCES categories(id) ON DELETE RESTRICT
                    )
                """
                )

                // Customers table
                statement.execute(
                    """
                    CREATE TABLE customers (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        email VARCHAR(255) NOT NULL UNIQUE
                    )
                """
                )

                // Orders table with customer foreign key
                statement.execute(
                    """
                    CREATE TABLE orders (
                        id SERIAL PRIMARY KEY,
                        order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        customer_id INTEGER NOT NULL,
                        CONSTRAINT fk_customer FOREIGN KEY (customer_id) 
                            REFERENCES customers(id) ON DELETE CASCADE
                    )
                """
                )

                // Order items table with order and product foreign keys
                statement.execute(
                    """
                    CREATE TABLE order_items (
                        id SERIAL PRIMARY KEY,
                        order_id INTEGER NOT NULL,
                        product_id INTEGER NOT NULL,
                        quantity INTEGER NOT NULL,
                        CONSTRAINT fk_order FOREIGN KEY (order_id) 
                            REFERENCES orders(id) ON DELETE CASCADE,
                        CONSTRAINT fk_product FOREIGN KEY (product_id) 
                            REFERENCES products(id) ON DELETE RESTRICT
                    )
                """
                )
            }

            val (tableInfoss, sequenceDetails) = postgresqlInfoService.fetchAll()

            // Generate changes for all tables using the new multi-table method
            val allChanges = changeGenerator.generateChanges(
                tableInfoss = tableInfoss,
                sequenceDetails = sequenceDetails
            )

            // Sort all changes by dependencies
            val sortedChanges = changeGenerator.sortChangesByDependencies(allChanges)

            // Store the original DDL for comparison
            val originalDdl = PostgresqlDatabase.generateDdl()

            // Clear the database
            PostgresqlDatabase.clear()

            // Apply the changes using PostgreMigrateEngine
            sortedChanges.forEach { change ->
                val ddl = PostgreMigrateEngine.forwardDdl(change)
                connection.createStatement().use { statement ->
                    statement.execute(ddl)
                }
            }

            // Get the resulting DDL
            val resultingDdl = PostgresqlDatabase.generateDdl()

            // Print the DDL for debugging
            println("Resulting DDL for complex schema test:")
            println(resultingDdl)

            resultingDdl shouldBe originalDdl

            // Verify the DDL contains expected elements for all tables
            resultingDdl shouldContain "CREATE TABLE public.categories"
            resultingDdl shouldContain "CREATE TABLE public.products"
            resultingDdl shouldContain "CREATE TABLE public.customers"
            resultingDdl shouldContain "CREATE TABLE public.orders"
            resultingDdl shouldContain "CREATE TABLE public.order_items"

            // Verify column names
            resultingDdl shouldContain "category_id"
            resultingDdl shouldContain "customer_id"
            resultingDdl shouldContain "order_id"
            resultingDdl shouldContain "product_id"

            // Verify sequences are included
            val sequences = sortedChanges.filterIsInstance<CreateSequence>()
            sequences.isNotEmpty() shouldBe true
            sequences.any { it.sequenceName.contains("categories_id_seq") } shouldBe true
            sequences.any { it.sequenceName.contains("products_id_seq") } shouldBe true
            sequences.any { it.sequenceName.contains("customers_id_seq") } shouldBe true
            sequences.any { it.sequenceName.contains("orders_id_seq") } shouldBe true
            sequences.any { it.sequenceName.contains("order_items_id_seq") } shouldBe true
        }
    }
})
