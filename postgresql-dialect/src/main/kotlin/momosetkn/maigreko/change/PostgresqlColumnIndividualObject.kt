package momosetkn.maigreko.change

data class PostgresqlColumnIndividualObject(
    /** Generation type（IDENTITY / serial / other） */
    val generatedKind: GeneratedKind? = null,
) : ColumnIndividualObject {
    enum class GeneratedKind(
        val sql: String,
        val isSerial: Boolean,
    ) {
        IDENTITY("identity", false),
        SERIAL("serial", true),
        SMALLSERIAL("smallserial", true),
        BIGSERIAL("bigserial", true),
        SERIAL_LIKE("serial-like", true),
        SMALLSERIAL_LIKE("smallserial-like", true),
        BIGSERIAL_LIKE("bigserial-like", true),
        OTHER("other", false),
        ;

        companion object {
            val default = IDENTITY
            fun fromSql(sql: String): GeneratedKind? {
                val s = sql.lowercase()
                return entries.find { it.sql == s }
            }
        }
    }

    companion object {
        fun build(generatedKind: String?) = PostgresqlColumnIndividualObject(
            generatedKind = generatedKind?.let { GeneratedKind.valueOf(it) }
        )
    }
}
