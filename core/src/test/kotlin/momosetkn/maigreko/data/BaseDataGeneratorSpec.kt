package momosetkn.maigreko.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import momosetkn.maigreko.change.AddColumn
import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.AddIndex
import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.change.AddUniqueConstraint
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ForeignKeyAction
import momosetkn.maigreko.introspector.Introspector

class BaseDataGeneratorSpec : FunSpec({

    context("BaseDataGenerator") {
        val mockIntrospector = object : Introspector {
            override fun introspect(): List<Change> = emptyList()
        }

        val generator = BaseDataGenerator(mockIntrospector)

        test("should generate empty DatabaseData for empty change list") {
            val changes = emptyList<Change>()
            val result = generator.generateData(changes)

            result.sourceChanges shouldBe changes
            result.tables shouldHaveSize 0
            result.indexes shouldHaveSize 0
            result.constraints shouldHaveSize 0
            result.sequences shouldHaveSize 0
            result.statements shouldHaveSize 0
            result.metadata["totalChanges"] shouldBe 0
        }

        test("should extract table information from CreateTable change") {
            val columns = listOf(
                Column.build(
                    name = "id",
                    type = "INTEGER",
                    columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                ),
                Column.build(
                    name = "name",
                    type = "VARCHAR(100)",
                    columnConstraint = ColumnConstraint(nullable = false)
                )
            )

            val changes = listOf(
                CreateTable(
                    tableName = "users",
                    columns = columns
                )
            )

            val result = generator.generateData(changes)

            result.tables shouldHaveSize 1
            val table = result.tables.first()
            table.name shouldBe "users"
            table.columns shouldHaveSize 2

            val idColumn = table.columns.find { it.name == "id" }!!
            idColumn.type shouldBe "INTEGER"
            idColumn.nullable shouldBe false

            val nameColumn = table.columns.find { it.name == "name" }!!
            nameColumn.type shouldBe "VARCHAR(100)"
            nameColumn.nullable shouldBe false
        }

        test("should extract primary key constraints from CreateTable") {
            val columns = listOf(
                Column.build(
                    name = "id",
                    type = "INTEGER",
                    columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                )
            )

            val changes = listOf(
                CreateTable(
                    tableName = "users",
                    columns = columns
                )
            )

            val result = generator.generateData(changes)

            result.constraints shouldHaveSize 1
            val constraint = result.constraints.first()
            constraint.name shouldBe "users_id_pk"
            constraint.type shouldBe ConstraintType.PRIMARY_KEY
            constraint.tableName shouldBe "users"
            constraint.columns shouldBe listOf("id")
        }

        test("should extract index information from AddIndex change") {
            val changes = listOf(
                AddIndex(
                    indexName = "idx_user_email",
                    tableName = "users",
                    columnNames = listOf("email"),
                    unique = true
                )
            )

            val result = generator.generateData(changes)

            result.indexes shouldHaveSize 1
            val index = result.indexes.first()
            index.name shouldBe "idx_user_email"
            index.tableName shouldBe "users"
            index.columns shouldBe listOf("email")
            index.unique shouldBe true
        }

        test("should extract foreign key constraints from AddForeignKey change") {
            val changes = listOf(
                AddForeignKey(
                    constraintName = "fk_user_department",
                    tableName = "users",
                    columnNames = listOf("department_id"),
                    referencedTableName = "departments",
                    referencedColumnNames = listOf("id"),
                    onDelete = ForeignKeyAction.CASCADE,
                    onUpdate = ForeignKeyAction.RESTRICT
                )
            )

            val result = generator.generateData(changes)

            result.constraints shouldHaveSize 1
            val constraint = result.constraints.first()
            constraint.name shouldBe "fk_user_department"
            constraint.type shouldBe ConstraintType.FOREIGN_KEY
            constraint.tableName shouldBe "users"
            constraint.columns shouldBe listOf("department_id")
            constraint.referencedTable shouldBe "departments"
            constraint.referencedColumns shouldBe listOf("id")

            val metadata = constraint.metadata
            metadata["onDelete"] shouldBe ForeignKeyAction.CASCADE
            metadata["onUpdate"] shouldBe ForeignKeyAction.RESTRICT
        }

        test("should extract unique constraints from AddUniqueConstraint change") {
            val changes = listOf(
                AddUniqueConstraint(
                    constraintName = "uk_user_email",
                    tableName = "users",
                    columnNames = listOf("email")
                )
            )

            val result = generator.generateData(changes)

            result.constraints shouldHaveSize 1
            val constraint = result.constraints.first()
            constraint.name shouldBe "uk_user_email"
            constraint.type shouldBe ConstraintType.UNIQUE
            constraint.tableName shouldBe "users"
            constraint.columns shouldBe listOf("email")
        }

        test("should extract not null constraints from AddNotNullConstraint change") {
            val changes = listOf(
                AddNotNullConstraint(
                    tableName = "users",
                    columnName = "name",
                    columnDataType = "VARCHAR(100)",
                    defaultValue = "Unknown"
                )
            )

            val result = generator.generateData(changes)

            result.constraints shouldHaveSize 1
            val constraint = result.constraints.first()
            constraint.name shouldBe "users_name_not_null"
            constraint.type shouldBe ConstraintType.NOT_NULL
            constraint.tableName shouldBe "users"
            constraint.columns shouldBe listOf("name")

            val metadata = constraint.metadata
            metadata["columnDataType"] shouldBe "VARCHAR(100)"
            metadata["defaultValue"] shouldBe "Unknown"
        }

        test("should extract sequence information from CreateSequence change") {
            val changes = listOf(
                CreateSequence(
                    sequenceName = "user_id_seq",
                    dataType = "BIGINT",
                    startValue = 1,
                    incrementBy = 1,
                    minValue = 1,
                    maxValue = Long.MAX_VALUE,
                    cycle = false,
                    cacheSize = 50
                )
            )

            val result = generator.generateData(changes)

            result.sequences shouldHaveSize 1
            val sequence = result.sequences.first()
            sequence.name shouldBe "user_id_seq"
            sequence.dataType shouldBe "BIGINT"
            sequence.startValue shouldBe 1
            sequence.incrementBy shouldBe 1
            sequence.minValue shouldBe 1
            sequence.maxValue shouldBe Long.MAX_VALUE
            sequence.cycle shouldBe false

            val metadata = sequence.metadata
            metadata["cacheSize"] shouldBe 50L
        }

        test("should handle AddColumn changes for existing tables") {
            val changes = listOf(
                CreateTable(
                    tableName = "users",
                    columns = listOf(
                        Column.build(name = "id", type = "INTEGER")
                    )
                ),
                AddColumn(
                    tableName = "users",
                    column = Column.build(
                        name = "email",
                        type = "VARCHAR(255)",
                        columnConstraint = ColumnConstraint(nullable = false)
                    )
                )
            )

            val result = generator.generateData(changes)

            result.tables shouldHaveSize 1
            val table = result.tables.first()
            table.name shouldBe "users"
            table.columns shouldHaveSize 2

            val columns = table.columns.map { it.name }
            columns shouldContain "id"
            columns shouldContain "email"
        }

        test("should generate metadata with change statistics") {
            val changes = listOf(
                CreateTable(tableName = "users", columns = emptyList()),
                CreateTable(tableName = "departments", columns = emptyList()),
                AddIndex(indexName = "idx_test", tableName = "users", columnNames = listOf("id")),
                CreateSequence(sequenceName = "test_seq")
            )

            val result = generator.generateData(changes)

            result.metadata["totalChanges"] shouldBe 4
            val changeTypeCounts = result.metadata["changeTypeCounts"] as Map<*, *>
            changeTypeCounts["CreateTable"] shouldBe 2
            changeTypeCounts["AddIndex"] shouldBe 1
            changeTypeCounts["CreateSequence"] shouldBe 1
            result.metadata["generatedAt"].shouldBeInstanceOf<Long>()
        }

        test("should build column metadata from Column properties") {
            val column = Column.build(
                name = "test_column",
                type = "INTEGER",
                autoIncrement = true,
                identityGeneration = "by default",
                startValue = 10,
                incrementBy = 2,
                cycle = true
            )

            val changes = listOf(
                CreateTable(
                    tableName = "test_table",
                    columns = listOf(column)
                )
            )

            val result = generator.generateData(changes)

            val table = result.tables.first()
            val columnInfo = table.columns.first()
            val metadata = columnInfo.metadata

            metadata["identityGeneration"] shouldBe Column.IdentityGeneration.BY_DEFAULT
            metadata["startValue"] shouldBe 10L
            metadata["incrementBy"] shouldBe 2L
            metadata["cycle"] shouldBe true
        }
    }
})
