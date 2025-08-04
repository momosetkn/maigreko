package momosetkn.maigreko.engine

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.maigreko.core.AddColumn
import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.Column
import momosetkn.maigreko.core.ColumnConstraint
import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.core.ForeignKeyAction
import momosetkn.maigreko.core.RenameColumn
import momosetkn.maigreko.core.RenameTable

class PosgresqlDdlGeneratorSpec : FunSpec({
    val subject = PosgresqlDdlGenerator()

    context("createTable") {
        context("primaryKey = true, nullable = false") {
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
                val ddl = subject.createTable(createTable)

                ddl shouldBe """
                    create table migrations (
                    version character varying(255) primary key
                    )
                """.trimIndent()
            }
        }

        context("multiple columns with different constraints") {
            test("can migrate") {
                val createTable = CreateTable(
                    tableName = "users",
                    columns = listOf(
                        Column(
                            name = "id",
                            type = "integer",
                            autoIncrement = true,
                            columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                        ),
                        Column(
                            name = "username",
                            type = "character varying(100)",
                            columnConstraint = ColumnConstraint(nullable = false, unique = true)
                        ),
                        Column(
                            name = "email",
                            type = "character varying(255)",
                            columnConstraint = ColumnConstraint(nullable = true)
                        )
                    )
                )
                val ddl = subject.createTable(createTable)

                ddl shouldBe """
                    create table users (
                    id integer generated always as identity primary key,
                    username character varying(100) unique not null,
                    email character varying(255)
                    )
                """.trimIndent()
            }
        }
    }

    context("addForeignKey") {
        test("basic foreign key") {
            val addForeignKey = AddForeignKey(
                constraintName = "fk_orders_users",
                tableName = "orders",
                columnNames = listOf("user_id"),
                referencedTableName = "users",
                referencedColumnNames = listOf("id")
            )

            val ddl = subject.addForeignKey(addForeignKey)

            ddl shouldBe """
                alter table orders
                add constraint fk_orders_users
                foreign key (user_id)
                references users (id)
            """.trimIndent()
        }

        test("foreign key with ON DELETE CASCADE") {
            val addForeignKey = AddForeignKey(
                constraintName = "fk_order_items_orders",
                tableName = "order_items",
                columnNames = listOf("order_id"),
                referencedTableName = "orders",
                referencedColumnNames = listOf("id"),
                onDelete = ForeignKeyAction.CASCADE
            )

            val ddl = subject.addForeignKey(addForeignKey)

            ddl shouldBe """
                alter table order_items
                add constraint fk_order_items_orders
                foreign key (order_id)
                references orders (id)
                on delete cascade
            """.trimIndent()
        }

        test("foreign key with multiple actions and deferrable") {
            val addForeignKey = AddForeignKey(
                constraintName = "fk_complex",
                tableName = "child",
                columnNames = listOf("parent_id"),
                referencedTableName = "parent",
                referencedColumnNames = listOf("id"),
                onDelete = ForeignKeyAction.SET_NULL,
                onUpdate = ForeignKeyAction.RESTRICT,
                deferrable = true,
                initiallyDeferred = true
            )

            val ddl = subject.addForeignKey(addForeignKey)

            ddl shouldBe """
                alter table child
                add constraint fk_complex
                foreign key (parent_id)
                references parent (id)
                on delete set null on update restrict deferrable initially deferred
            """.trimIndent()
        }

        test("composite foreign key") {
            val addForeignKey = AddForeignKey(
                constraintName = "fk_composite",
                tableName = "order_items",
                columnNames = listOf("product_id", "variant_id"),
                referencedTableName = "products",
                referencedColumnNames = listOf("id", "variant_id")
            )

            val ddl = subject.addForeignKey(addForeignKey)

            ddl shouldBe """
                alter table order_items
                add constraint fk_composite
                foreign key (product_id, variant_id)
                references products (id, variant_id)
            """.trimIndent()
        }
    }

    context("addColumn") {
        test("simple column") {
            val addColumn = AddColumn(
                tableName = "users",
                column = Column(
                    name = "address",
                    type = "character varying(255)"
                )
            )

            val ddl = subject.addColumn(addColumn)

            ddl shouldBe """
                alter table users
                add column address character varying(255)
            """.trimIndent()
        }

        test("not null column with default") {
            val addColumn = AddColumn(
                tableName = "products",
                column = Column(
                    name = "is_active",
                    type = "boolean",
                    defaultValue = "true",
                    columnConstraint = ColumnConstraint(nullable = false)
                )
            )

            val ddl = subject.addColumn(addColumn)

            ddl shouldBe """
                alter table products
                add column is_active boolean default true not null
            """.trimIndent()
        }

        test("column with position after - should throw exception") {
            val addColumn = AddColumn(
                tableName = "employees",
                column = Column(
                    name = "middle_name",
                    type = "character varying(100)"
                ),
                afterColumn = "first_name"
            )

            val exception = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                subject.addColumn(addColumn)
            }

            exception.message shouldBe "PostgreSQL does not support AFTER or BEFORE clauses in ADD COLUMN statements"
        }

        test("column with position before - should throw exception") {
            val addColumn = AddColumn(
                tableName = "employees",
                column = Column(
                    name = "prefix",
                    type = "character varying(10)"
                ),
                beforeColumn = "last_name"
            )

            val exception = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                subject.addColumn(addColumn)
            }

            exception.message shouldBe "PostgreSQL does not support AFTER or BEFORE clauses in ADD COLUMN statements"
        }
    }

    context("renameTable") {
        test("rename table") {
            val renameTable = RenameTable(
                oldTableName = "old_table",
                newTableName = "new_table"
            )

            val ddl = subject.renameTable(renameTable)

            ddl shouldBe """
                alter table old_table
                rename to new_table
            """.trimIndent()
        }
    }

    context("renameColumn") {
        test("rename column") {
            val renameColumn = RenameColumn(
                tableName = "users",
                oldColumnName = "username",
                newColumnName = "login"
            )

            val ddl = subject.renameColumn(renameColumn)

            ddl shouldBe """
                alter table users
                rename column username to login
            """.trimIndent()
        }
    }

    context("dropTable") {
        test("drop simple table") {
            val createTable = CreateTable(
                tableName = "temporary_logs",
                columns = listOf(
                    Column(
                        name = "id",
                        type = "integer",
                        columnConstraint = ColumnConstraint(primaryKey = true)
                    ),
                    Column(
                        name = "message",
                        type = "text"
                    )
                )
            )

            val ddl = subject.dropTable(createTable)

            ddl shouldBe """
                drop table temporary_logs
            """.trimIndent()
        }
    }

    context("dropForeignKey") {
        test("drop foreign key constraint") {
            val addForeignKey = AddForeignKey(
                constraintName = "fk_orders_users",
                tableName = "orders",
                columnNames = listOf("user_id"),
                referencedTableName = "users",
                referencedColumnNames = listOf("id")
            )

            val ddl = subject.dropForeignKey(addForeignKey)

            ddl shouldBe """
                alter table orders
                drop constraint fk_orders_users
            """.trimIndent()
        }
    }

    context("dropColumn") {
        test("drop simple column") {
            val addColumn = AddColumn(
                tableName = "users",
                column = Column(
                    name = "address",
                    type = "character varying(255)"
                )
            )

            val ddl = subject.dropColumn(addColumn)

            ddl shouldBe """
                alter table users
                drop column address
            """.trimIndent()
        }
    }
})
