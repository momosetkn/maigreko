package momosetkn.maigreko.sql

object MysqlMigrateEngine : MigrateEngine {
    override val ddlGenerator: DDLGenerator = MysqlDdlGenerator()
}
