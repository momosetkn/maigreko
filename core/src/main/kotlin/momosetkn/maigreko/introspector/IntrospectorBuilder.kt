package momosetkn.maigreko.introspector

import javax.sql.DataSource

/**
 * Interface for building Introspector instances.
 * This is used to create Introspector instances with a DataSource.
 */
interface IntrospectorBuilder {
    /**
     * Builds an Introspector instance with the given DataSource.
     *
     * @param dataSource The DataSource to use for database connections
     * @return An Introspector instance
     */
    fun build(dataSource: DataSource): Introspector
}
