package momosetkn.maigreko.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RecordDataGeneratorFactorySpec : FunSpec({

    context("RecordDataGeneratorFactory") {

        test("should create base implementation when no specific builders found") {
            val generator = RecordDataGeneratorFactory.createFirst()
            generator.shouldBeInstanceOf<BaseRecordDataGenerator>()
        }

        test("should generate records from DatabaseData") {
            val databaseData = DatabaseData(
                sourceChanges = emptyList(),
                tables = listOf(
                    TableInfo(
                        name = "test_table",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "INTEGER", autoIncrement = true, nullable = false),
                            ColumnInfo(name = "name", type = "VARCHAR(100)", nullable = false)
                        )
                    )
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 3, seed = 12345)
            val result = RecordDataGeneratorFactory.generateRecords(databaseData, config)

            result.size shouldBe 1
            val tableRecords = result["test_table"]!!
            tableRecords.tableName shouldBe "test_table"
            tableRecords.records.size shouldBe 3

            val firstRecord = tableRecords.records.first()
            firstRecord["id"] shouldBe 1L
            firstRecord["name"] shouldNotBe null
        }

        test("should handle empty DatabaseData") {
            val databaseData = DatabaseData(
                sourceChanges = emptyList(),
                tables = emptyList()
            )

            val result = RecordDataGeneratorFactory.generateRecords(databaseData)
            result.size shouldBe 0
        }

        test("should handle multiple tables in DatabaseData") {
            val databaseData = DatabaseData(
                sourceChanges = emptyList(),
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "INTEGER", autoIncrement = true),
                            ColumnInfo(name = "email", type = "VARCHAR(255)")
                        )
                    ),
                    TableInfo(
                        name = "orders",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "INTEGER", autoIncrement = true),
                            ColumnInfo(name = "total", type = "DECIMAL(10,2)")
                        )
                    )
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 2, seed = 54321)
            val result = RecordDataGeneratorFactory.generateRecords(databaseData, config)

            result.size shouldBe 2
            result["users"]!!.records.size shouldBe 2
            result["orders"]!!.records.size shouldBe 2

            // Verify data types
            val userRecord = result["users"]!!.records.first()
            val orderRecord = result["orders"]!!.records.first()

            userRecord["id"].shouldBeInstanceOf<Long>()
            userRecord["email"].shouldBeInstanceOf<String>()
            orderRecord["id"].shouldBeInstanceOf<Long>()
            orderRecord["total"].shouldBeInstanceOf<java.math.BigDecimal>()
        }
    }
})
