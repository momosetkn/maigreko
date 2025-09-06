package momosetkn.maigreko.data

/**
 * Interface for generating database record data from table structures.
 */
interface RecordDataGenerator {
    /**
     * Generates record data for the specified tables.
     *
     * @param tables List of table information to generate data for
     * @param config Configuration for record generation
     * @return Map of table name to generated record data
     */
    fun generateRecords(
        tables: List<TableInfo>,
        config: RecordGenerationConfig = RecordGenerationConfig()
    ): Map<String, RecordData>

    /**
     * Generates record data for a single table.
     *
     * @param table Table information to generate data for
     * @param config Configuration for record generation
     * @return Generated record data for the table
     */
    fun generateRecords(
        table: TableInfo,
        config: RecordGenerationConfig = RecordGenerationConfig()
    ): RecordData
}
