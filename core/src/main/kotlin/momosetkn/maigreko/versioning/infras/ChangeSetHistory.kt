package momosetkn.maigreko.versioning.infras

import java.time.LocalDateTime

data class ChangeSetHistory(
    val id: Long = 0,
    val filename: String,
    val author: String,
    val changeSetId: String,
    val tag: String? = null,
    val appliedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        const val TABLE_NAME = "change_set_history"
        const val ID_COLUMN = "id"
        const val FILENAME_COLUMN = "filename"
        const val AUTHOR_COLUMN = "author"
        const val CHANGE_SET_ID_COLUMN = "change_set_id"
        const val TAG_COLUMN = "tag"
        const val APPLIED_AT_COLUMN = "applied_at"
    }
}
