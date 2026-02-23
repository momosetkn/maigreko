package momosetkn.maigreko.introspector

import javax.sql.DataSource

/**
 * Builder for MysqlIntrospector.
 * This builder is used by ServiceLoader to create a MysqlIntrospector instance
 * with the provided DataSource.
 */
class MysqlIntrospectorBuilder : IntrospectorBuilder {
    /**
     * Builds a MysqlIntrospector instance with the given DataSource.
     *
     * @param dataSource The DataSource to use for database connections
     * @return A MysqlIntrospector instance
     */
    override fun build(dataSource: DataSource): Introspector {
        return MysqlIntrospector(dataSource)
    }
}
