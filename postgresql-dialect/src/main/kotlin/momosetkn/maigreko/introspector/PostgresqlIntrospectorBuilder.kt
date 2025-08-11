package momosetkn.maigreko.introspector

import javax.sql.DataSource

/**
 * Builder for PostgresqlIntrospector.
 * This builder is used by ServiceLoader to create a PostgresqlIntrospector instance
 * with the provided DataSource.
 */
class PostgresqlIntrospectorBuilder : IntrospectorBuilder {
    /**
     * The name of the dialect this builder creates introspectors for.
     */
    override val name: String = "postgresql"

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
