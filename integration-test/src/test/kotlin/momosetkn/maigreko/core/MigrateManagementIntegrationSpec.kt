package momosetkn.maigreko.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.PostgresqlColumnDetail
import momosetkn.PostgresqlInfoDao
import momosetkn.maigreko.db.PostgresDataSource
import momosetkn.maigreko.db.PostgresqlDatabase
import momosetkn.maigreko.engine.PostgreMigrateEngine
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcDialects
import javax.sql.DataSource

class MigrateManagementIntegrationSpec : FunSpec({
    lateinit var migrateManagement: MigrateManagement
    lateinit var dataSource: DataSource
    lateinit var db: JdbcDatabase
    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = PostgresDataSource(container)
        db = JdbcDatabase(dataSource = dataSource, dialect = JdbcDialects.get("postgresql"))
        migrateManagement = MigrateManagement(db, PostgreMigrateEngine())
    }
    beforeEach {
        PostgresqlDatabase.clear()
    }
    afterTest {
        println(PostgresqlDatabase.generateDdl())
    }
    context("double forward") {
        test("can migrate") {
            val createTable = CreateTable(
                tableName = "migrations",
                columns = listOf(
                    Column(
                        name = "version",
                        type = "character varying(255)",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    )
                )
            )

            val changeSet = ChangeSet(
                filename = "filename",
                author = "author",
                changeSetId = "changeSetId",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(changeSet)
            migrateManagement.forwardWithManagement(changeSet)
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val columnDetails = postgresqlInfoDao.getColumnDetails("migrations")
            columnDetails.size shouldBe 1
            columnDetails[0] shouldBe PostgresqlColumnDetail(
                columnName = "version",
                type = "character varying(255)",
                notNull = "YES",
                columnDefault = null,
                primaryKey = "YES",
                unique = "NO",
                foreignTable = null,
                foreignColumn = null,
            )
            val constraintDetails = postgresqlInfoDao.getConstraintDetails("migrations")
            constraintDetails.size shouldBe 0
        }
    }
    context("rollback") {
        test("can migrate") {
            val createTable = CreateTable(
                tableName = "migrations",
                columns = listOf(
                    Column(
                        name = "version",
                        type = "character varying(255)",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    )
                )
            )

            val changeSet = ChangeSet(
                filename = "filename",
                author = "author",
                changeSetId = "changeSetId",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(changeSet)
            migrateManagement.rollbackWithManagement(changeSet)
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val columnDetails = postgresqlInfoDao.getColumnDetails("migrations")
            columnDetails.size shouldBe 0
            val constraintDetails = postgresqlInfoDao.getConstraintDetails("migrations")
            constraintDetails.size shouldBe 0
        }
    }

    context("rename table") {
        test("can rename table") {
            // First create a table
            val createTable = CreateTable(
                tableName = "old_table",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    ),
                    Column(
                        name = "name",
                        type = "character varying(100)",
                        columnConstraint = ColumnConstraint(nullable = false)
                    )
                )
            )

            val createTableChangeSet = ChangeSet(
                filename = "create_table",
                author = "author",
                changeSetId = "create_table",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(createTableChangeSet)

            // Then rename the table
            val renameTable = RenameTable(
                oldTableName = "old_table",
                newTableName = "new_table"
            )

            val renameTableChangeSet = ChangeSet(
                filename = "rename_table",
                author = "author",
                changeSetId = "rename_table",
                changes = listOf(renameTable),
            )

            migrateManagement.forwardWithManagement(renameTableChangeSet)

            // Verify table was renamed
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val oldTableColumns = postgresqlInfoDao.getColumnDetails("old_table")
            oldTableColumns.size shouldBe 0

            val newTableColumns = postgresqlInfoDao.getColumnDetails("new_table")
            newTableColumns.size shouldBe 2

            // Test rollback
            migrateManagement.rollbackWithManagement(renameTableChangeSet)
            val columnsAfterRollback = postgresqlInfoDao.getColumnDetails("old_table")
            columnsAfterRollback.size shouldBe 2
            val newTableColumnsAfterRollback = postgresqlInfoDao.getColumnDetails("new_table")
            newTableColumnsAfterRollback.size shouldBe 0
        }
    }

    context("rename column") {
        test("can rename column") {
            // First create a table
            val createTable = CreateTable(
                tableName = "products",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    ),
                    Column(
                        name = "old_name",
                        type = "character varying(100)",
                        columnConstraint = ColumnConstraint(nullable = false)
                    )
                )
            )

            val createTableChangeSet = ChangeSet(
                filename = "create_products_table",
                author = "author",
                changeSetId = "create_products_table",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(createTableChangeSet)

            // Then rename the column
            val renameColumn = RenameColumn(
                tableName = "products",
                oldColumnName = "old_name",
                newColumnName = "new_name"
            )

            val renameColumnChangeSet = ChangeSet(
                filename = "rename_column",
                author = "author",
                changeSetId = "rename_column",
                changes = listOf(renameColumn),
            )

            migrateManagement.forwardWithManagement(renameColumnChangeSet)

            // Verify column was renamed
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val columnDetails = postgresqlInfoDao.getColumnDetails("products")
            columnDetails.size shouldBe 2

            val columnNames = columnDetails.map { it.columnName }
            columnNames.contains("old_name") shouldBe false
            columnNames.contains("new_name") shouldBe true

            // Test rollback
            migrateManagement.rollbackWithManagement(renameColumnChangeSet)
            val columnsAfterRollback = postgresqlInfoDao.getColumnDetails("products")
            columnsAfterRollback.size shouldBe 2

            val columnNamesAfterRollback = columnsAfterRollback.map { it.columnName }
            columnNamesAfterRollback.contains("old_name") shouldBe true
            columnNamesAfterRollback.contains("new_name") shouldBe false
        }
    }

    context("add foreign key") {
        test("can add foreign key") {
            // First create the tables
            val createParentTable = CreateTable(
                tableName = "parent_table",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    ),
                    Column(
                        name = "name",
                        type = "character varying(100)",
                        columnConstraint = ColumnConstraint(nullable = false)
                    )
                )
            )

            val createChildTable = CreateTable(
                tableName = "child_table",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    ),
                    Column(
                        name = "parent_id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(nullable = false)
                    ),
                    Column(
                        name = "description",
                        type = "character varying(200)",
                        columnConstraint = ColumnConstraint(nullable = true)
                    )
                )
            )

            val createTablesChangeSet = ChangeSet(
                filename = "create_tables",
                author = "author",
                changeSetId = "create_tables",
                changes = listOf(createParentTable, createChildTable),
            )

            migrateManagement.forwardWithManagement(createTablesChangeSet)

            // Then add a foreign key
            val addForeignKey = AddForeignKey(
                constraintName = "fk_child_parent",
                tableName = "child_table",
                columnNames = listOf("parent_id"),
                referencedTableName = "parent_table",
                referencedColumnNames = listOf("id"),
                onDelete = ForeignKeyAction.CASCADE,
                onUpdate = ForeignKeyAction.RESTRICT
            )

            val addForeignKeyChangeSet = ChangeSet(
                filename = "add_foreign_key",
                author = "author",
                changeSetId = "add_foreign_key",
                changes = listOf(addForeignKey),
            )

            migrateManagement.forwardWithManagement(addForeignKeyChangeSet)

            // Verify foreign key was added
            val postgresqlInfoDao = PostgresqlInfoDao(db)

            // Check column details
            val columnDetails = postgresqlInfoDao.getColumnDetails("child_table")
            val parentIdColumn = columnDetails.find { it.columnName == "parent_id" }
            parentIdColumn shouldBe PostgresqlColumnDetail(
                columnName = "parent_id",
                type = "integer",
                notNull = "YES",
                columnDefault = null,
                primaryKey = "NO",
                unique = "NO",
                foreignTable = "parent_table",
                foreignColumn = "id",
            )

            // Check constraint details
            val constraintDetails = postgresqlInfoDao.getConstraintDetails("child_table")
            constraintDetails.size shouldBe 1
            constraintDetails[0].constraintName shouldBe "fk_child_parent"
            constraintDetails[0].tableName shouldBe "child_table"
            constraintDetails[0].columnName shouldBe "parent_id"
            constraintDetails[0].foreignTableName shouldBe "parent_table"
            constraintDetails[0].foreignColumnName shouldBe "id"

            // Test rollback
            migrateManagement.rollbackWithManagement(addForeignKeyChangeSet)
            val constraintsAfterRollback = postgresqlInfoDao.getConstraintDetails("child_table")
            constraintsAfterRollback.size shouldBe 0

            // The column should still exist but without foreign key reference
            val columnsAfterRollback = postgresqlInfoDao.getColumnDetails("child_table")
            val parentIdAfterRollback = columnsAfterRollback.find { it.columnName == "parent_id" }
            parentIdAfterRollback?.foreignTable shouldBe null
            parentIdAfterRollback?.foreignColumn shouldBe null
        }
    }

    context("add column") {
        test("can add column") {
            // First create a table
            val createTable = CreateTable(
                tableName = "users",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    )
                )
            )

            val createTableChangeSet = ChangeSet(
                filename = "create_table",
                author = "author",
                changeSetId = "create_table",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(createTableChangeSet)

            // Then add a column
            val addColumn = AddColumn(
                tableName = "users",
                column = Column(
                    name = "username",
                    type = "character varying(100)",
                    columnConstraint = ColumnConstraint(nullable = false)
                )
            )

            val addColumnChangeSet = ChangeSet(
                filename = "add_column",
                author = "author",
                changeSetId = "add_column",
                changes = listOf(addColumn),
            )

            migrateManagement.forwardWithManagement(addColumnChangeSet)

            // Verify column was added
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val columnDetails = postgresqlInfoDao.getColumnDetails("users")
            columnDetails.size shouldBe 2

            val usernameColumn = columnDetails.find { it.columnName == "username" }
            usernameColumn shouldBe PostgresqlColumnDetail(
                columnName = "username",
                type = "character varying(100)",
                notNull = "YES",
                columnDefault = null,
                primaryKey = "NO",
                unique = "NO",
                foreignTable = null,
                foreignColumn = null,
            )

            // Test rollback
            migrateManagement.rollbackWithManagement(addColumnChangeSet)
            val columnsAfterRollback = postgresqlInfoDao.getColumnDetails("users")
            columnsAfterRollback.size shouldBe 1
            columnsAfterRollback[0].columnName shouldBe "id"
        }

        test("should throw exception when using position with PostgreSQL") {
            // First create a table with multiple columns
            val createTable = CreateTable(
                tableName = "employees",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    ),
                    Column(
                        name = "first_name",
                        type = "character varying(50)",
                        columnConstraint = ColumnConstraint(nullable = false)
                    ),
                    Column(
                        name = "last_name",
                        type = "character varying(50)",
                        columnConstraint = ColumnConstraint(nullable = false)
                    )
                )
            )

            val createTableChangeSet = ChangeSet(
                filename = "create_employees_table",
                author = "author",
                changeSetId = "create_employees_table",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(createTableChangeSet)

            // Add a column after first_name - should throw exception
            val addMiddleNameColumn = AddColumn(
                tableName = "employees",
                column = Column(
                    name = "middle_name",
                    type = "character varying(50)",
                    columnConstraint = ColumnConstraint(nullable = true)
                ),
                afterColumn = "first_name"
            )

            val addMiddleNameChangeSet = ChangeSet(
                filename = "add_middle_name",
                author = "author",
                changeSetId = "add_middle_name",
                changes = listOf(addMiddleNameColumn),
            )

            val exception = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                migrateManagement.forwardWithManagement(addMiddleNameChangeSet)
            }

            exception.message shouldBe "PostgreSQL does not support AFTER or BEFORE clauses in ADD COLUMN statements"

            // Add another column before last_name - should also throw exception
            val addPrefixColumn = AddColumn(
                tableName = "employees",
                column = Column(
                    name = "prefix",
                    type = "character varying(10)",
                    columnConstraint = ColumnConstraint(nullable = true)
                ),
                beforeColumn = "last_name"
            )

            val addPrefixChangeSet = ChangeSet(
                filename = "add_prefix",
                author = "author",
                changeSetId = "add_prefix",
                changes = listOf(addPrefixColumn),
            )

            val exception2 = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                migrateManagement.forwardWithManagement(addPrefixChangeSet)
            }

            exception2.message shouldBe "PostgreSQL does not support AFTER or BEFORE clauses in ADD COLUMN statements"

            // Verify only the original columns exist
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val columnDetails = postgresqlInfoDao.getColumnDetails("employees")
            columnDetails.size shouldBe 3
        }
    }

    context("modify data type") {
        test("can modify column data type") {
            // First create a table with a column
            val createTable = CreateTable(
                tableName = "products",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    ),
                    Column(
                        name = "price",
                        type = "integer",
                        columnConstraint = ColumnConstraint(nullable = false)
                    )
                )
            )

            val createTableChangeSet = ChangeSet(
                filename = "create_products_table",
                author = "author",
                changeSetId = "create_products_table",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(createTableChangeSet)

            // Verify the initial column type
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val initialColumnDetails = postgresqlInfoDao.getColumnDetails("products")
            val initialPriceColumn = initialColumnDetails.find { it.columnName == "price" }
            initialPriceColumn?.type shouldBe "integer"

            // Then modify the column data type
            val modifyDataType = ModifyDataType(
                tableName = "products",
                columnName = "price",
                newDataType = "numeric(10,2)",
                oldDataType = "integer"
            )

            val modifyDataTypeChangeSet = ChangeSet(
                filename = "modify_price_type",
                author = "author",
                changeSetId = "modify_price_type",
                changes = listOf(modifyDataType),
            )

            migrateManagement.forwardWithManagement(modifyDataTypeChangeSet)

            // Verify the column type was changed
            val columnDetails = postgresqlInfoDao.getColumnDetails("products")
            val priceColumn = columnDetails.find { it.columnName == "price" }
            priceColumn?.type shouldBe "numeric(10,2)"

            // Test rollback
            migrateManagement.rollbackWithManagement(modifyDataTypeChangeSet)

            // Verify the column type was reverted
            val columnsAfterRollback = postgresqlInfoDao.getColumnDetails("products")
            val priceColumnAfterRollback = columnsAfterRollback.find { it.columnName == "price" }
            priceColumnAfterRollback?.type shouldBe "integer"
        }

        test("can modify column to varchar with different length") {
            // First create a table with a varchar column
            val createTable = CreateTable(
                tableName = "articles",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    ),
                    Column(
                        name = "title",
                        type = "character varying(100)",
                        columnConstraint = ColumnConstraint(nullable = false)
                    )
                )
            )

            val createTableChangeSet = ChangeSet(
                filename = "create_articles_table",
                author = "author",
                changeSetId = "create_articles_table",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(createTableChangeSet)

            // Then modify the column length
            val modifyDataType = ModifyDataType(
                tableName = "articles",
                columnName = "title",
                newDataType = "character varying(255)",
                oldDataType = "character varying(100)"
            )

            val modifyDataTypeChangeSet = ChangeSet(
                filename = "modify_title_length",
                author = "author",
                changeSetId = "modify_title_length",
                changes = listOf(modifyDataType),
            )

            migrateManagement.forwardWithManagement(modifyDataTypeChangeSet)

            // Verify the column type was changed
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val columnDetails = postgresqlInfoDao.getColumnDetails("articles")
            val titleColumn = columnDetails.find { it.columnName == "title" }
            titleColumn?.type shouldBe "character varying(255)"

            // Test rollback
            migrateManagement.rollbackWithManagement(modifyDataTypeChangeSet)

            // Verify the column type was reverted
            val columnsAfterRollback = postgresqlInfoDao.getColumnDetails("articles")
            val titleColumnAfterRollback = columnsAfterRollback.find { it.columnName == "title" }
            titleColumnAfterRollback?.type shouldBe "character varying(100)"
        }
    }

    context("add not null constraint") {
        test("can add not null constraint") {
            // First create a table with a nullable column
            val createTable = CreateTable(
                tableName = "contacts",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    ),
                    Column(
                        name = "email",
                        type = "character varying(255)",
                        columnConstraint = ColumnConstraint(nullable = true)
                    )
                )
            )

            val createTableChangeSet = ChangeSet(
                filename = "create_contacts_table",
                author = "author",
                changeSetId = "create_contacts_table",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(createTableChangeSet)

            // Verify the initial column is nullable
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val initialColumnDetails = postgresqlInfoDao.getColumnDetails("contacts")
            val initialEmailColumn = initialColumnDetails.find { it.columnName == "email" }
            initialEmailColumn?.notNull shouldBe "NO"

            // Then add not null constraint
            val addNotNullConstraint = AddNotNullConstraint(
                tableName = "contacts",
                columnName = "email",
                columnDataType = "character varying(255)"
            )

            val addNotNullConstraintChangeSet = ChangeSet(
                filename = "add_not_null_to_email",
                author = "author",
                changeSetId = "add_not_null_to_email",
                changes = listOf(addNotNullConstraint),
            )

            migrateManagement.forwardWithManagement(addNotNullConstraintChangeSet)

            // Verify the column is now not null
            val columnDetails = postgresqlInfoDao.getColumnDetails("contacts")
            val emailColumn = columnDetails.find { it.columnName == "email" }
            emailColumn?.notNull shouldBe "YES"

            // Test rollback
            migrateManagement.rollbackWithManagement(addNotNullConstraintChangeSet)

            // Verify the column is nullable again
            val columnsAfterRollback = postgresqlInfoDao.getColumnDetails("contacts")
            val emailColumnAfterRollback = columnsAfterRollback.find { it.columnName == "email" }
            emailColumnAfterRollback?.notNull shouldBe "NO"
        }

        test("can add not null constraint with default value") {
            // First create a table with a nullable column
            val createTable = CreateTable(
                tableName = "products",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    ),
                    Column(
                        name = "is_active",
                        type = "boolean",
                        columnConstraint = ColumnConstraint(nullable = true)
                    )
                )
            )

            val createTableChangeSet = ChangeSet(
                filename = "create_products_table",
                author = "author",
                changeSetId = "create_products_table",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(createTableChangeSet)

            // Verify the initial column is nullable and has no default
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val initialColumnDetails = postgresqlInfoDao.getColumnDetails("products")
            val initialIsActiveColumn = initialColumnDetails.find { it.columnName == "is_active" }
            initialIsActiveColumn?.notNull shouldBe "NO"
            initialIsActiveColumn?.columnDefault shouldBe null

            // Then add not null constraint with default
            val addNotNullConstraint = AddNotNullConstraint(
                tableName = "products",
                columnName = "is_active",
                columnDataType = "boolean",
                defaultValue = "true"
            )

            val addNotNullConstraintChangeSet = ChangeSet(
                filename = "add_not_null_to_is_active",
                author = "author",
                changeSetId = "add_not_null_to_is_active",
                changes = listOf(addNotNullConstraint),
            )

            migrateManagement.forwardWithManagement(addNotNullConstraintChangeSet)

            // Verify the column is now not null and has default
            val columnDetails = postgresqlInfoDao.getColumnDetails("products")
            val isActiveColumn = columnDetails.find { it.columnName == "is_active" }
            isActiveColumn?.notNull shouldBe "YES"
            isActiveColumn?.columnDefault shouldBe "true"

            // Test rollback
            migrateManagement.rollbackWithManagement(addNotNullConstraintChangeSet)

            // Verify the column is nullable again but default remains
            // (PostgreSQL doesn't drop default when dropping NOT NULL constraint)
            val columnsAfterRollback = postgresqlInfoDao.getColumnDetails("products")
            val isActiveColumnAfterRollback = columnsAfterRollback.find { it.columnName == "is_active" }
            isActiveColumnAfterRollback?.notNull shouldBe "NO"
        }
    }
})
