package momosetkn.maigreko.sql

class MysqlMigrateEngine : MigrateEngine {
    override val ddlGenerator: DDLGenerator = MysqlDdlGenerator()
}
