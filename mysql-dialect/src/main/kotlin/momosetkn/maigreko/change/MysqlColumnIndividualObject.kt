package momosetkn.maigreko.change

data class MysqlColumnIndividualObject(
    /** Generation type (AUTO_INCREMENT / other) */
    val generatedKind: GeneratedKind? = null,
) : ColumnIndividualObject {
    enum class GeneratedKind(
        val sql: String,
        val isAutoIncrement: Boolean,
    ) {
        AUTO_INCREMENT("auto_increment", true),
        OTHER("other", false),
        ;

        companion object {
            val default = AUTO_INCREMENT
            fun fromSql(sql: String): GeneratedKind? {
                val s = sql.lowercase()
                return entries.find { it.sql == s }
            }
        }
    }

    companion object {
        fun build(generatedKind: String?) = MysqlColumnIndividualObject(
            generatedKind = generatedKind?.let { GeneratedKind.valueOf(it) }
        )
    }
}