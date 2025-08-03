package momosetkn.maigreko.core.infras

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperColumn
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable

@KomapperEntity
@KomapperTable(name = "change_set_history")
data class ChangeSetHistory(
    @KomapperId
    @KomapperAutoIncrement
    @KomapperColumn(name = "id")
    val id: Long = 0,

    @KomapperColumn(name = "filename")
    val filename: String,

    @KomapperColumn(name = "author")
    val author: String,

    @KomapperColumn(name = "change_set_id")
    val changeSetId: String,

    @KomapperColumn(name = "tag")
    val tag: String? = null,

    @KomapperColumn(name = "applied_at")
    val appliedAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
)
