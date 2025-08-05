package momosetkn.maigreko.engine

class PosgresqlDdlGenerator :
    DDLGenerator,
    PosgresqlCreateTableDdlGenerator,
    PosgresqlAddColumnDdlGenerator,
    PosgresqlAddKeyDdlGenerator,
    PosgresqlRenameDdlGenerator,
    PosgresqlDropDdlGenerator,
    PosgresqlModifyDataTypeDdlGenerator,
    PosgresqlNotNullConstraintDdlGenerator
