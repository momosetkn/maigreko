package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ForeignKeyAction
import momosetkn.maigreko.introspector.infras.OracleColumnDetail
import momosetkn.maigreko.introspector.infras.OracleConstraintDetail
import momosetkn.maigreko.introspector.infras.OracleSequenceDetail

/**
 * Generates Change objects from Oracle database information
 */
class OracleChangeGenerator {
    /**
     * Generate a list of Change objects from Oracle column details
     *
     * @param tableName The name of the table
     * @param columnDetails List of column details
     * @return List of Change objects, List<Change>
     */
    fun generateChangesFromColumns(
        tableName: String,
        columnDetails: List<OracleColumnDetail>
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
                autoIncrement = columnDetail.generatedKind == "IDENTITY", // Oracle uses IDENTITY
                identityGeneration = columnDetail.identityGeneration,
                startValue = columnDetail.startValue,
                incrementBy = columnDetail.incrementBy,
                cycle = columnDetail.cycle
            )
        }

        // Create the base CreateTable change
        val createTableChange = CreateTable(
            tableName = tableName,
            columns = columns
        )

        // Collect sequence names that are used by columns
        val usedSequences = columnDetails
            .mapNotNull { it.ownedSequence }
            .toList()

        return Pair(listOf(createTableChange), usedSequences)
    }

    /**
     * Generate foreign key changes from constraint details
     *
     * @param constraintDetails List of constraint details
     * @return List of AddForeignKey changes
     */
    fun generateForeignKeyChanges(
        constraintDetails: List<OracleConstraintDetail>
    ): List<AddForeignKey> {
        // Group constraints by constraint name to handle multi-column foreign keys
        val groupedConstraints = constraintDetails.groupBy { it.constraintName }

        return groupedConstraints.map { (constraintName, constraints) ->
            val firstConstraint = constraints.first()

            // Map Oracle action strings to ForeignKeyAction enum
            // Note: Oracle doesn't support ON UPDATE actions, so onUpdate is always NO_ACTION
            val onDelete = when (firstConstraint.onDelete) {
                "CASCADE" -> ForeignKeyAction.CASCADE
                "SET NULL" -> ForeignKeyAction.SET_NULL
                else -> ForeignKeyAction.NO_ACTION
            }

            AddForeignKey(
                constraintName = constraintName,
                tableName = firstConstraint.tableName,
                columnNames = constraints.map { it.columnName },
                referencedTableName = firstConstraint.foreignTableName,
                referencedColumnNames = constraints.map { it.foreignColumnName },
                onDelete = onDelete,
                onUpdate = ForeignKeyAction.NO_ACTION // Oracle doesn't support ON UPDATE actions
            )
        }
    }

    /**
     * Generate sequence changes from sequence details
     *
     * @param sequenceDetails List of sequence details
     * @return List of CreateSequence changes
     */
    fun generateSequenceChanges(
        sequenceDetails: List<OracleSequenceDetail>
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

    /**
     * Generate all changes from table info and sequence details
     *
     * @param tableInfos List of table info
     * @param sequenceDetails List of sequence details
     * @return List of Change objects
     */
    fun generateChanges(
        tableInfos: List<OracleTableInfo>,
        sequenceDetails: List<OracleSequenceDetail>
    ): List<Change> {
        val changes = mutableListOf<Change>()
        val processedSequences = mutableListOf<String>()

        // Process tables
        tableInfos.forEach { tableInfo ->
            val (tableChanges, tableSequences) = generateChangesFromColumns(
                tableInfo.tableName,
                tableInfo.columnDetails
            )
            changes.addAll(tableChanges)
            processedSequences.addAll(tableSequences)
        }

        // Process foreign keys
        val allConstraints = tableInfos.flatMap { it.columnConstraints }
        changes.addAll(generateForeignKeyChanges(allConstraints))

        // Process standalone sequences (not owned by columns)
        val standaloneSequences = sequenceDetails.filter { sequenceDetail ->
            !processedSequences.contains(sequenceDetail.sequenceName)
        }
        changes.addAll(generateSequenceChanges(standaloneSequences))

        return changes
    }
}
