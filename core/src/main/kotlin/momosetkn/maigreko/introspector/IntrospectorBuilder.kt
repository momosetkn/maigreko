package momosetkn.maigreko.introspector

import javax.sql.DataSource

/**
 * Interface for building Introspector instances.
 * This is used to create Introspector instances with a DataSource.
 */
interface IntrospectorBuilder {
    /**
     * The name of the dialect this builder creates introspectors for.
     * This should be a lowercase string that matches the dialect name.
     */
    val name: String

    /**
     * Builds an Introspector instance with the given DataSource.
     *
     * @param dataSource The DataSource to use for database connections
     * @return An Introspector instance
     */
    fun build(dataSource: DataSource): Introspector
}
