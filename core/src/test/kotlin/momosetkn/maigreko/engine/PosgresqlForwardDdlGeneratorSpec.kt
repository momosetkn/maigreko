package momosetkn.maigreko.engine

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.maigreko.core.Column
import momosetkn.maigreko.core.ColumnConstraint
import momosetkn.maigreko.core.CreateTable

class PosgresqlForwardDdlGeneratorSpec : FunSpec({
    val subject = PosgresqlForwardDdlGenerator()
    context("createTable") {
        context("primaryKey = true, nullable = false") {
            test("can migrate") {
                val createTable = CreateTable(
                    tableName = "migrations",
                    columns = listOf(
                        Column(
                            name = "version",
                            type = "character varying(255)",
                            columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                        )
                    )
                )
                val ddl = subject.createTable(createTable)

                ddl shouldBe """
                    create table migrations (
                    version character varying(255) primary key
                    )
                """.trimIndent()
            }
        }
    }
})
