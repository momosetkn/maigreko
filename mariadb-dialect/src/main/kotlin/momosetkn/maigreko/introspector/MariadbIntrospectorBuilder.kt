package momosetkn.maigreko.introspector

import javax.sql.DataSource

/**
 * Builder for MariadbIntrospector.
 * This builder is used by ServiceLoader to create a MariadbIntrospector instance
 * with the provided DataSource.
 */
class MariadbIntrospectorBuilder : IntrospectorBuilder {
    /**
     * Builds a MariadbIntrospector instance with the given DataSource.
     *
     * @param dataSource The DataSource to use for database connections
     * @return A MariadbIntrospector instance
     */
    override fun build(dataSource: DataSource): Introspector {
        return MariadbIntrospector(dataSource)
    }
}
