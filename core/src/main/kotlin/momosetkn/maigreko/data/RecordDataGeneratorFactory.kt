package momosetkn.maigreko.data

import momosetkn.maigreko.introspector.Introspector
import java.sql.DatabaseMetaData
import java.sql.SQLException
import java.util.ServiceLoader
import javax.sql.DataSource

/**
 * Factory for creating RecordDataGenerator instances.
 * Uses Java's ServiceLoader mechanism to discover RecordDataGenerator implementations
 * and automatically detects the appropriate dialect from database metadata.
 */
object RecordDataGeneratorFactory {
    /**
     * Creates a RecordDataGenerator instance based on database metadata.
     * Automatically detects the appropriate dialect from the database connection.
     *
     * @param dataSource The DataSource to use for database connections
     * @return A RecordDataGenerator instance for the detected dialect
     * @throws IllegalArgumentException if no RecordDataGenerator is found for the detected dialect
     * @throws SQLException if there is an error accessing the database metadata
     */
    fun create(dataSource: DataSource): RecordDataGenerator {
        dataSource.connection.use { connection ->
            val metadata = connection.metaData
            val dialect = getDialectByMetaData(metadata)

            return dialect?.let { create(it, dataSource) } ?: createFirst(dataSource)
        }
    }

    /**
     * Creates a RecordDataGenerator instance for the specified dialect.
     *
     * @param dialect The database dialect to create a RecordDataGenerator for
     * @param dataSource The DataSource to use for database connections
     * @return A RecordDataGenerator instance for the specified dialect
     * @throws IllegalArgumentException if no RecordDataGenerator is found for the specified dialect
     */
    fun create(dialect: String, dataSource: DataSource): RecordDataGenerator {
        // Try to find a builder for the dialect
        val builderLoader = ServiceLoader.load(RecordDataGeneratorBuilder::class.java)
        val builders = builderLoader.iterator()

        while (builders.hasNext()) {
            val builder = builders.next()
            if (builder.name.startsWith(dialect.lowercase())) {
                return builder.build(dataSource = dataSource)
            }
        }

        // If no dialect-specific builder found, return base implementation
        return BaseRecordDataGenerator()
    }

    /**
     * Creates a RecordDataGenerator instance using an existing Introspector.
     * Attempts to detect the dialect from the DataSource if provided.
     *
     * @param introspector The Introspector instance to use
     * @param dataSource The DataSource for database connections (optional)
     * @return A RecordDataGenerator instance
     * @throws IllegalArgumentException if no RecordDataGenerator implementation is found
     */
    fun create(introspector: Introspector, dataSource: DataSource? = null): RecordDataGenerator {
        if (dataSource != null) {
            // Try to detect dialect and create specific RecordDataGenerator
            try {
                dataSource.connection.use { connection ->
                    val metadata = connection.metaData
                    val dialect = getDialectByMetaData(metadata)
                    if (dialect != null) {
                        return create(dialect, introspector, dataSource)
                    }
                }
            } catch (e: SQLException) {
                // Fall back to generic approach if metadata access fails
            }
        }

        // Fall back to first available RecordDataGenerator
        return createFirst(introspector, dataSource)
    }

    /**
     * Creates a RecordDataGenerator instance for the specified dialect using an existing Introspector.
     *
     * @param dialect The database dialect
     * @param introspector The Introspector instance to use
     * @param dataSource The DataSource for database connections (optional)
     * @return A RecordDataGenerator instance for the specified dialect
     * @throws IllegalArgumentException if no RecordDataGenerator is found for the specified dialect
     */
    fun create(dialect: String, introspector: Introspector, dataSource: DataSource? = null): RecordDataGenerator {
        val builderLoader = ServiceLoader.load(RecordDataGeneratorBuilder::class.java)
        val builders = builderLoader.iterator()

        while (builders.hasNext()) {
            val builder = builders.next()
            if (builder.name.startsWith(dialect.lowercase())) {
                return builder.build(introspector, dataSource)
            }
        }

        // If no dialect-specific builder found, return base implementation
        return BaseRecordDataGenerator()
    }

    /**
     * Creates a RecordDataGenerator instance by loading the first available implementation
     * from ServiceLoader, or returns the base implementation if none found.
     *
     * @param dataSource The DataSource to use for database connections
     * @return A RecordDataGenerator instance
     */
    fun createFirst(dataSource: DataSource): RecordDataGenerator {
        val builderLoader = ServiceLoader.load(RecordDataGeneratorBuilder::class.java)
        val builders = builderLoader.iterator()

        if (builders.hasNext()) {
            val builder = builders.next()
            return builder.build(dataSource = dataSource)
        }

        // Fall back to base implementation
        return BaseRecordDataGenerator()
    }

    /**
     * Creates a RecordDataGenerator instance by loading the first available implementation
     * from ServiceLoader using an existing Introspector.
     *
     * @param introspector The Introspector instance to use
     * @param dataSource The DataSource for database connections (optional)
     * @return A RecordDataGenerator instance
     */
    fun createFirst(introspector: Introspector? = null, dataSource: DataSource? = null): RecordDataGenerator {
        val builderLoader = ServiceLoader.load(RecordDataGeneratorBuilder::class.java)
        val builders = builderLoader.iterator()

        if (builders.hasNext()) {
            val builder = builders.next()
            return builder.build(introspector, dataSource)
        }

        // Fall back to base implementation
        return BaseRecordDataGenerator()
    }

    /**
     * Creates a RecordDataGenerator that can work with DatabaseData from the existing data generation system.
     * This method integrates with the existing DataGenerator to provide record generation capabilities.
     *
     * @param databaseData The DatabaseData containing table structures
     * @param config Configuration for record generation
     * @return Map of table name to generated record data
     */
    fun generateRecords(
        databaseData: DatabaseData,
        config: RecordGenerationConfig = RecordGenerationConfig()
    ): Map<String, RecordData> {
        val generator = BaseRecordDataGenerator()
        return generator.generateRecords(databaseData.tables, config)
    }

    @Suppress("ComplexMethod")
    private fun getDialectByMetaData(metadata: DatabaseMetaData): String? {
        // Try to determine dialect from database product name
        val productName = metadata.databaseProductName
        val driverName = metadata.driverName
        val url = metadata.url

        // Try to match by product name first
        val dialect = when {
            productName.contains("PostgreSQL", ignoreCase = true) -> "postgresql"
            productName.contains("MySQL", ignoreCase = true) -> "mysql"
            productName.contains("Oracle", ignoreCase = true) -> "oracle"
            productName.contains("H2", ignoreCase = true) -> "h2"
            productName.contains("MariaDB", ignoreCase = true) -> "mariadb"
            productName.contains("Microsoft SQL Server", ignoreCase = true) -> "sqlserver"
            // If product name doesn't match, try URL
            url.contains("postgresql", ignoreCase = true) -> "postgresql"
            url.contains("mysql", ignoreCase = true) -> "mysql"
            url.contains("oracle", ignoreCase = true) -> "oracle"
            url.contains("h2", ignoreCase = true) -> "h2"
            url.contains("mariadb", ignoreCase = true) -> "mariadb"
            url.contains("sqlserver", ignoreCase = true) -> "sqlserver"
            url.contains("jdbc:microsoft:sqlserver", ignoreCase = true) -> "sqlserver"
            // If URL doesn't match, try driver name
            driverName.contains("PostgreSQL", ignoreCase = true) -> "postgresql"
            driverName.contains("MySQL", ignoreCase = true) -> "mysql"
            driverName.contains("Oracle", ignoreCase = true) -> "oracle"
            driverName.contains("H2", ignoreCase = true) -> "h2"
            driverName.contains("MariaDB", ignoreCase = true) -> "mariadb"
            driverName.contains("SQLServer", ignoreCase = true) -> "sqlserver"
            driverName.contains("Microsoft JDBC Driver", ignoreCase = true) -> "sqlserver"
            // Default case
            else -> null
        }
        return dialect
    }
}
