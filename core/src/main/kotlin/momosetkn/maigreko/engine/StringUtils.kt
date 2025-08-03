package momosetkn.maigreko.engine

object StringUtils {
    fun String.collapseSpaces(): String {
        val sb = StringBuilder(this.length)
        var prevIsSpace = false
        for (ch in this) {
            if (ch == ' ') {
                if (!prevIsSpace) {
                    sb.append(' ')
                    prevIsSpace = true
                }
            } else {
                sb.append(ch)
                prevIsSpace = false
            }
        }
        return sb.toString()
    }
}
