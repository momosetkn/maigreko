package momosetkn.maigreko.dialect

import momosetkn.maigreko.introspector.IntrospectorBuilder
import momosetkn.maigreko.sql.MigrateEngine

interface Dialect {
    val name: String
    val migrateEngine: MigrateEngine
    val introspectorBuilder: IntrospectorBuilder
}
