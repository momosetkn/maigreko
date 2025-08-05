package momosetkn.maigreko.engine

class PostgresqlDdlGenerator :
    DDLGenerator,
    PostgresqlCreateTableDdlGenerator,
    PostgresqlAddColumnDdlGenerator,
    PostgresqlAddKeyDdlGenerator,
    PostgresqlRenameDdlGenerator,
    PostgresqlDropDdlGenerator,
    PostgresqlModifyDataTypeDdlGenerator,
    PostgresqlNotNullConstraintDdlGenerator
