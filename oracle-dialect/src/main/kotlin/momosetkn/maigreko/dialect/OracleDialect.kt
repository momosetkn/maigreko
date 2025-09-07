package momosetkn.maigreko.dialect

import momosetkn.maigreko.introspector.IntrospectorBuilder
import momosetkn.maigreko.introspector.OracleIntrospectorBuilder
import momosetkn.maigreko.sql.MigrateEngine

class OracleDialect : Dialect {
    override val name: String = "oracle"
    override val migrateEngine: MigrateEngine = TODO("Oracle MigrateEngine not implemented")
    override val introspectorBuilder: IntrospectorBuilder = OracleIntrospectorBuilder()
}
