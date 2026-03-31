package momosetkn.maigreko.data

import momosetkn.maigreko.change.Change

/**
 * Interface for generating database-specific data from Change objects.
 * This interface defines the contract for converting Change objects
 * into database-specific data structures or formats.
 */
interface DataGenerator {
    /**
     * Generates database-specific data from a list of Change objects.
     *
     * @param changes List of Change objects to process
     * @return Generated database-specific data
     */
    fun generateData(changes: List<Change>): DatabaseData
}
