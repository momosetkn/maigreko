package momosetkn.maigreko.dialect

import momosetkn.maigreko.introspector.H2IntrospectorBuilder
import momosetkn.maigreko.introspector.IntrospectorBuilder
import momosetkn.maigreko.sql.MigrateEngine

class H2Dialect : Dialect {
    override val name: String = "h2"
    override val migrateEngine: MigrateEngine = TODO("H2 MigrateEngine not implemented")
    override val introspectorBuilder: IntrospectorBuilder = H2IntrospectorBuilder()
}
