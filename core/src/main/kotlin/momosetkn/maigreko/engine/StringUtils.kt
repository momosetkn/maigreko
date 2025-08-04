package momosetkn.maigreko.engine

internal object StringUtils {
    fun String.normalizeText(): String {
        return this
            .collapseNewlines()
            .collapseSpaces()
            .trimEndSpacesBeforeNewlines()
            .trimEnd()
    }

    fun String.collapseSpaces(): String {
        return collapseChars(' ')
    }
    fun String.collapseNewlines(): String {
        return collapseChars('\n')
    }

    fun String.collapseChars(removeChar: Char): String {
        val sb = StringBuilder(this.length)
        var prevIsSpace = false
        for (ch in this) {
            if (ch == removeChar) {
                if (!prevIsSpace) {
                    sb.append(removeChar)
                    prevIsSpace = true
                }
            } else {
                sb.append(ch)
                prevIsSpace = false
            }
        }
        return sb.toString()
    }

    fun String.trimEndSpacesBeforeNewlines(): String {
        val input = this
        val s = buildString {
            for (line in input.lineSequence()) {
                append(line.trimEnd())
                append('\n')
            }
        }
        return s.removeSuffix("\n")
    }
}
