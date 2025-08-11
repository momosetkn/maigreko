package momosetkn.maigreko.sql

class MysqlDdlGenerator :
    DDLGenerator,
    MysqlCreateTableDdlGenerator,
    MysqlAddColumnDdlGenerator,
    MysqlAddKeyDdlGenerator,
    MysqlRenameDdlGenerator,
    MysqlDropDdlGenerator,
    MysqlModifyDataTypeDdlGenerator,
    MysqlNotNullConstraintDdlGenerator,
    MysqlCreateSequenceDdlGenerator
