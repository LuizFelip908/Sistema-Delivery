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
    private val ordersHeader = "id_pedido;data_hora;email_restaurante;nome_restaurante;telefone_cliente;nome_cliente;endereco_cliente;numero_item;quantidade;descricao_item;valor_unitario;valor_total_item;status"

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

    fun loadOrdersByRestaurant(email: String): List<Order> {
        return loadOrders().filter { it.emailRestaurante == email }
    }

    fun loadOrdersByClient(telefone: String): List<Order> {
        return loadOrders().filter { it.telefoneCliente == telefone }
    }

    fun updateOrderStatus(idPedido: String, newStatus: Int) {
        val orders = loadOrders().toMutableList()
        val idx = orders.indexOfFirst { it.idPedido == idPedido }
        if (idx >= 0) {
            orders[idx] = orders[idx].copy(status = newStatus)
            writeOrders(orders)
        }
    }

    private fun loadOrders(): List<Order> {
        val file = File(dataDir, "pedidos.csv")
        if (!file.exists() || file.readText().isBlank()) return emptyList()
        val lines = file.readLines().filter { it.isNotBlank() }
        val dataLines = if (lines.firstOrNull() == ordersHeader) lines.drop(1) else lines
        return dataLines.map { line ->
            val values = line.split(";")
            Order(
                idPedido = values[0],
                dataHora = values[1],
                emailRestaurante = values[2],
                nomeRestaurante = values[3],
                telefoneCliente = values[4],
                nomeCliente = values[5],
                enderecoCliente = values[6],
                numeroItem = values[7].toInt(),
                quantidade = values[8].toInt(),
                descricaoItem = values[9],
                valorUnitario = values[10].toDouble(),
                valorTotalItem = values[11].toDouble(),
                status = values[12].toInt()
            )
        }
    }

    private fun writeOrders(orders: List<Order>) {
        val file = File(dataDir, "pedidos.csv")
        file.writeText(ordersHeader + "\n" + orders.joinToString(separator = "\n") { order ->
            listOf(
                order.idPedido,
                order.dataHora,
                order.emailRestaurante,
                order.nomeRestaurante,
                order.telefoneCliente,
                order.nomeCliente,
                order.enderecoCliente,
                order.numeroItem.toString(),
                order.quantidade.toString(),
                order.descricaoItem,
                order.valorUnitario.toString(),
                order.valorTotalItem.toString(),
                order.status.toString()
            ).joinToString(separator = ";")
        })
    }

    fun loadAllClients(): List<Client> {
        val file = File(dataDir, "clientes.json")
        if (!file.exists()) return emptyList()
        return file.readText().trim().let { text ->
            if (text.isBlank()) emptyList() else parseClients(text)
        }
    }
}
