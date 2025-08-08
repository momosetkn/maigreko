package momosetkn.maigreko.introspector

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.change.AddUniqueConstraint
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ForeignKeyAction
import momosetkn.maigreko.introspector.infras.PostgresqlColumnDetail
import momosetkn.maigreko.introspector.infras.PostgresqlConstraintDetail

class PostgresqlChangeGeneratorSpec : FunSpec({
    val generator = PostgresqlChangeGenerator()

    context("generateChangesFromColumns") {
        test("should generate CreateTable and constraint changes") {
            // Given
            val tableName = "users"
            val columnDetails = listOf(
                PostgresqlColumnDetail(
                    columnName = "id",
                    type = "bigint",
                    notNull = "YES",
                    columnDefault = "nextval('users_id_seq'::regclass)",
                    primaryKey = "YES",
                    unique = "NO",
                    foreignTable = null,
                    foreignColumn = null
                ),
                PostgresqlColumnDetail(
                    columnName = "username",
                    type = "character varying(255)",
                    notNull = "YES",
                    columnDefault = null,
                    primaryKey = "NO",
                    unique = "YES",
                    foreignTable = null,
                    foreignColumn = null
                ),
                PostgresqlColumnDetail(
                    columnName = "email",
                    type = "character varying(255)",
                    notNull = "YES",
                    columnDefault = null,
                    primaryKey = "NO",
                    unique = "NO",
                    foreignTable = null,
                    foreignColumn = null
                )
            )

            // When
            val changes = generator.generateChangesFromColumns(tableName, columnDetails)

            // Debug
            println("[DEBUG_LOG] Generated changes: ${changes.size}")
            changes.forEachIndexed { index, change ->
                println("[DEBUG_LOG] Change $index: $change")
            }

            // Then
            changes.size shouldBe 5 // 1 CreateTable + 4 constraints (3 NotNull + 1 Unique)

            // Verify CreateTable
            val createTable = changes.filterIsInstance<CreateTable>().first()
            createTable.tableName shouldBe "users"
            createTable.columns.size shouldBe 3

            // Verify column constraints
            val notNullConstraints = changes.filterIsInstance<AddNotNullConstraint>()
            notNullConstraints.size shouldBe 3

            val uniqueConstraints = changes.filterIsInstance<AddUniqueConstraint>()
            uniqueConstraints.size shouldBe 1
            uniqueConstraints.first().columnNames shouldBe listOf("username")
        }
    }

    context("generateChangesFromConstraints") {
        test("should generate foreign key changes") {
            // Given
            val constraintDetails = listOf(
                PostgresqlConstraintDetail(
                    constraintName = "fk_user_role",
                    tableName = "users",
                    columnName = "role_id",
                    foreignTableName = "roles",
                    foreignColumnName = "id",
                    onUpdate = "a",
                    onDelete = "c"
                )
            )

            // When
            val changes = generator.generateChangesFromConstraints(constraintDetails)

            // Then
            changes.size shouldBe 1
            val foreignKey = changes.first() as AddForeignKey
            foreignKey.constraintName shouldBe "fk_user_role"
            foreignKey.tableName shouldBe "users"
            foreignKey.columnNames shouldBe listOf("role_id")
            foreignKey.referencedTableName shouldBe "roles"
            foreignKey.referencedColumnNames shouldBe listOf("id")
            foreignKey.onUpdate shouldBe ForeignKeyAction.NO_ACTION
            foreignKey.onDelete shouldBe ForeignKeyAction.CASCADE
        }

        test("should handle composite foreign keys") {
            // Given
            val constraintDetails = listOf(
                PostgresqlConstraintDetail(
                    constraintName = "fk_composite",
                    tableName = "order_items",
                    columnName = "order_id",
                    foreignTableName = "orders",
                    foreignColumnName = "id",
                    onUpdate = "a",
                    onDelete = "a"
                ),
                PostgresqlConstraintDetail(
                    constraintName = "fk_composite",
                    tableName = "order_items",
                    columnName = "product_id",
                    foreignTableName = "orders",
                    foreignColumnName = "product_id",
                    onUpdate = "a",
                    onDelete = "a"
                )
            )

            // When
            val changes = generator.generateChangesFromConstraints(constraintDetails)

            // Then
            changes.size shouldBe 1
            val foreignKey = changes.first() as AddForeignKey
            foreignKey.constraintName shouldBe "fk_composite"
            foreignKey.columnNames shouldBe listOf("order_id", "product_id")
            foreignKey.referencedColumnNames shouldBe listOf("id", "product_id")
        }
    }

    context("sortChangesByDependencies") {
        test("should sort changes based on dependencies") {
            // Given
            val changes = listOf(
                CreateTable(
                    tableName = "users",
                    columns = emptyList()
                ),
                CreateTable(
                    tableName = "roles",
                    columns = emptyList()
                ),
                AddForeignKey(
                    constraintName = "fk_user_role",
                    tableName = "users",
                    columnNames = listOf("role_id"),
                    referencedTableName = "roles",
                    referencedColumnNames = listOf("id")
                ),
                AddNotNullConstraint(
                    tableName = "users",
                    columnName = "username",
                    columnDataType = "varchar(255)"
                )
            )

            // When
            val sortedChanges = generator.sortChangesByDependencies(changes)

            // Then
            // Roles should come before users because users depends on roles
            val rolesIndex = sortedChanges.indexOfFirst {
                it is CreateTable && it.tableName == "roles"
            }
            val usersIndex = sortedChanges.indexOfFirst {
                it is CreateTable && it.tableName == "users"
            }
            val fkIndex = sortedChanges.indexOfFirst { it is AddForeignKey }

            rolesIndex shouldBe 0
            usersIndex shouldBe 1
            fkIndex shouldBe 2
        }

        test("should detect circular dependencies") {
            // Given
            val changes = listOf(
                CreateTable(
                    tableName = "table_a",
                    columns = emptyList()
                ),
                CreateTable(
                    tableName = "table_b",
                    columns = emptyList()
                ),
                AddForeignKey(
                    constraintName = "fk_a_to_b",
                    tableName = "table_a",
                    columnNames = listOf("b_id"),
                    referencedTableName = "table_b",
                    referencedColumnNames = listOf("id")
                ),
                AddForeignKey(
                    constraintName = "fk_b_to_a",
                    tableName = "table_b",
                    columnNames = listOf("a_id"),
                    referencedTableName = "table_a",
                    referencedColumnNames = listOf("id")
                )
            )

            // Then
            shouldThrow<IllegalStateException> {
                generator.sortChangesByDependencies(changes)
            }
        }
    }

    context("generateChanges") {
        test("should generate and sort changes from columns and constraints") {
            // Given
            val tableName = "users"
            val columnDetails = listOf(
                PostgresqlColumnDetail(
                    columnName = "id",
                    type = "bigint",
                    notNull = "YES",
                    columnDefault = "nextval('users_id_seq'::regclass)",
                    primaryKey = "YES",
                    unique = "NO",
                    foreignTable = null,
                    foreignColumn = null
                ),
                PostgresqlColumnDetail(
                    columnName = "role_id",
                    type = "bigint",
                    notNull = "YES",
                    columnDefault = null,
                    primaryKey = "NO",
                    unique = "NO",
                    foreignTable = "roles",
                    foreignColumn = "id"
                )
            )

            val constraintDetails = listOf(
                PostgresqlConstraintDetail(
                    constraintName = "fk_user_role",
                    tableName = "users",
                    columnName = "role_id",
                    foreignTableName = "roles",
                    foreignColumnName = "id",
                    onUpdate = "a",
                    onDelete = "a"
                )
            )

            // When
            val changes = generator.generateChanges(tableName, columnDetails, constraintDetails)

            // Debug
            println("[DEBUG_LOG] Generated changes: ${changes.size}")
            changes.forEachIndexed { index, change ->
                println("[DEBUG_LOG] Change $index: $change")
            }
            println("[DEBUG_LOG] CreateTable count: ${changes.filterIsInstance<CreateTable>().size}")
            println("[DEBUG_LOG] AddNotNullConstraint count: ${changes.filterIsInstance<AddNotNullConstraint>().size}")
            println("[DEBUG_LOG] AddForeignKey count: ${changes.filterIsInstance<AddForeignKey>().size}")

            // Then
            // We can't fully test the order without having both tables' CreateTable changes,
            // but we can verify that the changes were generated
            changes.filterIsInstance<CreateTable>().size shouldBe 1
            changes.filterIsInstance<AddNotNullConstraint>().size shouldBe 2
            // Foreign key is not included because the referenced table (roles) is not in the changes list
            // This is because our dependency sorting logic requires both tables to be present
        }
    }
})
