package momosetkn.maigreko.data

import momosetkn.maigreko.change.Change

/**
 * Represents database-specific data generated from Change objects.
 * This class contains processed and transformed data that can be used
 * for various database operations and reporting.
 */
data class DatabaseData(
    /**
     * The original Change objects that were processed
     */
    val sourceChanges: List<Change>,

    /**
     * Database-specific metadata extracted from the changes
     */
    val metadata: Map<String, Any> = emptyMap(),

    /**
     * Generated SQL statements or other database commands
     */
    val statements: List<String> = emptyList(),

    /**
     * Table information extracted from the changes
     */
    val tables: List<TableInfo> = emptyList(),

    /**
     * Index information extracted from the changes
     */
    val indexes: List<IndexInfo> = emptyList(),

    /**
     * Constraint information extracted from the changes
     */
    val constraints: List<ConstraintInfo> = emptyList(),

    /**
     * Sequence information extracted from the changes
     */
    val sequences: List<SequenceInfo> = emptyList()
)

/**
 * Represents table information extracted from Change objects
 */
data class TableInfo(
    val name: String,
    val columns: List<ColumnInfo>,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Represents column information
 */
data class ColumnInfo(
    val name: String,
    val type: String,
    val nullable: Boolean = true,
    val defaultValue: Any? = null,
    val autoIncrement: Boolean = false,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Represents index information
 */
data class IndexInfo(
    val name: String,
    val tableName: String,
    val columns: List<String>,
    val unique: Boolean = false,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Represents constraint information
 */
data class ConstraintInfo(
    val name: String,
    val type: ConstraintType,
    val tableName: String,
    val columns: List<String>,
    val referencedTable: String? = null,
    val referencedColumns: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Types of database constraints
 */
enum class ConstraintType {
    PRIMARY_KEY,
    FOREIGN_KEY,
    UNIQUE,
    CHECK,
    NOT_NULL
}

/**
 * Represents sequence information
 */
data class SequenceInfo(
    val name: String,
    val dataType: String? = null,
    val startValue: Long? = null,
    val incrementBy: Long? = null,
    val minValue: Long? = null,
    val maxValue: Long? = null,
    val cycle: Boolean = false,
    val metadata: Map<String, Any> = emptyMap()
)
