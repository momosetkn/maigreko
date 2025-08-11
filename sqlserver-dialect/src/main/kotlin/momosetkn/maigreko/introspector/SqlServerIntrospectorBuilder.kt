package momosetkn.maigreko.introspector

import javax.sql.DataSource

/**
 * Builder for SqlServerIntrospector.
 * This builder is used by ServiceLoader to create a SqlServerIntrospector instance
 * with the provided DataSource.
 */
class SqlServerIntrospectorBuilder : IntrospectorBuilder {
    /**
     * The name of the dialect this builder creates introspectors for.
     */
    override val name: String = "sqlserver"

    /**
     * Builds a SqlServerIntrospector instance with the given DataSource.
     *
     * @param dataSource The DataSource to use for database connections
     * @return A SqlServerIntrospector instance
     */
    override fun build(dataSource: DataSource): Introspector {
        return SqlServerIntrospector(dataSource)
    }
}
