package momosetkn.maigreko.introspector

import javax.sql.DataSource

/**
 * Builder for H2Introspector.
 * This builder is used by ServiceLoader to create a H2Introspector instance
 * with the provided DataSource.
 */
class H2IntrospectorBuilder : IntrospectorBuilder {
    /**
     * No-arg constructor required by ServiceLoader.
     */
    constructor()

    /**
     * Builds a H2Introspector instance with the given DataSource.
     *
     * @param dataSource The DataSource to use for database connections
     * @return A H2Introspector instance
     */
    override fun build(dataSource: DataSource): Introspector {
        return H2Introspector(dataSource)
    }
}
