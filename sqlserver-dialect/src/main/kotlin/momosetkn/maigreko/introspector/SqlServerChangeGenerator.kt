package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ForeignKeyAction
import momosetkn.maigreko.introspector.infras.SqlServerColumnDetail
import momosetkn.maigreko.introspector.infras.SqlServerConstraintDetail
import momosetkn.maigreko.introspector.infras.SqlServerSequenceDetail

/**
 * Generates Change objects from SQL Server database information
 */
class SqlServerChangeGenerator {
    /**
     * Generate a list of Change objects from SQL Server column details
     *
     * @param tableName The name of the table
     * @param columnDetails List of column details
     * @return List of Change objects, List<Change>
     */
    fun generateChangesFromColumns(
        tableName: String,
        columnDetails: List<SqlServerColumnDetail>
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
                autoIncrement = columnDetail.generatedKind == "IDENTITY", // SQL Server uses IDENTITY
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

        return Pair(listOf(createTableChange), emptyList())
    }

    /**
     * Generate foreign key changes from constraint details
     *
     * @param constraintDetails List of constraint details
     * @return List of AddForeignKey changes
     */
    fun generateForeignKeyChanges(
        constraintDetails: List<SqlServerConstraintDetail>
    ): List<AddForeignKey> {
        // Group constraints by constraint name to handle multi-column foreign keys
        val groupedConstraints = constraintDetails.groupBy { it.constraintName }

        return groupedConstraints.map { (constraintName, constraints) ->
            val firstConstraint = constraints.first()

            // Map SQL Server action strings to ForeignKeyAction enum
            val onDelete = when (firstConstraint.onDelete) {
                "CASCADE" -> ForeignKeyAction.CASCADE
                "SET NULL" -> ForeignKeyAction.SET_NULL
                "SET DEFAULT" -> ForeignKeyAction.SET_DEFAULT
                "NO ACTION" -> ForeignKeyAction.NO_ACTION
                else -> ForeignKeyAction.NO_ACTION
            }

            val onUpdate = when (firstConstraint.onUpdate) {
                "CASCADE" -> ForeignKeyAction.CASCADE
                "SET NULL" -> ForeignKeyAction.SET_NULL
                "SET DEFAULT" -> ForeignKeyAction.SET_DEFAULT
                "NO ACTION" -> ForeignKeyAction.NO_ACTION
                else -> ForeignKeyAction.NO_ACTION
            }

            AddForeignKey(
                constraintName = constraintName,
                tableName = firstConstraint.tableName,
                columnNames = constraints.map { it.columnName },
                referencedTableName = firstConstraint.foreignTableName,
                referencedColumnNames = constraints.map { it.foreignColumnName },
                onDelete = onDelete,
                onUpdate = onUpdate
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
        sequenceDetails: List<SqlServerSequenceDetail>
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
        tableInfos: List<SqlServerTableInfo>,
        sequenceDetails: List<SqlServerSequenceDetail>
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
