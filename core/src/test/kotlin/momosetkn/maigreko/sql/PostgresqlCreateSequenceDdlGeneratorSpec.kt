package momosetkn.maigreko.sql

import momosetkn.maigreko.change.CreateSequence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PostgresqlCreateSequenceDdlGeneratorSpec {
    private val generator = PostgresqlDdlGenerator()

    @Test
    fun `should generate CREATE SEQUENCE statement with all options`() {
        // given
        val createSequence = CreateSequence(
            sequenceName = "test_sequence",
            dataType = "bigint",
            startValue = 1L,
            minValue = 1L,
            maxValue = 1000000L,
            incrementBy = 1L,
            cycle = true,
            cacheSize = 10L
        )

        // when
        val sql = generator.createSequence(createSequence)

        // then
        val expected = """
            CREATE SEQUENCE test_sequence
                AS bigint
                INCREMENT BY 1
                MINVALUE 1
                MAXVALUE 1000000
                START WITH 1
                CACHE 10
                CYCLE;
        """.trimIndent()
        assertEquals(expected, sql)
    }

    @Test
    fun `should generate CREATE SEQUENCE statement with minimal options`() {
        // given
        val createSequence = CreateSequence(
            sequenceName = "test_sequence"
        )

        // when
        val sql = generator.createSequence(createSequence)

        // then
        val expected = """
            CREATE SEQUENCE test_sequence
                NO CYCLE;
        """.trimIndent()
        assertEquals(expected, sql)
    }

    @Test
    fun `should generate DROP SEQUENCE statement`() {
        // given
        val createSequence = CreateSequence(
            sequenceName = "test_sequence"
        )

        // when
        val sql = generator.dropSequence(createSequence)

        // then
        assertEquals("DROP SEQUENCE IF EXISTS test_sequence;", sql)
    }
}
