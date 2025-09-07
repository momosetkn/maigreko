package momosetkn.maigreko.dialect

import momosetkn.maigreko.introspector.IntrospectorBuilder
import momosetkn.maigreko.introspector.MariadbIntrospectorBuilder
import momosetkn.maigreko.sql.MigrateEngine

class MariadbDialect : Dialect {
    override val name: String = "mariadb"
    override val migrateEngine: MigrateEngine = TODO("MariaDB MigrateEngine not implemented")
    override val introspectorBuilder: IntrospectorBuilder = MariadbIntrospectorBuilder()
}
