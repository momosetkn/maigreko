package momosetkn.maigreko.engine

import momosetkn.maigreko.core.CreateTable

interface DDLGenerator {
    fun createTable(createTable: CreateTable): String
}
