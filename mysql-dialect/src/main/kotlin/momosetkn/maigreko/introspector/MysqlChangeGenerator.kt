package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ForeignKeyAction
import momosetkn.maigreko.change.MysqlColumnIndividualObject
import momosetkn.maigreko.introspector.infras.MysqlColumnDetail
import momosetkn.maigreko.introspector.infras.MysqlConstraintDetail
import momosetkn.maigreko.introspector.infras.MysqlSequenceDetail

/**
 * Generates Change objects from MySQL database information
 */
class MysqlChangeGenerator {
    /**
     * Generate a list of Change objects from MySQL column details
     *
     * @param tableName The name of the table
     * @param columnDetails List of column details
     * @return List of Change objects, List<Change>
     */
    fun generateChangesFromColumns(
        tableName: String,
        columnDetails: List<MysqlColumnDetail>
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
                individualObject = MysqlColumnIndividualObject.build(columnDetail.generatedKind),
                autoIncrement = columnDetail.generatedKind == "AUTO_INCREMENT", // For backward compatibility
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
        constraintDetails: List<MysqlConstraintDetail>
    ): List<AddForeignKey> {
        // Group constraints by constraint name to handle multi-column foreign keys
        val groupedConstraints = constraintDetails.groupBy { it.constraintName }

        return groupedConstraints.map { (constraintName, constraints) ->
            val firstConstraint = constraints.first()

            // Map MySQL action strings to ForeignKeyAction enum
            val onDelete = when (firstConstraint.onDelete) {
                "CASCADE" -> ForeignKeyAction.CASCADE
                "SET NULL" -> ForeignKeyAction.SET_NULL
                "SET DEFAULT" -> ForeignKeyAction.SET_DEFAULT
                "RESTRICT" -> ForeignKeyAction.RESTRICT
                else -> ForeignKeyAction.NO_ACTION
            }

            val onUpdate = when (firstConstraint.onUpdate) {
                "CASCADE" -> ForeignKeyAction.CASCADE
                "SET NULL" -> ForeignKeyAction.SET_NULL
                "SET DEFAULT" -> ForeignKeyAction.SET_DEFAULT
                "RESTRICT" -> ForeignKeyAction.RESTRICT
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
     * Note: MySQL 8.0+ supports sequences, but older versions don't
     *
     * @param sequenceDetails List of sequence details
     * @return List of CreateSequence changes
     */
    fun generateSequenceChanges(
        sequenceDetails: List<MysqlSequenceDetail>
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
        tableInfos: List<MysqlTableInfo>,
        sequenceDetails: List<MysqlSequenceDetail>
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
        // For MySQL 8.0+ this might do something, for older versions it will do nothing
        val standaloneSequences = sequenceDetails.filter { sequenceDetail ->
            !processedSequences.contains(sequenceDetail.sequenceName)
        }
        changes.addAll(generateSequenceChanges(standaloneSequences))

        return changes
    }
}
