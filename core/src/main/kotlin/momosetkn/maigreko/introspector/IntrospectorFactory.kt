package momosetkn.maigreko.introspector

import java.sql.DatabaseMetaData
import java.sql.SQLException
import java.util.ServiceLoader
import javax.sql.DataSource

/**
 * Factory for creating Introspector instances.
 * Uses Java's ServiceLoader mechanism to discover Introspector implementations.
 */
object IntrospectorFactory {
    /**
     * Creates an Introspector instance based on database metadata.
     * Automatically detects the appropriate dialect from the database connection.
     *
     * @param dataSource The DataSource to use for database connections
     * @return An Introspector instance for the detected dialect
     * @throws IllegalArgumentException if no Introspector is found for the detected dialect
     * @throws SQLException if there is an error accessing the database metadata
     */
    fun create(dataSource: DataSource): Introspector {
        dataSource.connection.use { connection ->
            val metadata = connection.metaData
            val dialect = getDialectByMetaData(metadata)

            return dialect?.let { create(it, dataSource) } ?: createFirst(dataSource)
        }
    }

    /**
     * Creates an Introspector instance for the specified dialect.
     *
     * @param dialect The database dialect to create an Introspector for
     * @param dataSource The DataSource to use for database connections
     * @return An Introspector instance for the specified dialect
     * @throws IllegalArgumentException if no Introspector is found for the specified dialect
     */
    fun create(dialect: String, dataSource: DataSource): Introspector {
        // First try to find a builder for the dialect
        val builderLoader = ServiceLoader.load(IntrospectorBuilder::class.java)
        val builders = builderLoader.iterator()

        while (builders.hasNext()) {
            val builder = builders.next()
            if (builder.name.startsWith(dialect.lowercase())) {
                return builder.build(dataSource)
            }
        }

        throw IllegalArgumentException("No Introspector found for dialect: $dialect")
    }

    /**
     * Creates an Introspector instance by loading the first available implementation
     * from ServiceLoader.
     *
     * @param dataSource The DataSource to use for database connections
     * @return The first available Introspector instance
     * @throws IllegalArgumentException if no Introspector implementation is found
     */
    fun createFirst(dataSource: DataSource): Introspector {
        // First try to find a builder
        val builderLoader = ServiceLoader.load(IntrospectorBuilder::class.java)
        val builders = builderLoader.iterator()

        if (builders.hasNext()) {
            val builder = builders.next()
            return builder.build(dataSource)
        }

        throw IllegalArgumentException("No Introspector implementation found")
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
