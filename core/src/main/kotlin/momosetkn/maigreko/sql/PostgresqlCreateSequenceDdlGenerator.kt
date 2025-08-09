package momosetkn.maigreko.sql

import momosetkn.maigreko.change.CreateSequence

interface PostgresqlCreateSequenceDdlGenerator : DDLGenerator {
    override fun createSequence(createSequence: CreateSequence): String {
        val sb = StringBuilder("CREATE SEQUENCE ")
        sb.append(createSequence.sequenceName)
        
        createSequence.dataType?.let {
            sb.append("\n    AS ").append(it)
        }
        
        createSequence.incrementBy?.let {
            sb.append("\n    INCREMENT BY ").append(it)
        }
        
        createSequence.minValue?.let {
            sb.append("\n    MINVALUE ").append(it)
        }
        
        createSequence.maxValue?.let {
            sb.append("\n    MAXVALUE ").append(it)
        }
        
        createSequence.startValue?.let {
            sb.append("\n    START WITH ").append(it)
        }
        
        createSequence.cacheSize?.let {
            sb.append("\n    CACHE ").append(it)
        }
        
        if (createSequence.cycle) {
            sb.append("\n    CYCLE")
        } else {
            sb.append("\n    NO CYCLE")
        }
        
        sb.append(";")
        return sb.toString()
    }
    
    override fun dropSequence(createSequence: CreateSequence): String {
        return "DROP SEQUENCE IF EXISTS ${createSequence.sequenceName};"
    }
}