import java.io.File

fun main(args: Array<String>) {
    val mode = args.firstOrNull() ?: "restaurant"
    val dataDir = File("data")
    dataDir.mkdirs()

    when (mode) {
        "restaurant" -> println("Modo restaurante")
        "client" -> println("Modo cliente")
        else -> println("Uso: ./gradlew run --args='restaurant' ou --args='client'")
    }
}
