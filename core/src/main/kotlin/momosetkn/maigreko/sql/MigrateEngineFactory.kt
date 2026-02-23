package momosetkn.maigreko.sql

import momosetkn.maigreko.dialect.DialectFactory

/**
 * Factory for creating MigrateEngine instances.
 *
 * This facade remains for backward compatibility with existing tests/usages.
 * Internally it delegates to DialectFactory and returns the associated engine.
 */
object MigrateEngineFactory {
    /**
     * Create a MigrateEngine for the specified dialect key (e.g., "postgresql", "mysql").
     */
    fun create(dialect: String): MigrateEngine = DialectFactory.create(dialect).migrateEngine

    /**
     * Create the first available MigrateEngine discovered via ServiceLoader.
     */
    fun createFirst(): MigrateEngine = DialectFactory.createFirst().migrateEngine
}
