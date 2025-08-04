package momosetkn.maigreko.engine

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.maigreko.engine.StringUtils.collapseChars
import momosetkn.maigreko.engine.StringUtils.collapseNewlines
import momosetkn.maigreko.engine.StringUtils.collapseSpaces
import momosetkn.maigreko.engine.StringUtils.normalizeText
import momosetkn.maigreko.engine.StringUtils.trimEndSpacesBeforeNewlines

class StringUtilsSpec : FunSpec({

    context("collapseSpaces") {
        test("collapses multiple spaces into one") {
            "a  b   c".collapseSpaces() shouldBe "a b c"
            "   a    b ".collapseSpaces() shouldBe " a b "
            "".collapseSpaces() shouldBe ""
            "abc".collapseSpaces() shouldBe "abc"
            "   ".collapseSpaces() shouldBe " "
        }
    }

    context("collapseNewlines") {
        test("collapses multiple newlines into one") {
            "a\n\nb\n\n\n".collapseNewlines() shouldBe "a\nb\n"
            "\n\n\n".collapseNewlines() shouldBe "\n"
            "abc".collapseNewlines() shouldBe "abc"
            "a\nb\nc".collapseNewlines() shouldBe "a\nb\nc"
        }
    }

    context("collapseChars") {
        test("collapses specified character") {
            "aaabbbccc".collapseChars('b') shouldBe "aaabccc"
            "..a..b..".collapseChars('.') shouldBe ".a.b."
            "".collapseChars('x') shouldBe ""
        }
    }

    context("trimEndSpacesBeforeNewlines") {
        test("removes trailing spaces before newlines") {
            val input = "abc   \ndef\t\t \nghi  "
            val expected = "abc\ndef\nghi"
            input.trimEndSpacesBeforeNewlines() shouldBe expected
        }

        test("preserves internal spaces") {
            val input = " a  b \n  c d  \n"
            val expected = " a  b\n  c d\n"
            input.trimEndSpacesBeforeNewlines() shouldBe expected
        }
    }

    context("normalizeText") {
        test("normalizeText collapses spaces and newlines, trims line ends") {
            val input = "a   b   \nc    d   \n\n e   f  \n  "
            val expected = "a b\nc d\n e f"
            input.normalizeText() shouldBe expected
        }

        test("empty string") {
            "".normalizeText() shouldBe ""
        }

        test("only newlines and spaces") {
            "   \n \n".normalizeText() shouldBe ""
        }
    }
})
