package momosetkn.maigreko.data

import momosetkn.maigreko.change.AddColumn
import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.AddIndex
import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.change.AddUniqueConstraint
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ModifyDataType
import momosetkn.maigreko.change.RenameColumn
import momosetkn.maigreko.change.RenameTable
import momosetkn.maigreko.introspector.Introspector

/**
 * Base implementation of DataGenerator that provides common functionality
 * for processing Change objects into DatabaseData structures.
 * Database-specific implementations can extend this class and override
 * methods for dialect-specific behavior.
 */
interface BaseDataGenerator : DataGenerator {
    val introspector: Introspector

    /**
     * Generates database-specific data from a list of Change objects.
     * This base implementation extracts common database elements like tables,
     * indexes, constraints, and sequences from the Change objects.
     *
     * @param changes List of Change objects to process
     * @return Generated DatabaseData containing processed information
     */
    override fun generateData(changes: List<Change>): DatabaseData {
        val tables = extractTables(changes)
        val indexes = extractIndexes(changes)
        val constraints = extractConstraints(changes)
        val sequences = extractSequences(changes)
        val statements = generateStatements(changes)
        val metadata = generateMetadata(changes)

        return DatabaseData(
            sourceChanges = changes,
            metadata = metadata,
            statements = statements,
            tables = tables,
            indexes = indexes,
            constraints = constraints,
            sequences = sequences
        )
    }

    /**
     * Extracts table information from Change objects.
     * Processes CreateTable and AddColumn changes to build table structures.
     *
     * @param changes List of Change objects
     * @return List of TableInfo objects
     */
    fun extractTables(changes: List<Change>): List<TableInfo> {
        val tables = mutableMapOf<String, MutableList<ColumnInfo>>()

        changes.forEach { change ->
            when (change) {
                is CreateTable -> {
                    val columns = change.columns.map { column ->
                        ColumnInfo(
                            name = column.name,
                            type = column.type,
                            nullable = column.columnConstraint?.nullable ?: true,
                            defaultValue = column.defaultValue,
                            autoIncrement = column.autoIncrement,
                            metadata = buildColumnMetadata(column)
                        )
                    }
                    tables[change.tableName] = columns.toMutableList()
                }
                is AddColumn -> {
                    val column = change.column
                    val columnInfo = ColumnInfo(
                        name = column.name,
                        type = column.type,
                        nullable = column.columnConstraint?.nullable ?: true,
                        defaultValue = column.defaultValue,
                        autoIncrement = column.autoIncrement,
                        metadata = buildColumnMetadata(column)
                    )
                    tables.getOrPut(change.tableName) { mutableListOf() }.add(columnInfo)
                }
                else -> { /* Ignore other change types for table extraction */ }
            }
        }

        return tables.map { (tableName, columns) ->
            TableInfo(
                name = tableName,
                columns = columns,
                metadata = buildTableMetadata(tableName, changes)
            )
        }
    }

    /**
     * Extracts index information from Change objects.
     *
     * @param changes List of Change objects
     * @return List of IndexInfo objects
     */
    fun extractIndexes(changes: List<Change>): List<IndexInfo> {
        return changes.filterIsInstance<AddIndex>().map { change ->
            IndexInfo(
                name = change.indexName,
                tableName = change.tableName,
                columns = change.columnNames,
                unique = change.unique,
                metadata = buildIndexMetadata(change)
            )
        }
    }

    /**
     * Extracts constraint information from Change objects.
     *
     * @param changes List of Change objects
     * @return List of ConstraintInfo objects
     */
    fun extractConstraints(changes: List<Change>): List<ConstraintInfo> {
        val constraints = mutableListOf<ConstraintInfo>()

        changes.forEach { change ->
            when (change) {
                is AddForeignKey -> {
                    constraints.add(
                        ConstraintInfo(
                            name = change.constraintName,
                            type = ConstraintType.FOREIGN_KEY,
                            tableName = change.tableName,
                            columns = change.columnNames,
                            referencedTable = change.referencedTableName,
                            referencedColumns = change.referencedColumnNames,
                            metadata = buildForeignKeyMetadata(change)
                        )
                    )
                }
                is AddUniqueConstraint -> {
                    constraints.add(
                        ConstraintInfo(
                            name = change.constraintName,
                            type = ConstraintType.UNIQUE,
                            tableName = change.tableName,
                            columns = change.columnNames,
                            metadata = buildUniqueConstraintMetadata(change)
                        )
                    )
                }
                is AddNotNullConstraint -> {
                    constraints.add(
                        ConstraintInfo(
                            name = "${change.tableName}_${change.columnName}_not_null",
                            type = ConstraintType.NOT_NULL,
                            tableName = change.tableName,
                            columns = listOf(change.columnName),
                            metadata = buildNotNullConstraintMetadata(change)
                        )
                    )
                }
                is CreateTable -> {
                    // Extract primary key and other constraints from table columns
                    change.columns.forEach { column ->
                        column.columnConstraint?.let { constraint ->
                            if (constraint.primaryKey) {
                                constraints.add(
                                    ConstraintInfo(
                                        name = "${change.tableName}_${column.name}_pk",
                                        type = ConstraintType.PRIMARY_KEY,
                                        tableName = change.tableName,
                                        columns = listOf(column.name),
                                        metadata = buildPrimaryKeyMetadata(change, column)
                                    )
                                )
                            }
                        }
                    }
                }
                else -> { /* Ignore other change types for constraint extraction */ }
            }
        }

        return constraints
    }

    /**
     * Extracts sequence information from Change objects.
     *
     * @param changes List of Change objects
     * @return List of SequenceInfo objects
     */
    fun extractSequences(changes: List<Change>): List<SequenceInfo> {
        return changes.filterIsInstance<CreateSequence>().map { change ->
            SequenceInfo(
                name = change.sequenceName,
                dataType = change.dataType,
                startValue = change.startValue,
                incrementBy = change.incrementBy,
                minValue = change.minValue,
                maxValue = change.maxValue,
                cycle = change.cycle,
                metadata = buildSequenceMetadata(change)
            )
        }
    }

    /**
     * Generates SQL statements or other database commands from Change objects.
     * This base implementation returns empty list - subclasses should override for dialect-specific SQL generation.
     *
     * @param changes List of Change objects
     * @return List of SQL statements or commands
     */
    fun generateStatements(changes: List<Change>): List<String> {
        return emptyList()
    }

    /**
     * Generates general metadata about the processed changes.
     *
     * @param changes List of Change objects
     * @return Map of metadata key-value pairs
     */
    fun generateMetadata(changes: List<Change>): Map<String, Any> {
        val changeTypeCounts = changes.groupBy { it::class.simpleName }.mapValues { it.value.size }
        return mapOf(
            "totalChanges" to changes.size,
            "changeTypeCounts" to changeTypeCounts,
            "generatedAt" to System.currentTimeMillis()
        )
    }

    // Metadata building methods - can be overridden by subclasses for dialect-specific metadata

    fun buildColumnMetadata(column: Column): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        column.identityGeneration?.let { metadata["identityGeneration"] = it }
        column.startValue?.let { metadata["startValue"] = it }
        column.incrementBy?.let { metadata["incrementBy"] = it }
        column.cycle?.let { metadata["cycle"] = it }
        column.individualObject?.let { metadata["individualObject"] = it }
        return metadata
    }

    fun buildTableMetadata(tableName: String, changes: List<Change>): Map<String, Any> {
        val tableChanges = changes.filter { change ->
            when (change) {
                is CreateTable -> change.tableName == tableName
                is AddColumn -> change.tableName == tableName
                is RenameTable -> change.oldTableName == tableName || change.newTableName == tableName
                is RenameColumn -> change.tableName == tableName
                is ModifyDataType -> change.tableName == tableName
                is AddNotNullConstraint -> change.tableName == tableName
                is AddForeignKey -> change.tableName == tableName
                is AddUniqueConstraint -> change.tableName == tableName
                is AddIndex -> change.tableName == tableName
                else -> false
            }
        }
        return mapOf("relatedChanges" to tableChanges.size)
    }

    fun buildIndexMetadata(change: AddIndex): Map<String, Any> = emptyMap()

    fun buildForeignKeyMetadata(change: AddForeignKey): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        change.onDelete?.let { metadata["onDelete"] = it }
        change.onUpdate?.let { metadata["onUpdate"] = it }
        metadata["deferrable"] = change.deferrable
        metadata["initiallyDeferred"] = change.initiallyDeferred
        return metadata
    }

    fun buildUniqueConstraintMetadata(change: AddUniqueConstraint): Map<String, Any> = emptyMap()

    fun buildNotNullConstraintMetadata(change: AddNotNullConstraint): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        metadata["columnDataType"] = change.columnDataType
        change.defaultValue?.let { metadata["defaultValue"] = it }
        return metadata
    }

    fun buildPrimaryKeyMetadata(change: CreateTable, column: Column): Map<String, Any> = emptyMap()

    fun buildSequenceMetadata(change: CreateSequence): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        change.generatedKind?.let { metadata["generatedKind"] = it }
        change.identityGeneration?.let { metadata["identityGeneration"] = it }
        change.cacheSize?.let { metadata["cacheSize"] = it }
        return metadata
    }
}
