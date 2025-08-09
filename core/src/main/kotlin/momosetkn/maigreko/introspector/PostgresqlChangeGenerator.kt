package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.change.AddUniqueConstraint
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ForeignKeyAction
import momosetkn.maigreko.introspector.infras.PostgresqlColumnDetail
import momosetkn.maigreko.introspector.infras.PostgresqlConstraintDetail
import momosetkn.maigreko.introspector.infras.PostgresqlSequenceDetail

/**
 * Generates Change objects from PostgreSQL database information
 */
class PostgresqlChangeGenerator {
    /**
     * Generate a list of Change objects from PostgreSQL column details
     *
     * @param tableName The name of the table
     * @param columnDetails List of column details
     * @return List of Change objects
     */
    fun generateChangesFromColumns(
        tableName: String,
        columnDetails: List<PostgresqlColumnDetail>
    ): List<Change> {
        // Create columns for CreateTable change
        val columns = columnDetails.map { columnDetail ->
            Column(
                name = columnDetail.columnName,
                type = columnDetail.type,
                columnConstraint = ColumnConstraint(
                    nullable = columnDetail.notNull == "NO",
                    primaryKey = columnDetail.primaryKey == "YES",
                    unique = columnDetail.unique == "YES"
                )
            )
        }

        // Create the base CreateTable change
        val createTableChange = CreateTable(
            tableName = tableName,
            columns = columns
        )

        // Generate NotNull constraints
        val notNullConstraints = columnDetails
            .filter { it.notNull == "YES" }
            .map { columnDetail ->
                AddNotNullConstraint(
                    tableName = tableName,
                    columnName = columnDetail.columnName,
                    columnDataType = columnDetail.type
                )
            }

        // Generate Unique constraints
        val uniqueConstraints = columnDetails
            .filter { it.unique == "YES" }
            .map { columnDetail ->
                AddUniqueConstraint(
                    constraintName = "${tableName}_${columnDetail.columnName}_unique",
                    tableName = tableName,
                    columnNames = listOf(columnDetail.columnName)
                )
            }

        // Combine all changes
        return listOf(createTableChange) + notNullConstraints + uniqueConstraints
    }

    /**
     * Generate a list of Change objects from PostgreSQL constraint details
     *
     * @param constraintDetails List of constraint details
     * @return List of Change objects
     */
    fun generateChangesFromConstraints(
        constraintDetails: List<PostgresqlConstraintDetail>
    ): List<Change> {
        // Group constraints by constraint name to handle multi-column foreign keys
        val groupedConstraints = constraintDetails.groupBy { it.constraintName }

        // Map each constraint group to an AddForeignKey change
        return groupedConstraints.map { (constraintName, constraints) ->
            // All constraints in the group have the same table, foreign table, and actions
            val firstConstraint = constraints.first()

            AddForeignKey(
                constraintName = constraintName,
                tableName = firstConstraint.tableName,
                columnNames = constraints.map { it.columnName },
                referencedTableName = firstConstraint.foreignTableName,
                referencedColumnNames = constraints.map { it.foreignColumnName },
                onDelete = mapForeignKeyAction(firstConstraint.onDelete),
                onUpdate = mapForeignKeyAction(firstConstraint.onUpdate)
            )
        }
    }

    /**
     * Generate a list of Change objects from both column and constraint details for a single table
     *
     * @param tableName The name of the table
     * @param columnDetails List of column details
     * @param constraintDetails List of constraint details
     * @return List of Change objects ordered by dependencies
     */
    fun generateChanges(
        tableName: String,
        columnDetails: List<PostgresqlColumnDetail>,
        constraintDetails: List<PostgresqlConstraintDetail>
    ): List<Change> {
        return generateChanges(
            tableName = tableName,
            columnDetails = columnDetails,
            constraintDetails = constraintDetails,
            sequenceDetails = emptyList()
        )
    }

    /**
     * Generate a list of Change objects from column, constraint, and sequence details for a single table
     *
     * @param tableName The name of the table
     * @param columnDetails List of column details
     * @param constraintDetails List of constraint details
     * @param sequenceDetails List of sequence details
     * @return List of Change objects ordered by dependencies
     */
    fun generateChanges(
        tableName: String,
        columnDetails: List<PostgresqlColumnDetail>,
        constraintDetails: List<PostgresqlConstraintDetail>,
        sequenceDetails: List<PostgresqlSequenceDetail>
    ): List<Change> {
        // Generate changes from columns, constraints, and sequences
        val columnChanges = generateChangesFromColumns(tableName, columnDetails)
        val constraintChanges = generateChangesFromConstraints(constraintDetails)
        val sequenceChanges = generateChangesFromSequences(sequenceDetails)

        // Combine changes and sort based on dependencies
        return sortChangesByDependencies(columnChanges + constraintChanges + sequenceChanges)
    }

    /**
     * Generate a list of Change objects from column, constraint, and sequence details for multiple tables
     *
     * @param tableNames The names of the tables
     * @param columnDetails Map of table names to their column details
     * @param constraintDetails Map of table names to their constraint details
     * @param sequenceDetails List of sequence details
     * @return List of Change objects ordered by dependencies
     */
    fun generateChanges(
        tableInfoss: List<PostgresqlTableInfo>,
        sequenceDetails: List<PostgresqlSequenceDetail>,
    ): List<Change> {
        // Generate changes for each table
        val allChanges = tableInfoss.flatMap { tableInfo ->
            val columns = tableInfo.columnDetails
            val constraints = tableInfo.columnConstraints
            generateChangesFromColumns(tableInfo.tableName, columns) + generateChangesFromConstraints(constraints)
        }

        // Add sequence changes
        val sequenceChanges = generateChangesFromSequences(sequenceDetails)

        // Combine changes and sort based on dependencies
        return sortChangesByDependencies(allChanges + sequenceChanges)
    }

    /**
     * Sort changes based on their dependencies
     *
     * @param changes List of changes to sort
     * @return Sorted list of changes
     */
    fun sortChangesByDependencies(changes: List<Change>): List<Change> {
        // Create a map of table names to their CreateTable changes
        val tableCreationMap = changes.filterIsInstance<CreateTable>()
            .associateBy { it.tableName }

        // Create a map of table names to their foreign key dependencies
        val dependencyMap = changes.filterIsInstance<AddForeignKey>()
            .groupBy { it.tableName }
            .mapValues { (_, fks) -> fks.map { it.referencedTableName }.toSet() }

        // Topological sort to resolve dependencies
        val sortedTables = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        // Process all tables
        tableCreationMap.keys.forEach { tableName ->
            visitTableImmutable(tableName, tableCreationMap, dependencyMap, visited, visiting, sortedTables)
        }

        // Create sorted list of CreateTable changes
        val sortedCreateTables = sortedTables.mapNotNull { tableName -> tableCreationMap[tableName] }

        // Get processed tables
        val processedTables = sortedCreateTables.map { it.tableName }.toSet()

        // Filter foreign keys that reference existing tables
        val validForeignKeys = changes.filterIsInstance<AddForeignKey>()
            .filter { fk ->
                processedTables.contains(fk.tableName) &&
                    processedTables.contains(fk.referencedTableName)
            }

        // Filter other changes
        val otherChanges = changes.filter { change ->
            change !is CreateTable && change !is AddForeignKey
        }

        // Combine all changes in the correct order
        return sortedCreateTables + validForeignKeys + otherChanges
    }

    /**
     * Visit a table in the dependency graph for topological sorting (immutable version)
     *
     * @param tableName Name of the table to visit
     * @param tableCreationMap Map of table names to their CreateTable changes
     * @param dependencyMap Map of table names to their dependencies
     * @param visited Set of visited tables
     * @param visiting Set of tables currently being visited (for cycle detection)
     * @param result List to store the sorted table names
     */
    private fun visitTableImmutable(
        tableName: String,
        tableCreationMap: Map<String, CreateTable>,
        dependencyMap: Map<String, Set<String>>,
        visited: MutableSet<String>,
        visiting: MutableSet<String>,
        result: MutableList<String>
    ) {
        // Skip if already visited
        if (visited.contains(tableName)) {
            return
        }

        // Detect cycles
        if (visiting.contains(tableName)) {
            throw IllegalStateException("Circular dependency detected involving table: $tableName")
        }

        // Mark as being visited
        visiting.add(tableName)

        // Visit dependencies first
        dependencyMap[tableName]?.forEach { dependency ->
            visitTableImmutable(dependency, tableCreationMap, dependencyMap, visited, visiting, result)
        }

        // Mark as visited
        visiting.remove(tableName)
        visited.add(tableName)

        // Add the table name to the result
        result.add(tableName)
    }

    /**
     * Map PostgreSQL foreign key action to ForeignKeyAction enum
     *
     * @param action PostgreSQL foreign key action
     * @return ForeignKeyAction enum value or null if action is not recognized
     */
    private fun mapForeignKeyAction(action: String): ForeignKeyAction? {
        return when (action) {
            "a" -> ForeignKeyAction.NO_ACTION
            "r" -> ForeignKeyAction.RESTRICT
            "c" -> ForeignKeyAction.CASCADE
            "n" -> ForeignKeyAction.SET_NULL
            "d" -> ForeignKeyAction.SET_DEFAULT
            else -> null
        }
    }

    /**
     * Generate a list of Change objects from PostgreSQL sequence details
     *
     * @param sequenceDetails List of sequence details
     * @return List of Change objects
     */
    fun generateChangesFromSequences(
        sequenceDetails: List<PostgresqlSequenceDetail>
    ): List<Change> {
        return sequenceDetails.map { sequenceDetail ->
            CreateSequence(
                sequenceName = sequenceDetail.sequenceName,
                dataType = sequenceDetail.dataType,
                startValue = sequenceDetail.startValue,
                minValue = sequenceDetail.minValue,
                maxValue = sequenceDetail.maxValue,
                incrementBy = sequenceDetail.incrementBy,
                cycle = sequenceDetail.cycle,
                cacheSize = sequenceDetail.cacheSize
            )
        }
    }
}
