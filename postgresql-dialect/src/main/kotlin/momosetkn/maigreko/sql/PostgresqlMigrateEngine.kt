package momosetkn.maigreko.sql

class PostgresqlMigrateEngine : MigrateEngine {
    override val ddlGenerator: DDLGenerator = PostgresqlDdlGenerator()

    override val name = "postgresql"
}
