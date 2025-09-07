package momosetkn.maigreko.dialect

import momosetkn.maigreko.introspector.IntrospectorBuilder
import momosetkn.maigreko.introspector.SqlServerIntrospectorBuilder
import momosetkn.maigreko.sql.MigrateEngine

class SqlServerDialect : Dialect {
    override val name: String = "sqlserver"
    override val migrateEngine: MigrateEngine = TODO("SQL Server MigrateEngine not implemented")
    override val introspectorBuilder: IntrospectorBuilder = SqlServerIntrospectorBuilder()
}
