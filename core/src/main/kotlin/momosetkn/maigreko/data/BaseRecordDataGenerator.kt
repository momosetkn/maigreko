package momosetkn.maigreko.data

import java.time.LocalDate
import java.time.LocalDateTime

// import kotlin.random.Random

/**
 * Base implementation of RecordDataGenerator that provides common functionality
 * for generating database record data. Database-specific implementations can extend
 * this class and override methods for dialect-specific behavior.
 */
open class BaseRecordDataGenerator : RecordDataGenerator {
    override fun generateRecords(
        tables: List<TableInfo>,
        config: RecordGenerationConfig
    ): Map<String, RecordData> {
        val result = mutableMapOf<String, RecordData>()

        // Sort tables to handle foreign key dependencies
        val sortedTables = if (config.respectForeignKeys) {
            sortTablesByDependencies(tables)
        } else {
            tables
        }

        for (table in sortedTables) {
            result[table.name] = generateRecords(table, config)
        }

        return result
    }

    override fun generateRecords(
        table: TableInfo,
        config: RecordGenerationConfig
    ): RecordData {
        val records = mutableListOf<Map<String, Any?>>()

        repeat(config.recordsPerTable) { recordIndex ->
            val record = mutableMapOf<String, Any?>()

            for (column in table.columns) {
                val value = generateValueForColumn(column, record, config, recordIndex)
                record[column.name] = value
            }

            records.add(record)
        }

        return RecordData(
            tableName = table.name,
            records = records,
            metadata = mapOf(
                "generatedAt" to System.currentTimeMillis(),
                "recordCount" to records.size,
                "config" to config
            )
        )
    }

    /**
     * Generates a value for a specific column based on its type and constraints.
     */
    protected open fun generateValueForColumn(
        column: ColumnInfo,
        currentRecord: Map<String, Any?>,
        config: RecordGenerationConfig,
        recordIndex: Int,
        setNull: Boolean,
    ): Any? {
        // Check for custom generator first
        val customGenerator = config.customGenerators[column.name]
            ?: config.customGenerators[column.type.lowercase()]

        if (customGenerator != null) {
            return customGenerator.generate(column, currentRecord)
        }

        // Handle nullable columns
        if (column.nullable && setNull) {
            return null
        }

        // Handle auto-increment columns
        if (column.autoIncrement) {
            return recordIndex + 1L
        }

        // Generate value based on column type
        return generateValueByType(column.name, column.type, recordIndex)
    }

    /**
     * Generates a value based on the column's data type.
     */
    protected open fun generateValueByType(columnName: String, type: String, recordIndex: Int): Any? {
        val lowerType = type.lowercase()

        return when {
            // Integer types
            lowerType.contains("int") || lowerType.contains("number") -> {
                when {
                    lowerType.contains("bigint") || lowerType.contains("long") -> random.nextLong(1, 1000000)
                    lowerType.contains("smallint") || lowerType.contains("short") -> random.nextInt(1, 32767)
                    lowerType.contains("tinyint") || lowerType.contains("byte") -> random.nextInt(1, 255)
                    else -> random.nextInt(1, 100000)
                }
            }

            // Decimal/Float types
            lowerType.contains("decimal") || lowerType.contains("numeric") ||
                lowerType.contains("float") || lowerType.contains("double") ||
                lowerType.contains("real") -> {
                (random.nextDouble() * 10000).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)
            }

            // String/Text types
            lowerType.contains("varchar") || lowerType.contains("char") ||
                lowerType.contains("text") || lowerType.contains("string") -> {
                generateStringValue(columnName, lowerType, random, recordIndex)
            }

            // Date/Time types
            lowerType.contains("date") || lowerType.contains("time") || lowerType.contains("timestamp") -> {
                generateDateTimeValue(lowerType, random)
            }

            // Boolean types
            lowerType.contains("bool") || lowerType.contains("bit") -> {
                random.nextBoolean()
            }

            // Binary types
            lowerType.contains("blob") || lowerType.contains("binary") || lowerType.contains("bytea") -> {
                random.nextBytes(random.nextInt(10, 100))
            }

            // UUID types
            lowerType.contains("uuid") || lowerType.contains("guid") -> {
                java.util.UUID.randomUUID().toString()
            }

            // JSON types
            lowerType.contains("json") -> {
                """{"id": ${recordIndex + 1}, "data": "sample_data_${random.nextInt(1000)}"}"""
            }

            // Default fallback
            else -> "generated_value_${recordIndex + 1}"
        }
    }

    /**
     * Generates string values based on column name patterns and constraints.
     */
    protected open fun generateStringValue(columnName: String, type: String, random: Random, recordIndex: Int): String {
        val maxLength = extractMaxLength(type) ?: 50

        return when {
            columnName.contains("email", ignoreCase = true) -> generateEmail(random)
            columnName.contains("name", ignoreCase = true) -> generateName(random)
            columnName.contains("address", ignoreCase = true) -> generateAddress(random)
            columnName.contains("phone", ignoreCase = true) -> generatePhone(random)
            type.contains("email") -> generateEmail(random)
            type.contains("name") -> generateName(random)
            type.contains("address") -> generateAddress(random)
            type.contains("phone") -> generatePhone(random)
            else -> generateRandomString(maxLength, random, recordIndex)
        }
    }

    /**
     * Generates date/time values based on the specific type.
     */
    protected open fun generateDateTimeValue(type: String, random: Random): Any {
        val now = System.currentTimeMillis()
        val randomOffset = random.nextLong(-365 * 24 * 60 * 60 * 1000L, 365 * 24 * 60 * 60 * 1000L)
        val timestamp = now + randomOffset

        return when {
            type.contains("timestamp") -> java.sql.Timestamp(timestamp)
            type.contains("date") -> java.sql.Date(timestamp)
            type.contains("time") -> java.sql.Time(timestamp % (24 * 60 * 60 * 1000))
            else -> java.sql.Timestamp(timestamp)
        }
    }

    /**
     * Sorts tables to respect foreign key dependencies.
     * Tables with no dependencies come first.
     */
    protected open fun sortTablesByDependencies(tables: List<TableInfo>): List<TableInfo> {
        // For now, return tables as-is. In a full implementation, this would
        // analyze foreign key constraints and sort accordingly.
        return tables
    }

    // Utility methods for generating specific types of data

    private fun extractMaxLength(type: String): Int? {
        val regex = """.*\((\d+)\).*""".toRegex()
        return regex.find(type)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun generateRandomString(maxLength: Int, recordIndex: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        if (maxLength <= 10) {
            // For short lengths, generate simple string
            return buildString {
                repeat(maxLength) {
                    append(chars.random(random))
                }
            }
        }

        val length = minOf(maxLength, AutoData.nextInt(5, 20))
        val prefix = "gen_${recordIndex + 1}_"
        val remainingLength = maxOf(0, length - prefix.length)

        return buildString {
            append(prefix)
            repeat(remainingLength) {
                append(chars.random(random))
            }
        }.take(maxLength)
    }
}

object AutoData {
    var currentUuidNum = 0

    inline fun <reified T> nextValue(): T {
        return when (T::class) {
            Int::class -> currentUuidNum++
            Long::class -> (currentUuidNum++).toLong()
            Double::class -> currentUuidNum++ / 10.0
            String::class -> "str" + currentUuidNum++
            LocalDate::class -> LocalDate.now().plusDays(currentUuidNum++.toLong())
            LocalDateTime::class -> LocalDateTime.now().withNano(0).plusHours(currentUuidNum++.toLong())
            else -> throw NotImplementedError("not supported ${T::class} type")
        } as T
    }

    fun nextInt(min: Int = 0, max: Int = Int.MAX_VALUE): Int {
        return nextValue<Int>() % (max - min + 1) + min
    }
}
