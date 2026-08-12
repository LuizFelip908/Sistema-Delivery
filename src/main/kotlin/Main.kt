import java.io.File

fun main(args: Array<String>) {
    val mode = args.firstOrNull() ?: "restaurant"
    val dataDir = File("data")
    dataDir.mkdirs()

    when (mode) {
        "restaurant" -> RestaurantCli(dataDir).start()
        "client" -> ClientCli(dataDir).start()
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

    private fun parseRestaurant(file: File): Restaurant? {
        val text = file.readText()
        if (!text.contains("\"menu\"") || !text.contains("\"email\"")) return null
        val email = Regex("\"email\":\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1) ?: ""
        val nome = Regex("\"nome\":\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1) ?: ""
        val endereco = Regex("\"endereco\":\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1) ?: ""
        val menuItems = Regex("\"numero_item\":\s*(\\d+),\s*\"descricao\":\s*\"([^\"]+)\",\s*\"preco\":\s*(\\d+(?:\\.\\d+)?)")
            .findAll(text)
            .map {
                MenuItem(
                    numeroItem = it.groupValues[1].toInt(),
                    descricao = it.groupValues[2],
                    preco = it.groupValues[3].toDouble()
                )
            }.toList()
        return Restaurant(nome, email, endereco, menuItems.toMutableList())
    }

    private fun parseClients(text: String): List<Client> {
        val clientes = mutableListOf<Client>()
        val regexNome = Regex("\"nome\":\s*\"([^\"]+)\"")
        val regexTelefone = Regex("\"telefone\":\s*\"([^\"]+)\"")
        val regexEndereco = Regex("\"endereco\":\s*\"([^\"]+)\"")
        val names = regexNome.findAll(text).map { it.groupValues[1] }.toList()
        val phones = regexTelefone.findAll(text).map { it.groupValues[1] }.toList()
        val addresses = regexEndereco.findAll(text).map { it.groupValues[1] }.toList()
        for (i in names.indices) {
            clientes.add(Client(names[i], phones[i], addresses[i]))
        }
        return clientes
    }

    private fun sanitizeFileName(value: String) = value.replace("@", "_").replace(".", "_")
}

private fun Restaurant.toJson(): String {
    val items = menu.joinToString(separator = ",") { item ->
        "{\"numero_item\":${item.numeroItem},\"descricao\":\"${item.descricao}\",\"preco\":${item.preco}}"
    }
    return "{\"nome\":\"$nome\",\"email\":\"$email\",\"endereco\":\"$endereco\",\"menu\":[${items}] }"
}

private fun List<Client>.toJson(): String {
    return joinToString(prefix = "[", postfix = "]") { client ->
        "{\"nome\":\"${client.nome}\",\"telefone\":\"${client.telefone}\",\"endereco\":\"${client.endereco}\"}"
    }
}

class RestaurantCli(private val dataDir: File) {
    private val storage = FileStorage(dataDir)

    fun start() {
        while (true) {
            println("\n[1] Entrar como Restaurante Existente")
            println("[2] Novo Cadastro")
            print("Escolha: ")
            when (readLine()?.trim()) {
                "1" -> loginRestaurant()
                "2" -> registerRestaurant()
                else -> println("Opcao invalida")
            }
        }
    }

    private fun loginRestaurant() {
        print("E-mail: ")
        val email = readLine()?.trim() ?: ""
        val restaurant = storage.loadRestaurant(email)
        if (restaurant != null) {
            println("Bem-vindo, ${restaurant.nome}")
            showRestaurantMenu(restaurant)
        } else {
            println("Restaurante nao encontrado")
        }
    }

    private fun registerRestaurant() {
        print("Nome: ")
        val nome = readLine()?.trim() ?: ""
        print("E-mail: ")
        val email = readLine()?.trim() ?: ""
        print("Endereco: ")
        val endereco = readLine()?.trim() ?: ""
        val restaurant = Restaurant(nome, email, endereco)
        storage.saveRestaurant(restaurant)
        println("Restaurante cadastrado com sucesso")
    }

    private fun showRestaurantMenu(restaurant: Restaurant) {
        while (true) {
            println("\n[1] Ver cardápio")
            println("[2] Adicionar item")
            println("[3] Remover item")
            println("[4] Ver pedidos")
            println("[5] Sair")
            print("Escolha: ")
            when (readLine()?.trim()) {
                "1" -> restaurant.menu.forEach { println("${it.numeroItem} - ${it.descricao} R$${it.preco}") }
                "2" -> addMenuItem(restaurant)
                "3" -> removeMenuItem(restaurant)
                "4" -> showOrders(restaurant)
                "5" -> return
                else -> println("Opcao invalida")
            }
        }
    }

    private fun addMenuItem(restaurant: Restaurant) {
        print("Numero do item: ")
        val numero = readLine()?.toIntOrNull() ?: return
        print("Descricao: ")
        val descricao = readLine()?.trim() ?: ""
        print("Preco: ")
        val preco = readLine()?.toDoubleOrNull() ?: return
        restaurant.menu.add(MenuItem(numero, descricao, preco))
        storage.updateRestaurant(restaurant)
        println("Item adicionado")
    }

    private fun removeMenuItem(restaurant: Restaurant) {
        print("Numero do item para remover: ")
        val numero = readLine()?.toIntOrNull() ?: return
        restaurant.menu.removeIf { it.numeroItem == numero }
        storage.updateRestaurant(restaurant)
        println("Item removido")
    }

    private fun showOrders(restaurant: Restaurant) {
        val orders = storage.loadOrdersByRestaurant(restaurant.email)
        if (orders.isEmpty()) {
            println("Nenhum pedido encontrado")
            return
        }
        orders.forEach { println("${it.idPedido}: ${it.descricaoItem} x${it.quantidade} - status ${it.status}") }
        print("Id do pedido para atualizar status ou ENTER para voltar: ")
        val option = readLine()?.trim()
        if (!option.isNullOrBlank()) {
            storage.updateOrderStatus(option, 1)
            println("Status atualizado")
        }
    }
}
