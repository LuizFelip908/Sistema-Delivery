import java.io.File

fun main(args: Array<String>) {
    val mode = args.firstOrNull()?.lowercase() ?: "help"
    val dataDir = File("data")
    dataDir.mkdirs()

    when (mode) {
        "restaurant", "restaurante" -> RestaurantCli(dataDir).start()
        "client", "cliente" -> ClientCli(dataDir).start()
        else -> {
            println("Sistema de Delivery - Kotlin CLI")
            println()
            println("Uso:")
            println("  ./gradlew run --args='restaurant'")
            println("  ./gradlew run --args='client'")
            println()
            println("Os dados sao gravados na pasta data/:")
            println("  restaurante_ID.json  cadastro e cardapio de cada restaurante")
            println("  clientes.json        cadastro central de clientes")
            println("  pedidos.csv          pedidos compartilhados entre as duas apps")
        }
    }
}
