package momosetkn.maigreko.migrations

import momosetkn.maigreko.dsl.MaigrekoMigration

@Suppress("unused", "ClassNaming")
class Migration1 : MaigrekoMigration({
    changeSet {
        createTable(this::class.simpleName!!) {
            column("id", "bigint") {
                constraint(primaryKey = true, nullable = false)
            }
            column("name", "varchar(255)") { }
        }
    }
})
