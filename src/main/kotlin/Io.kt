interface Io {
    fun print(text: String)
    fun println(text: String = "")
    fun readLine(): String?

    fun ask(prompt: String): String? {
        print(prompt)
        return readLine()?.trim()
    }

    fun askRequired(prompt: String): String? {
        while (true) {
            val value = ask(prompt) ?: return null
            if (value.isNotEmpty()) return value
            println("Valor obrigatorio.")
        }
    }
}

class SystemIo : Io {
    override fun print(text: String) = kotlin.io.print(text)
    override fun println(text: String) = kotlin.io.println(text)
    override fun readLine(): String? = readlnOrNull()
}

class ScriptedIo(lines: List<String>) : Io {
    private val queue = ArrayDeque(lines)
    val output = StringBuilder()

    override fun print(text: String) {
        output.append(text)
    }

    override fun println(text: String) {
        output.append(text).append('\n')
    }

    override fun readLine(): String? = queue.removeFirstOrNull()

    fun text(): String = output.toString()
}
