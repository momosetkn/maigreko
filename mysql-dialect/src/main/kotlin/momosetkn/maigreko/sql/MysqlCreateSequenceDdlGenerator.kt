package momosetkn.maigreko.sql

import momosetkn.maigreko.change.CreateSequence

interface MysqlCreateSequenceDdlGenerator : DDLGenerator {
    override fun createSequence(createSequence: CreateSequence): String {
        // MySQL doesn't have native sequence objects like PostgreSQL
        // We'll implement a similar functionality using a table
        val startValue = createSequence.startValue ?: 1
        val incrementBy = createSequence.incrementBy ?: 1

        return """
            CREATE TABLE ${createSequence.sequenceName} (
                id BIGINT NOT NULL,
                PRIMARY KEY (id)
            );
            
            INSERT INTO ${createSequence.sequenceName} VALUES ($startValue);
            
            -- To get the next value, use:
            -- UPDATE ${createSequence.sequenceName} SET id = LAST_INSERT_ID(id + $incrementBy);
            -- SELECT LAST_INSERT_ID();
            """.trimIndent()
    }

    override fun dropSequence(createSequence: CreateSequence): String {
        return "DROP TABLE ${createSequence.sequenceName};"
    }
}
