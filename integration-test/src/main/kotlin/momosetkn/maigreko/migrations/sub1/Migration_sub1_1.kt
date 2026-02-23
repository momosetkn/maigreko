package momosetkn.maigreko.migrations.sub1

import momosetkn.maigreko.dsl.MaigrekoMigration

@Suppress("unused", "ClassNaming")
class Migration_sub1_1 : MaigrekoMigration({
    changeSet {
        createTable(this::class.simpleName!!) {
            column("id", "bigint") {
                constraint(primaryKey = true, nullable = false)
            }
            column("name", "varchar(255)") { }
        }
    }
})
