import java.util.Locale

object JsonSupport {
    fun quote(value: String): String {
        val escaped = buildString(value.length + 8) {
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
        return "\"$escaped\""
    }

    fun number(value: Double): String = String.format(Locale.US, "%.2f", value)

    fun string(json: String, key: String): String? {
        val regex = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        return regex.find(json)?.groupValues?.get(1)?.let { unescape(it) }
    }

    fun int(json: String, key: String): Int? = numberToken(json, key)?.toIntOrNull()

    fun double(json: String, key: String): Double? = numberToken(json, key)?.toDoubleOrNull()

    fun arrayObjects(json: String, key: String): List<String> {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return emptyList()
        val bracket = json.indexOf('[', keyIndex + keyToken.length)
        if (bracket < 0) return emptyList()
        val end = matchingBracket(json, bracket, '[', ']')
        if (end < 0) return emptyList()
        return splitObjects(json.substring(bracket + 1, end))
    }

    fun topLevelArrayObjects(json: String): List<String> {
        val start = json.indexOf('[')
        val end = json.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return splitObjects(json.substring(start + 1, end))
    }

    private fun numberToken(json: String, key: String): String? {
        val regex = Regex("\"${Regex.escape(key)}\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun unescape(value: String): String = buildString(value.length) {
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                when (val next = value[i + 1]) {
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    '"', '\\' -> append(next)
                    else -> append(next)
                }
                i += 2
            } else {
                append(ch)
                i++
            }
        }
    }

    private fun matchingBracket(text: String, openIndex: Int, open: Char, close: Char): Int {
        var depth = 0
        var inString = false
        var escape = false
        for (i in openIndex until text.length) {
            val ch = text[i]
            if (inString) {
                if (escape) {
                    escape = false
                } else if (ch == '\\') {
                    escape = true
                } else if (ch == '"') {
                    inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun splitObjects(body: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escape = false
        for (i in body.indices) {
            val ch = body[i]
            if (inString) {
                if (escape) {
                    escape = false
                } else if (ch == '\\') {
                    escape = true
                } else if (ch == '"') {
                    inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        result += body.substring(start, i + 1)
                        start = -1
                    }
                }
            }
        }
        return result
    }
}

fun Restaurant.toJson(): String {
    val items = menu.joinToString(",\n") { item ->
        """    {
      "numero_item": ${item.numeroItem},
      "descricao": ${JsonSupport.quote(item.descricao)},
      "preco": ${JsonSupport.number(item.preco)}
    }"""
    }
    val menuBlock = if (menu.isEmpty()) "[]" else "[\n$items\n  ]"
    return """{
  "nome": ${JsonSupport.quote(nome)},
  "email": ${JsonSupport.quote(email)},
  "endereco": ${JsonSupport.quote(endereco)},
  "menu": $menuBlock
}
"""
}

fun List<Client>.toJson(): String {
    if (isEmpty()) return "[]\n"
    val body = joinToString(",\n") { client ->
        """  {
    "nome": ${JsonSupport.quote(client.nome)},
    "telefone": ${JsonSupport.quote(client.telefone)},
    "endereco": ${JsonSupport.quote(client.endereco)}
  }"""
    }
    return "[\n$body\n]\n"
}

fun parseRestaurantJson(text: String): Restaurant? {
    val email = JsonSupport.string(text, "email") ?: return null
    val nome = JsonSupport.string(text, "nome") ?: ""
    val endereco = JsonSupport.string(text, "endereco") ?: ""
    val menu = JsonSupport.arrayObjects(text, "menu").mapNotNull { item ->
        val numero = JsonSupport.int(item, "numero_item") ?: return@mapNotNull null
        val descricao = JsonSupport.string(item, "descricao") ?: ""
        val preco = JsonSupport.double(item, "preco") ?: 0.0
        MenuItem(numero, descricao, preco)
    }
    return Restaurant(nome, email, endereco, menu)
}

fun parseClientsJson(text: String): List<Client> {
    if (text.isBlank()) return emptyList()
    return JsonSupport.topLevelArrayObjects(text).mapNotNull { obj ->
        val nome = JsonSupport.string(obj, "nome") ?: return@mapNotNull null
        val telefone = JsonSupport.string(obj, "telefone") ?: return@mapNotNull null
        val endereco = JsonSupport.string(obj, "endereco") ?: ""
        Client(nome, telefone, endereco)
    }
}
