package momosetkn.maigreko.introspector

import javax.sql.DataSource

/**
 * Builder for OracleIntrospector.
 * This builder is used by ServiceLoader to create an OracleIntrospector instance
 * with the provided DataSource.
 */
class OracleIntrospectorBuilder : IntrospectorBuilder {
    /**
     * Builds an OracleIntrospector instance with the given DataSource.
     *
     * @param dataSource The DataSource to use for database connections
     * @return An OracleIntrospector instance
     */
    override fun build(dataSource: DataSource): Introspector {
        return OracleIntrospector(dataSource)
    }
}
