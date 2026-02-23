package momosetkn.maigreko.dialect

import momosetkn.maigreko.introspector.IntrospectorBuilder
import momosetkn.maigreko.introspector.MysqlIntrospectorBuilder
import momosetkn.maigreko.sql.MigrateEngine
import momosetkn.maigreko.sql.MysqlMigrateEngine

class MysqlDialect : Dialect {
    override val name: String = "mysql"
    override val migrateEngine: MigrateEngine =
        MysqlMigrateEngine()
    override val introspectorBuilder: IntrospectorBuilder =
        MysqlIntrospectorBuilder()
}
