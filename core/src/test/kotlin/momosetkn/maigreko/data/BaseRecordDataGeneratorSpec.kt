package momosetkn.maigreko.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.random.Random

class BaseRecordDataGeneratorSpec : FunSpec({

    context("BaseRecordDataGenerator") {
        val generator = BaseRecordDataGenerator()

        test("should generate records for a simple table") {
            val table = TableInfo(
                name = "users",
                columns = listOf(
                    ColumnInfo(name = "id", type = "INTEGER", autoIncrement = true, nullable = false),
                    ColumnInfo(name = "name", type = "VARCHAR(100)", nullable = false),
                    ColumnInfo(name = "email", type = "VARCHAR(255)", nullable = false)
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 5, seed = 12345)
            val result = generator.generateRecords(table, config)

            result.tableName shouldBe "users"
            result.records shouldHaveSize 5

            val firstRecord = result.records.first()
            firstRecord.keys shouldHaveSize 3
            firstRecord["id"] shouldBe 1L
            firstRecord["name"] shouldNotBe null
            firstRecord["email"] shouldNotBe null
        }

        test("should handle nullable columns") {
            val table = TableInfo(
                name = "products",
                columns = listOf(
                    ColumnInfo(name = "id", type = "INTEGER", autoIncrement = true, nullable = false),
                    ColumnInfo(name = "description", type = "TEXT", nullable = true)
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 50, seed = 12345)
            val result = generator.generateRecords(table, config)

            result.records shouldHaveSize 50

            // Check that some records have null values for nullable columns
            val nullDescriptions = result.records.count { it["description"] == null }
            nullDescriptions shouldNotBe 0 // Should have some null values
        }

        test("should handle default values") {
            val table = TableInfo(
                name = "settings",
                columns = listOf(
                    ColumnInfo(name = "id", type = "INTEGER", autoIncrement = true, nullable = false),
                    ColumnInfo(name = "active", type = "BOOLEAN", defaultValue = true, nullable = false)
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 10, seed = 12345)
            val result = generator.generateRecords(table, config)

            result.records shouldHaveSize 10

            // Some records should use the default value
            val defaultValues = result.records.count { it["active"] == true }
            defaultValues shouldNotBe 0 // Should have some default values
        }

        test("should generate appropriate values for different data types") {
            val table = TableInfo(
                name = "test_types",
                columns = listOf(
                    ColumnInfo(name = "int_col", type = "INTEGER", nullable = false),
                    ColumnInfo(name = "bigint_col", type = "BIGINT", nullable = false),
                    ColumnInfo(name = "varchar_col", type = "VARCHAR(50)", nullable = false),
                    ColumnInfo(name = "decimal_col", type = "DECIMAL(10,2)", nullable = false),
                    ColumnInfo(name = "date_col", type = "DATE", nullable = false),
                    ColumnInfo(name = "timestamp_col", type = "TIMESTAMP", nullable = false),
                    ColumnInfo(name = "bool_col", type = "BOOLEAN", nullable = false),
                    ColumnInfo(name = "uuid_col", type = "UUID", nullable = false)
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 1, seed = 12345)
            val result = generator.generateRecords(table, config)

            result.records shouldHaveSize 1
            val record = result.records.first()

            record["int_col"].shouldBeInstanceOf<Int>()
            record["bigint_col"].shouldBeInstanceOf<Long>()
            record["varchar_col"].shouldBeInstanceOf<String>()
            record["decimal_col"].shouldBeInstanceOf<java.math.BigDecimal>()
            record["date_col"].shouldBeInstanceOf<java.sql.Date>()
            record["timestamp_col"].shouldBeInstanceOf<java.sql.Timestamp>()
            record["bool_col"].shouldBeInstanceOf<Boolean>()
            record["uuid_col"].shouldBeInstanceOf<String>()
        }

        test("should use custom value generators when provided") {
            val table = TableInfo(
                name = "custom_test",
                columns = listOf(
                    ColumnInfo(name = "id", type = "INTEGER"),
                    ColumnInfo(name = "custom_field", type = "VARCHAR(100)")
                )
            )

            val customGenerator = object : ValueGenerator {
                override fun generate(columnInfo: ColumnInfo, context: Map<String, Any?>): Any? {
                    return "custom_value_${context.size}"
                }
            }

            val config = RecordGenerationConfig(
                recordsPerTable = 3,
                customGenerators = mapOf("custom_field" to customGenerator)
            )

            val result = generator.generateRecords(table, config)

            result.records shouldHaveSize 3
            result.records.forEach { record ->
                val customValue = record["custom_field"] as String
                customValue shouldBe "custom_value_1" // context size is always 1 (id field)
            }
        }

        test("should generate realistic email addresses for email fields") {
            val table = TableInfo(
                name = "users",
                columns = listOf(
                    ColumnInfo(name = "email", type = "VARCHAR(255)", nullable = false)
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 5, seed = 12345)
            val result = generator.generateRecords(table, config)

            result.records shouldHaveSize 5
            result.records.forEach { record ->
                val email = record["email"]
                email shouldNotBe null
                if (email is String) {
                    email.contains("@") shouldBe true
                    email.contains(".") shouldBe true
                }
            }
        }

        test("should handle multiple tables with relationships") {
            val userTable = TableInfo(
                name = "users",
                columns = listOf(
                    ColumnInfo(name = "id", type = "INTEGER", autoIncrement = true, nullable = false),
                    ColumnInfo(name = "name", type = "VARCHAR(100)", nullable = false)
                )
            )

            val orderTable = TableInfo(
                name = "orders",
                columns = listOf(
                    ColumnInfo(name = "id", type = "INTEGER", autoIncrement = true, nullable = false),
                    ColumnInfo(name = "user_id", type = "INTEGER", nullable = false),
                    ColumnInfo(name = "amount", type = "DECIMAL(10,2)", nullable = false)
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 3, seed = 12345)
            val result = generator.generateRecords(listOf(userTable, orderTable), config)

            result.size shouldBe 2
            result["users"]!!.records shouldHaveSize 3
            result["orders"]!!.records shouldHaveSize 3
        }

        test("should respect string length constraints") {
            val table = TableInfo(
                name = "short_strings",
                columns = listOf(
                    ColumnInfo(name = "short_field", type = "VARCHAR(10)")
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 5, seed = 12345)
            val result = generator.generateRecords(table, config)

            result.records shouldHaveSize 5
            result.records.forEach { record ->
                val shortValue = record["short_field"] as String
                shortValue.length shouldBe 10 // Should respect max length
            }
        }

        test("should include metadata in generated record data") {
            val table = TableInfo(
                name = "test_table",
                columns = listOf(
                    ColumnInfo(name = "id", type = "INTEGER", autoIncrement = true)
                )
            )

            val config = RecordGenerationConfig(recordsPerTable = 5)
            val result = generator.generateRecords(table, config)

            result.metadata["recordCount"] shouldBe 5
            result.metadata["config"] shouldBe config
            result.metadata.containsKey("generatedAt") shouldBe true
        }
    }
})
