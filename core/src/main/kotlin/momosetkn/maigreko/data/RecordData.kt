package momosetkn.maigreko.data

/**
 * Represents generated database record data.
 * This class contains actual data records that can be inserted into database tables.
 */
data class RecordData(
    /**
     * The table name this record data belongs to
     */
    val tableName: String,

    /**
     * List of records, where each record is a map of column name to value
     */
    val records: List<Map<String, Any?>>,

    /**
     * Metadata about the record generation
     */
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Configuration for generating record data
 */
data class RecordGenerationConfig(
    /**
     * Number of records to generate per table
     */
    val recordsPerTable: Int = 10,

    /**
     * Seed for random data generation (for reproducible results)
     */
    val seed: Long? = null,

    /**
     * Locale for generating localized data (names, addresses, etc.)
     */
    val locale: String = "en_US",

    /**
     * Whether to respect foreign key constraints when generating data
     */
    val respectForeignKeys: Boolean = true,

    /**
     * Custom value generators for specific columns or data types
     */
    val customGenerators: Map<String, ValueGenerator> = emptyMap(),

    /**
     * Additional configuration options
     */
    val options: Map<String, Any> = emptyMap()
)

/**
 * Interface for generating values for specific data types or columns
 */
interface ValueGenerator {
    /**
     * Generates a value for the specified column
     *
     * @param columnInfo Information about the column
     * @param context Generation context (e.g., other column values in the same record)
     * @return Generated value
     */
    fun generate(columnInfo: ColumnInfo, context: Map<String, Any?> = emptyMap()): Any?
}
