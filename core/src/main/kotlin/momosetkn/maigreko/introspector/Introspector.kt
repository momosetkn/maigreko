package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.Change

/**
 * Interface for database introspection to generate Change objects
 * from database schema information.
 */
interface Introspector {
    /**
     * Introspects the database and generates a list of Change objects
     * representing the database schema.
     *
     * @return List of Change objects
     */
    fun introspect(): List<Change>
}
