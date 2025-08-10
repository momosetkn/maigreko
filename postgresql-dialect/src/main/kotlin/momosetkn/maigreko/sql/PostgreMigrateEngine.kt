package momosetkn.maigreko.sql

object PostgreMigrateEngine : MigrateEngine {
    override val ddlGenerator: DDLGenerator = PostgresqlDdlGenerator()
}