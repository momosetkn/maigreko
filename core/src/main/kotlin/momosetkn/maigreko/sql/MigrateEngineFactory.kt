package momosetkn.maigreko.sql

import java.sql.DatabaseMetaData
import java.util.ServiceLoader
import javax.sql.DataSource

/**
 * Factory for creating MigrateEngine instances using ServiceLoader.
 */
object MigrateEngineFactory {
    /**
     * Create MigrateEngine by detecting the dialect via the provided DataSource.
     * Preferred when you only have a DataSource.
     */
    fun create(dataSource: DataSource): MigrateEngine {
        dataSource.connection.use { connection ->
            val metadata = connection.metaData
            val dialect = getDialectByMetaData(metadata)
            return dialect?.let { create(it) } ?: createFirst()
        }
    }

    /**
     * Create MigrateEngine by explicitly specifying a dialect key (e.g., "postgresql", "mysql").
     */
    fun create(dialect: String): MigrateEngine {
        val loader = ServiceLoader.load(MigrateEngine::class.java)
        val iterator = loader.iterator()
        val dialectLower = dialect.lowercase()
        val candidates = mutableListOf<MigrateEngine>()
        while (iterator.hasNext()) {
            val engine = iterator.next()
            if (engine.name.contains(dialectLower)) {
                candidates.add(engine)
            }
        }
        return when (candidates.size) {
            0 -> throw IllegalArgumentException("No MigrateEngine found for dialect: $dialect")
            1 -> candidates.first()
            else -> {
                val candidateNames = candidates.map { it::class.qualifiedName }
                throw IllegalArgumentException(
                    "Multiple MigrateEngine candidates found for dialect: $dialect -> $candidateNames"
                )
            }
        }
    }

    /**
     * Create the first available MigrateEngine from ServiceLoader as a fallback.
     */
    fun createFirst(): MigrateEngine {
        val loader = ServiceLoader.load(MigrateEngine::class.java)
        val it = loader.iterator()
        if (it.hasNext()) return it.next()
        throw IllegalArgumentException("No MigrateEngine implementation found")
    }

    /**
     * Try to derive a dialect key from JDBC DatabaseMetaData.
     */
    @Suppress("ComplexMethod")
    private fun getDialectByMetaData(metadata: DatabaseMetaData): String? {
        val productName = metadata.databaseProductName
        val driverName = metadata.driverName
        val url = metadata.url ?: ""
        return when {
            productName.contains("PostgreSQL", true) -> "postgresql"
            productName.contains("MySQL", true) -> "mysql"
            productName.contains("Oracle", true) -> "oracle"
            productName.contains("H2", true) -> "h2"
            productName.contains("MariaDB", true) -> "mariadb"
            productName.contains("Microsoft SQL Server", true) -> "sqlserver"
            url.contains("postgresql", true) -> "postgresql"
            url.contains("mysql", true) -> "mysql"
            url.contains("oracle", true) -> "oracle"
            url.contains("h2", true) -> "h2"
            url.contains("mariadb", true) -> "mariadb"
            url.contains("sqlserver", true) || url.contains("jdbc:microsoft:sqlserver", true) -> "sqlserver"
            driverName.contains("PostgreSQL", true) -> "postgresql"
            driverName.contains("MySQL", true) -> "mysql"
            driverName.contains("Oracle", true) -> "oracle"
            driverName.contains("H2", true) -> "h2"
            driverName.contains("MariaDB", true) -> "mariadb"
            driverName.contains("SQLServer", true) || driverName.contains("Microsoft JDBC Driver", true) -> "sqlserver"
            else -> null
        }
    }
}
