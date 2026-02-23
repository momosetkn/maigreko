package momosetkn.maigreko.dialect

import momosetkn.maigreko.introspector.IntrospectorBuilder
import momosetkn.maigreko.introspector.PostgresqlIntrospectorBuilder
import momosetkn.maigreko.sql.MigrateEngine
import momosetkn.maigreko.sql.PostgresqlMigrateEngine

class PostgresqlDialect : Dialect {
    override val name: String = "postgresql"
    override val migrateEngine: MigrateEngine = PostgresqlMigrateEngine()
    override val introspectorBuilder: IntrospectorBuilder = PostgresqlIntrospectorBuilder()
}
