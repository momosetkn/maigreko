package momosetkn.maigreko.data

import momosetkn.maigreko.introspector.Introspector
import javax.sql.DataSource

/**
 * Builder interface for creating RecordDataGenerator instances.
 * Implementations of this interface are discovered using Java's ServiceLoader mechanism.
 * Each database dialect should provide its own implementation if needed.
 */
interface RecordDataGeneratorBuilder {
    /**
     * The name of this builder, typically the database dialect name.
     * Used for matching against database types.
     *
     * @return The name of this builder (e.g., "postgresql", "mysql", etc.)
     */
    val name: String

    /**
     * Creates a RecordDataGenerator instance for the specific database dialect.
     *
     * @param introspector The Introspector instance to use for database introspection (optional)
     * @param dataSource The DataSource for database connections (optional)
     * @return A RecordDataGenerator instance for the specific dialect
     */
    fun build(introspector: Introspector? = null, dataSource: DataSource? = null): RecordDataGenerator
}
