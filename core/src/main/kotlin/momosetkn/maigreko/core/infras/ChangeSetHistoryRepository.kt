package momosetkn.maigreko.core.infras

import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.singleOrNull
import org.komapper.jdbc.JdbcDatabase

class ChangeSetHistoryRepository(
    val db: JdbcDatabase,
) {
    private val cl = Meta.changeSetHistory

    fun acquireLock() {
        db.runQuery(
            QueryDsl.from(cl)
                .forUpdate()
        )
    }

    fun fetchChangeSetHistory(changeSetId: String): ChangeSetHistory? {
        return db.runQuery(
            QueryDsl.from(cl)
                .where {
                    cl.changeSetId eq changeSetId
                }
                .singleOrNull()
        )
    }

    fun save(entity: ChangeSetHistory): ChangeSetHistory {
        return db.runQuery(
            QueryDsl.insert(cl)
                .single(entity)
        )
    }

    fun remove(entity: ChangeSetHistory) {
        return db.runQuery(
            QueryDsl.delete(cl)
                .single(entity)
        )
    }
}
