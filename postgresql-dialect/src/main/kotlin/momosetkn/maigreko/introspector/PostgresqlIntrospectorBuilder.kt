package momosetkn.maigreko.introspector

import javax.sql.DataSource

/**
 * Builder for PostgresqlIntrospector.
 * This builder is used by ServiceLoader to create a PostgresqlIntrospector instance
 * with the provided DataSource.
 */
class PostgresqlIntrospectorBuilder : IntrospectorBuilder {
    /**
     * Builds a PostgresqlIntrospector instance with the given DataSource.
     *
     * @param dataSource The DataSource to use for database connections
     * @return A PostgresqlIntrospector instance
     */
    override fun build(dataSource: DataSource): Introspector {
        return PostgresqlIntrospector(dataSource)
    }
}
