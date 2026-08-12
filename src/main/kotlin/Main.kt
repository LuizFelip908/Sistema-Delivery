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

data class Restaurant(
    val nome: String,
    val email: String,
    val endereco: String,
    val menu: MutableList<MenuItem> = mutableListOf()
)

data class MenuItem(
    val numeroItem: Int,
    val descricao: String,
    val preco: Double
)

data class Client(
    val nome: String,
    val telefone: String,
    val endereco: String
)

data class Order(
    val idPedido: String,
    val dataHora: String,
    val emailRestaurante: String,
    val nomeRestaurante: String,
    val telefoneCliente: String,
    val nomeCliente: String,
    val enderecoCliente: String,
    val numeroItem: Int,
    val quantidade: Int,
    val descricaoItem: String,
    val valorUnitario: Double,
    val valorTotalItem: Double,
    val status: Int
)

class FileStorage(private val dataDir: File) {
    init {
        dataDir.mkdirs()
    }

    fun saveRestaurant(restaurant: Restaurant) {
        val file = File(dataDir, "restaurante_")
        file.writeText(restaurant.toJson())
    }

    fun saveClient(client: Client) {
        val clientsFile = File(dataDir, "clientes.json")
        val existing = loadAllClients().toMutableList()
        if (existing.any { it.telefone == client.telefone }) {
            throw IllegalArgumentException("Telefone já cadastrado")
        }
        existing.add(client)
        clientsFile.writeText(existing.toJson())
    }

    fun appendOrder(order: Order) {
        val orders = loadOrders().toMutableList()
        orders.add(order)
        writeOrders(orders)
    }

    private fun loadOrders(): List<Order> {
        return emptyList()
    }

    private fun writeOrders(orders: List<Order>) {}

    fun loadAllClients(): List<Client> {
        val file = File(dataDir, "clientes.json")
        if (!file.exists()) return emptyList()
        return emptyList()
    }
}
