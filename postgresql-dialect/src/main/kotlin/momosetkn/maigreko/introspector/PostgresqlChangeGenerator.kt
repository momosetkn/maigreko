package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ForeignKeyAction
import momosetkn.maigreko.change.PostgresqlColumnIndividualObject
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
     * @return List of Change objects, List<Change>
     */
    fun generateChangesFromColumns(
        tableName: String,
        columnDetails: List<PostgresqlColumnDetail>
    ): Pair<List<Change>, List<String>> {
        // Create columns for CreateTable change
        val columns = columnDetails.map { columnDetail ->
            Column.build(
                name = columnDetail.columnName,
                type = columnDetail.type,
                defaultValue = columnDetail.columnDefault,
                columnConstraint = ColumnConstraint(
                    nullable = columnDetail.notNull == "NO",
                    primaryKey = columnDetail.primaryKey == "YES",
                    unique = columnDetail.unique == "YES"
                ),
                autoIncrement = columnDetail.ownedSequence != null, // auto generate sequence
                identityGeneration = columnDetail.identityGeneration,
                startValue = columnDetail.startValue,
                incrementBy = columnDetail.incrementBy,
                cycle = columnDetail.cycle,
                individualObject = PostgresqlColumnIndividualObject(
                    columnDetail.generatedKind?.let { PostgresqlColumnIndividualObject.GeneratedKind.fromSql(it) }
                )
            )
        }

        // Create the base CreateTable change
        val createTableChange = CreateTable(
            tableName = tableName,
            columns = columns
        )

//        // Generate NotNull constraints
//        val notNullConstraints = columnDetails
//            .filter { it.notNull == "YES" }
//            .map { columnDetail ->
//                AddNotNullConstraint(
//                    tableName = tableName,
//                    columnName = columnDetail.columnName,
//                    columnDataType = columnDetail.type
//                )
//            }

//        // Generate Unique constraints
//        val uniqueConstraints = columnDetails
//            .filter { it.unique == "YES" }
//            .map { columnDetail ->
//                AddUniqueConstraint(
//                    constraintName = "${tableName}_${columnDetail.columnName}_unique",
//                    tableName = tableName,
//                    columnNames = listOf(columnDetail.columnName)
//                )
//            }

        val createSequenceNames = columnDetails
            .mapNotNull {
                findSequenceName(it.columnDefault)
                    // Generate Sequence changes from column defaults
                    ?: it.ownedSequence?.split(".")?.last()
            }
        val sequenceChanges = columnDetails
            .filter { it.ownedSequence == null } // skip when auto-generated sequences
            .mapNotNull {
                extractSequenceFromColumnDefault(it)
            }

        // Combine all changes
        return Pair(
            listOf(createTableChange) + sequenceChanges,
            createSequenceNames
        )
    }

    /**
     * Extracts a `CreateSequence` object from the default value of a PostgreSQL column, if it contains a sequence.
     *
     * @param columnDetail The details of the PostgreSQL column, including its default value.
     * @return A `CreateSequence` object if a sequence is derived from the column's default value; otherwise, null.
     */
    private fun extractSequenceFromColumnDefault(columnDetail: PostgresqlColumnDetail): CreateSequence? {
        val name = findSequenceName(columnDetail.columnDefault)
        return name?.let {
            CreateSequence(
                sequenceName = name,
                dataType = columnDetail.type,
                generatedKind = columnDetail.generatedKind,
                identityGeneration = columnDetail.identityGeneration
            )
        }
    }

    /**
     * Extracts the sequence name from the default value of a PostgreSQL column, if it contains a sequence.
     *
     * @param columnDefault The default value of the PostgreSQL column, which may include a sequence reference.
     * @return The name of the sequence if it is found in the column's default value; otherwise, null.
     */
    private fun findSequenceName(columnDefault: String?): String? {
        if (columnDefault == null) {
            return null
        }

        // Extract sequence name from nextval expression
        val matchResult = sequenceNameRegex.find(columnDefault)

        return matchResult?.let {
            it.groupValues[1]
        }
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
        columnConstraints: List<PostgresqlConstraintDetail> = emptyList(),
        sequenceDetails: List<PostgresqlSequenceDetail> = emptyList(),
    ): List<Change> {
        return generateChanges(
            tableInfoss = listOf(
                PostgresqlTableInfo(
                    tableName = tableName,
                    columnDetails = columnDetails,
                    columnConstraints = columnConstraints
                )
            ),
            sequenceDetails = sequenceDetails,
        )
    }

    /**
     * Generate a list of Change objects from column, constraint, and sequence details for multiple tables
     *
     * @param tableInfoss List of table information
     * @param sequenceDetails List of sequence details
     * @return List of Change objects ordered by dependencies
     */
    fun generateChanges(
        tableInfoss: List<PostgresqlTableInfo>,
        sequenceDetails: List<PostgresqlSequenceDetail> = emptyList(),
    ): List<Change> {
        val allCreateSequenceNames = mutableSetOf<String>()
        // Generate changes for each table
        val tableChanges = tableInfoss.flatMap { tableInfo ->
            val columns = tableInfo.columnDetails
            val constraints = tableInfo.columnConstraints
            val (changes, createSequenceNames) = generateChangesFromColumns(tableInfo.tableName, columns)
            allCreateSequenceNames += createSequenceNames
            changes + generateChangesFromConstraints(constraints)
        }

//        val tableChangeSequenceNames = tableChanges
//            .mapNotNull {
//                val createSequence = it as? CreateSequence
//                createSequence?.sequenceName
//            }

        // Add sequence changes
        val sequenceChanges = generateChangesFromSequences(sequenceDetails)
        val filteredSequenceChanges = sequenceChanges
            .filter { sequenceChange ->
                sequenceChange.sequenceName !in allCreateSequenceNames
            }

        // Combine changes and sort based on dependencies
        return sortChangesByDependencies(tableChanges + filteredSequenceChanges)
    }

    /**
     * Sort changes based on their dependencies
     *
     * @param changes List of changes to sort
     * @return Sorted list of changes
     */
    fun sortChangesByDependencies(changes: List<Change>): List<Change> {
//        // Create a map of table names to their CreateTable changes
//        val tableCreationMap = changes.filterIsInstance<CreateTable>()
//            .associateBy { it.tableName }
//
//        // Create a map of table names to their foreign key dependencies
//        val dependencyMap = changes.filterIsInstance<AddForeignKey>()
//            .groupBy { it.tableName }
//            .mapValues { (_, fks) -> fks.map { it.referencedTableName }.toSet() }
//
//        // Topological sort to resolve dependencies
//        val sortedTables = mutableListOf<String>()
//        val visited = mutableSetOf<String>()
//        val visiting = mutableSetOf<String>()
//
//        // Process all tables
//        tableCreationMap.keys.forEach { tableName ->
//            visitTableImmutable(tableName, tableCreationMap, dependencyMap, visited, visiting, sortedTables)
//        }
//
//        // Create sorted list of CreateTable changes
//        val sortedCreateTables = sortedTables.mapNotNull { tableName -> tableCreationMap[tableName] }
//
//        // Get processed tables
//        val processedTables = sortedCreateTables.map { it.tableName }.toSet()
//
//        // Filter foreign keys that reference existing tables
//        val validForeignKeys = changes.filterIsInstance<AddForeignKey>()
//            .filter { fk ->
//                processedTables.contains(fk.tableName) &&
//                    processedTables.contains(fk.referencedTableName)
//            }
//
//        // Filter other changes
//        val otherChanges = changes.filter { change ->
//            change !is CreateTable && change !is AddForeignKey
//        }
//
//        // Combine all changes in the correct order
//        return sortedCreateTables + validForeignKeys + otherChanges

        @Suppress("MagicNumber")
        fun getSortKey1(c: Change) = when (c) {
            is CreateSequence -> 100
            is CreateTable -> 200
            is AddForeignKey -> 300
            else -> 500
        }
        fun getSortKey2(c: Change) = when (c) {
            is CreateSequence -> c.sequenceName
            is CreateTable -> c.tableName
            is AddForeignKey -> c.tableName
            else -> ""
        }
        val sorted = changes.sortedWith(
            compareBy(::getSortKey1)
                .thenBy(::getSortKey2)
        )
        return sorted
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
    ): List<CreateSequence> {
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

    companion object {
        // Format is typically: nextval('sequence_name'::regclass)
        private val sequenceNameRegex = Regex("nextval\\('(?<sequenceName>.+)'::regclass\\)")
    }
}