import java.io.File
import java.util.Locale

class FileStorage(private val dataDir: File) {
    val ordersHeader =
        "id_pedido;data_hora;email_restaurante;nome_restaurante;telefone_cliente;nome_cliente;endereco_cliente;numero_item;quantidade;descricao_item;valor_unitario;valor_total_item;status"

    init {
        dataDir.mkdirs()
    }

    fun saveRestaurant(restaurant: Restaurant) {
        val file = findRestaurantFile(restaurant.email) ?: File(dataDir, "restaurante_${nextRestaurantId()}.json")
        file.writeText(restaurant.toJson())
    }

    fun updateRestaurant(restaurant: Restaurant) = saveRestaurant(restaurant)

    fun loadRestaurant(email: String): Restaurant? =
        loadAllRestaurants().firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }

    fun loadAllRestaurants(): List<Restaurant> =
        restaurantFiles().mapNotNull { parseRestaurant(it) }.sortedBy { it.nome.lowercase() }

    fun emailExists(email: String): Boolean = loadRestaurant(email) != null

    fun saveClient(client: Client) {
        val existing = loadAllClients().toMutableList()
        if (existing.any { it.telefone == client.telefone }) {
            throw IllegalArgumentException("Telefone já cadastrado")
        }
        existing.add(client)
        writeClients(existing)
    }

    fun loadClient(telefone: String): Client? =
        loadAllClients().firstOrNull { it.telefone == telefone.trim() }

    fun loadAllClients(): List<Client> {
        val file = File(dataDir, "clientes.json")
        if (!file.exists()) return emptyList()
        val text = file.readText().trim()
        if (text.isBlank()) return emptyList()
        return parseClientsJson(text)
    }

    fun phoneExists(telefone: String): Boolean = loadClient(telefone) != null

    fun appendOrder(order: Order) {
        val orders = loadOrders().toMutableList()
        orders.add(order)
        writeOrders(orders)
    }

    fun appendOrders(newOrders: List<Order>) {
        if (newOrders.isEmpty()) return
        val orders = loadOrders().toMutableList()
        orders.addAll(newOrders)
        writeOrders(orders)
    }

    fun loadOrders(): List<Order> {
        val file = File(dataDir, "pedidos.csv")
        if (!file.exists()) return emptyList()
        val lines = file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val dataLines = if (isHeader(lines.first())) lines.drop(1) else lines
        return dataLines.mapNotNull { parseOrderLine(it) }
    }

    fun loadOrdersByRestaurant(email: String): List<Order> =
        loadOrders().filter { it.emailRestaurante.equals(email, ignoreCase = true) }

    fun loadOrdersByClient(telefone: String): List<Order> =
        loadOrders().filter { it.telefoneCliente == telefone }

    fun updateOrderStatus(idPedido: String, newStatus: Int): Boolean {
        val orders = loadOrders().toMutableList()
        var changed = false
        for (i in orders.indices) {
            if (orders[i].idPedido == idPedido) {
                orders[i] = orders[i].copy(status = newStatus)
                changed = true
            }
        }
        if (changed) writeOrders(orders)
        return changed
    }

    fun updateOrderStatusForRestaurant(idPedido: String, newStatus: Int, restaurantEmail: String): Boolean {
        val orders = loadOrders()
        val belongs = orders.any {
            it.idPedido == idPedido && it.emailRestaurante.equals(restaurantEmail, ignoreCase = true)
        }
        if (!belongs) return false
        return updateOrderStatus(idPedido, newStatus)
    }

    fun nextOrderId(): String {
        val max = loadOrders().maxOfOrNull { it.idPedido.toLongOrNull() ?: 0L } ?: 0L
        return (max + 1).toString()
    }

    fun ordersFile(): File = File(dataDir, "pedidos.csv")

    fun clientsFile(): File = File(dataDir, "clientes.json")

    private fun writeClients(clients: List<Client>) {
        clientsFile().writeText(clients.toJson())
    }

    private fun writeOrders(orders: List<Order>) {
        val body = orders.joinToString("\n") { it.toCsvLine() }
        val content = if (body.isEmpty()) ordersHeader + "\n" else ordersHeader + "\n" + body + "\n"
        ordersFile().writeText(content)
    }

    private fun parseRestaurant(file: File): Restaurant? = parseRestaurantJson(file.readText())

    private fun findRestaurantFile(email: String): File? =
        restaurantFiles().firstOrNull { parseRestaurant(it)?.email.equals(email.trim(), ignoreCase = true) }

    private fun restaurantFiles(): List<File> =
        dataDir.listFiles { file: File ->
            file.isFile && file.name.startsWith("restaurante_") && file.name.endsWith(".json")
        }?.sortedBy { it.name } ?: emptyList()

    private fun nextRestaurantId(): Int {
        val ids = restaurantFiles().mapNotNull { file ->
            Regex("""restaurante_(\d+)\.json""").matchEntire(file.name)?.groupValues?.get(1)?.toIntOrNull()
        }
        return (ids.maxOrNull() ?: 0) + 1
    }

    private fun isHeader(line: String): Boolean =
        line.equals(ordersHeader, ignoreCase = true) || line.startsWith("id_pedido;")

    private fun parseOrderLine(line: String): Order? {
        val values = line.split(";")
        if (values.size < 13) return null
        return try {
            Order(
                idPedido = values[0].trim(),
                dataHora = values[1].trim(),
                emailRestaurante = values[2].trim(),
                nomeRestaurante = values[3].trim(),
                telefoneCliente = values[4].trim(),
                nomeCliente = values[5].trim(),
                enderecoCliente = values[6].trim(),
                numeroItem = values[7].trim().toInt(),
                quantidade = values[8].trim().toInt(),
                descricaoItem = values[9].trim(),
                valorUnitario = values[10].trim().toDouble(),
                valorTotalItem = values[11].trim().toDouble(),
                status = values[12].trim().toInt()
            )
        } catch (_: NumberFormatException) {
            null
        }
    }
}

fun Order.toCsvLine(): String = listOf(
    idPedido,
    dataHora,
    emailRestaurante,
    nomeRestaurante,
    telefoneCliente,
    nomeCliente,
    enderecoCliente,
    numeroItem.toString(),
    quantidade.toString(),
    descricaoItem,
    String.format(Locale.US, "%.2f", valorUnitario),
    String.format(Locale.US, "%.2f", valorTotalItem),
    status.toString()
).joinToString(";")
