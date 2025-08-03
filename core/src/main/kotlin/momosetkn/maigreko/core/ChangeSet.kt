package momosetkn.maigreko.core

data class ChangeSet(
    val filename: String,
    val author: String,
    val changeSetId: String,
    val changes: List<Change>,
)
