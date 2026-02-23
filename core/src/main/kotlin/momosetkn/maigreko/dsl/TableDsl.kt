package momosetkn.maigreko.dsl

import momosetkn.maigreko.change.Column

@MaigrekoDslAnnotation
class TableDsl(
    val tableName: String,
) {
    internal val columns = mutableListOf<Column>()

    fun column(
        name: String,
        type: String,
        block: (ColumnDsl.() -> Unit)? = null,
    ) {
        val cb = ColumnDsl(name, type)
        block?.let { cb.it() }
        columns += cb.build()
    }
}
