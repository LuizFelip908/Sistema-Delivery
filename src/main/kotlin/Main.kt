import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
        val file = File(dataDir, "restaurante_${sanitizeFileName(restaurant.email)}.json")
        file.writeText(restaurant.toJson())
    }

    fun loadRestaurant(email: String): Restaurant? {
        val files = dataDir.listFiles { f -> f.name.startsWith("restaurante_") && f.name.endsWith(".json") } ?: emptyArray()
        return files.firstOrNull { file ->
            val content = file.readText()
            content.contains("\"email\":\"$email\"")
        }?.let { parseRestaurant(it) }
    }

    fun loadAllRestaurants(): List<Restaurant> {
        val files = dataDir.listFiles { f -> f.name.startsWith("restaurante_") && f.name.endsWith(".json") } ?: emptyArray()
        return files.mapNotNull { parseRestaurant(it) }
    }

    fun updateRestaurant(restaurant: Restaurant) {
        saveRestaurant(restaurant)
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

    fun loadClient(telefone: String): Client? = loadAllClients().firstOrNull { it.telefone == telefone }

    fun loadAllClients(): List<Client> {
        val file = File(dataDir, "clientes.json")
        if (!file.exists()) return emptyList()
        return file.readText().trim().let { text ->
            if (text.isBlank()) emptyList() else parseClients(text)
        }
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
        val header = "id_pedido;data_hora;email_restaurante;nome_restaurante;telefone_cliente;nome_cliente;endereco_cliente;numero_item;quantidade;descricao_item;valor_unitario;valor_total_item;status"
        file.writeText(header + "\n" + orders.joinToString(separator = "\n") { order ->
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
        if (storage.loadRestaurant(email) != null) {
            println("E-mail já cadastrado")
            return
        }
        print("Endereco: ")
        val endereco = readLine()?.trim() ?: ""

        val menu = mutableListOf<MenuItem>()
        while (true) {
            print("Numero item (Enter para encerrar): ")
            val numeroInput = readLine()?.trim() ?: ""
            if (numeroInput.isEmpty()) break
            print("Descricao: ")
            val descricao = readLine()?.trim() ?: ""
            print("Preco: ")
            val preco = readLine()?.toDoubleOrNull() ?: 0.0
            menu.add(MenuItem(numeroInput.toInt(), descricao, preco))
        }

        val restaurant = Restaurant(nome, email, endereco, menu)
        storage.saveRestaurant(restaurant)
        println("Restaurante cadastrado com sucesso")
    }

    private fun showRestaurantMenu(restaurant: Restaurant) {
        while (true) {
            println("\nMenu Restaurante")
            println("[1] Gerenciar Cardapio")
            println("[2] Visualizar Pedidos por Status")
            println("[3] Alterar Status do Pedido")
            println("[0] Sair")
            print("Escolha: ")
            when (readLine()?.trim()) {
                "1" -> manageMenu(restaurant)
                "2" -> viewOrders(restaurant)
                "3" -> changeOrderStatus(restaurant)
                "0" -> return
                else -> println("Opcao invalida")
            }
        }
    }

    private fun manageMenu(restaurant: Restaurant) {
        while (true) {
            println("\n[1] Ver Cardapio")
            println("[2] Adicionar Item")
            println("[3] Remover Item")
            println("[0] Voltar")
            print("Escolha: ")
            when (readLine()?.trim()) {
                "1" -> println(restaurant.menu.joinToString(separator = "\n") { "${it.numeroItem} - ${it.descricao} - R$ ${it.preco}" })
                "2" -> {
                    print("Numero item: ")
                    val numero = readLine()?.trim()?.toIntOrNull() ?: 0
                    print("Descricao: ")
                    val descricao = readLine()?.trim() ?: ""
                    print("Preco: ")
                    val preco = readLine()?.toDoubleOrNull() ?: 0.0
                    val item = MenuItem(numero, descricao, preco)
                    val updated = restaurant.menu.toMutableList().apply { add(item) }
                    val updatedRestaurant = restaurant.copy(menu = updated)
                    storage.updateRestaurant(updatedRestaurant)
                    println("Item adicionado")
                }
                "3" -> {
                    print("Numero item: ")
                    val numero = readLine()?.trim()?.toIntOrNull() ?: 0
                    val updated = restaurant.menu.filterNot { it.numeroItem == numero }.toMutableList()
                    val updatedRestaurant = restaurant.copy(menu = updated)
                    storage.updateRestaurant(updatedRestaurant)
                    println("Item removido")
                }
                "0" -> return
                else -> println("Opcao invalida")
            }
        }
    }

    private fun viewOrders(restaurant: Restaurant) {
        val orders = storage.loadOrdersByRestaurant(restaurant.email)
        if (orders.isEmpty()) {
            println("Nenhum pedido encontrado")
            return
        }
        orders.forEach { order ->
            println("Pedido ${order.idPedido} | Status ${order.status} (${statusLabel(order.status)}) | ${order.descricaoItem} x${order.quantidade}")
        }
    }

    private fun changeOrderStatus(restaurant: Restaurant) {
        print("Id do pedido: ")
        val id = readLine()?.trim() ?: ""
        print("Novo status (0-4): ")
        val status = readLine()?.trim()?.toIntOrNull() ?: 0
        if (status in 0..4) {
            storage.updateOrderStatus(id, status)
            println("Status atualizado")
        } else {
            println("Status invalido")
        }
    }
}

class ClientCli(private val dataDir: File) {
    private val storage = FileStorage(dataDir)
    private var currentClient: Client? = null

    fun start() {
        while (true) {
            println("\n[1] Entrar")
            println("[2] Novo Cadastro")
            print("Escolha: ")
            when (readLine()?.trim()) {
                "1" -> loginClient()
                "2" -> registerClient()
                else -> println("Opcao invalida")
            }
        }
    }

    private fun loginClient() {
        print("Telefone: ")
        val telefone = readLine()?.trim() ?: ""
        val client = storage.loadClient(telefone)
        if (client != null) {
            currentClient = client
            println("Bem-vindo, ${client.nome}")
            showClientMenu(client)
        } else {
            println("Cliente nao encontrado")
        }
    }

    private fun registerClient() {
        print("Nome: ")
        val nome = readLine()?.trim() ?: ""
        print("Telefone: ")
        val telefone = readLine()?.trim() ?: ""
        if (storage.loadClient(telefone) != null) {
            println("Telefone já cadastrado")
            return
        }
        print("Endereco: ")
        val endereco = readLine()?.trim() ?: ""
        try {
            storage.saveClient(Client(nome, telefone, endereco))
            println("Cliente cadastrado com sucesso")
        } catch (e: IllegalArgumentException) {
            println(e.message)
        }
    }

    private fun showClientMenu(client: Client) {
        while (true) {
            println("\nMenu Cliente")
            println("[1] Realizar Novo Pedido")
            println("[2] Ver Pedidos em Andamento")
            println("[3] Ver Pedidos Finalizados")
            println("[0] Sair")
            print("Escolha: ")
            when (readLine()?.trim()) {
                "1" -> newOrder(client)
                "2" -> viewOrders(client, includeFinished = false)
                "3" -> viewOrders(client, includeFinished = true)
                "0" -> return
                else -> println("Opcao invalida")
            }
        }
    }

    private fun newOrder(client: Client) {
        val restaurants = storage.loadAllRestaurants()
        if (restaurants.isEmpty()) {
            println("Nenhum restaurante cadastrado")
            return
        }
        println("Restaurantes disponíveis:")
        restaurants.forEachIndexed { index, restaurant ->
            println("[${index + 1}] ${restaurant.nome} - ${restaurant.email}")
        }
        print("Escolha o restaurante: ")
        val selection = readLine()?.trim()?.toIntOrNull() ?: 0
        val restaurant = restaurants.getOrNull(selection - 1) ?: run {
            println("Restaurante invalido")
            return
        }

        println("Cardapio de ${restaurant.nome}:")
        restaurant.menu.forEach { item ->
            println("${item.numeroItem} - ${item.descricao} - R$ ${item.preco}")
        }

        val items = mutableListOf<Pair<Int, Int>>()
        while (true) {
            print("Numero item (Enter para encerrar): ")
            val numeroInput = readLine()?.trim() ?: ""
            if (numeroInput.isEmpty()) break
            print("Quantidade: ")
            val quantidade = readLine()?.trim()?.toIntOrNull() ?: 0
            if (quantidade > 0) {
                items.add(numeroInput.toInt() to quantidade)
            }
        }
        if (items.isEmpty()) {
            println("Pedido vazio")
            return
        }

        val total = items.sumOf { pair ->
            val item = restaurant.menu.firstOrNull { it.numeroItem == pair.first }
            item?.preco?.times(pair.second) ?: 0.0
        }
        println("Resumo do pedido: Total R$ $total")
        print("Confirmar [S/N]? ")
        if ((readLine()?.trim()?.uppercase() ?: "N") == "S") {
            items.forEach { pair ->
                val item = restaurant.menu.firstOrNull { it.numeroItem == pair.first }
                if (item != null) {
                    val order = Order(
                        idPedido = "${System.currentTimeMillis()}",
                        dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        emailRestaurante = restaurant.email,
                        nomeRestaurante = restaurant.nome,
                        telefoneCliente = client.telefone,
                        nomeCliente = client.nome,
                        enderecoCliente = client.endereco,
                        numeroItem = item.numeroItem,
                        quantidade = pair.second,
                        descricaoItem = item.descricao,
                        valorUnitario = item.preco,
                        valorTotalItem = item.preco * pair.second,
                        status = 0
                    )
                    storage.appendOrder(order)
                }
            }
            println("Pedido registrado")
        } else {
            println("Pedido cancelado")
        }
    }

    private fun viewOrders(client: Client, includeFinished: Boolean) {
        val orders = storage.loadOrdersByClient(client.telefone)
        val filtered = if (includeFinished) orders.filter { it.status == 4 } else orders.filter { it.status < 4 }
        if (filtered.isEmpty()) {
            println("Nenhum pedido encontrado")
        } else {
            filtered.forEach { order ->
                println("Pedido ${order.idPedido} | Status ${order.status} (${statusLabel(order.status)}) | ${order.descricaoItem} x${order.quantidade}")
            }
        }
    }
}

private fun statusLabel(status: Int): String = when (status) {
    0 -> "SOLICITADO"
    1 -> "EM PREPARACAO"
    2 -> "AGUARDANDO ENTREGADOR"
    3 -> "EM TRANSITO"
    4 -> "ENTREGUE"
    else -> "DESCONHECIDO"
}
