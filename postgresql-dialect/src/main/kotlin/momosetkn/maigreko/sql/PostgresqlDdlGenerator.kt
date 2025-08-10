package momosetkn.maigreko.sql

class PostgresqlDdlGenerator :
    DDLGenerator,
    PostgresqlCreateTableDdlGenerator,
    PostgresqlAddColumnDdlGenerator,
    PostgresqlAddKeyDdlGenerator,
    PostgresqlRenameDdlGenerator,
    PostgresqlDropDdlGenerator,
    PostgresqlModifyDataTypeDdlGenerator,
    PostgresqlNotNullConstraintDdlGenerator,
    PostgresqlCreateSequenceDdlGenerator