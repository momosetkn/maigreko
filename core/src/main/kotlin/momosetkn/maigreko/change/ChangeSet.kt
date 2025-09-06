package momosetkn.maigreko.change

data class ChangeSet(
    val migrationClass: String,
    val changeSetId: Int,
    val changes: List<Change>,
)
