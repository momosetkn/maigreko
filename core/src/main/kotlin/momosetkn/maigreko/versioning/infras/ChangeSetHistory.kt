package momosetkn.maigreko.versioning.infras

import momosetkn.maigreko.versioning.Versioning
import java.time.LocalDateTime

data class ChangeSetHistory(
    val id: Long = 0,
    val migrationClass: String,
    val changeSetId: Int,
    val tag: String? = null,
    val appliedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        const val TABLE_NAME = Versioning.VERSIONING_TABLE_NAME
        const val ID_COLUMN = "id"
        const val MIGRATION_CLASS_COLUMN = "migration_class"
        const val CHANGE_SET_ID_COLUMN = "change_set_id"
        const val TAG_COLUMN = "tag"
        const val APPLIED_AT_COLUMN = "applied_at"
    }
}
