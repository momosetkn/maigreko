package momosetkn.maigreko.sql

import momosetkn.maigreko.change.CreateSequence

interface PostgresqlCreateSequenceDdlGenerator : DDLGenerator {
    override fun createSequence(createSequence: CreateSequence): String {
        return listOfNotNull(
            "CREATE SEQUENCE ${createSequence.sequenceName}",
            createSequence.dataType?.let {
                "AS $it"
            },
            createSequence.incrementBy?.let {
                "INCREMENT BY $it"
            },
            createSequence.minValue?.let {
                "MINVALUE $it"
            },
            createSequence.maxValue?.let {
                "MAXVALUE $it"
            },
            createSequence.startValue?.let {
                "START WITH $it"
            },
            createSequence.cacheSize?.let {
                "CACHE $it"
            },
            if (createSequence.cycle) {
                "CYCLE"
            } else {
                "NO CYCLE"
            }
        ).joinToString("\n ")
    }

    override fun dropSequence(createSequence: CreateSequence): String {
        return "DROP SEQUENCE ${createSequence.sequenceName};"
    }
}