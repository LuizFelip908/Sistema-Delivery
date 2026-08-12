import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ClientCli(
    dataDir: File,
    private val io: Io = SystemIo()
) {
    private val storage = FileStorage(dataDir)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun start() {
        while (true) {
            io.println()
            io.println("========================================")
            io.println("        APP CLIENTE - DELIVERY")
            io.println("========================================")
            io.println("[1] Entrar")
            io.println("[2] Novo Cadastro")
            io.println("[0] Sair")
            when (io.ask("Escolha: ") ?: return) {
                "1" -> loginClient()
                "2" -> registerClient()
                "0" -> {
                    io.println("Encerrando App Cliente.")
                    return
                }
                else -> io.println("Opcao invalida")
            }
        }
    }

    private fun loginClient() {
        val telefone = io.askRequired("Telefone: ") ?: return
        val client = storage.loadClient(telefone)
        if (client != null) {
            io.println("Bem-vindo, ${client.nome}")
            showClientMenu(client)
        } else {
            io.println("Cliente nao encontrado")
        }
    }

    private fun registerClient() {
        val nome = io.askRequired("Nome: ") ?: return
        val telefone = io.askRequired("Telefone: ") ?: return
        if (storage.phoneExists(telefone)) {
            io.println("Telefone já cadastrado")
            return
        }
        val endereco = io.askRequired("Endereco: ") ?: return
        try {
            storage.saveClient(Client(nome, telefone, endereco))
            io.println("Cliente cadastrado com sucesso")
        } catch (e: IllegalArgumentException) {
            io.println(e.message ?: "Nao foi possivel cadastrar")
        }
    }

    private fun showClientMenu(client: Client) {
        while (true) {
            io.println()
            io.println("Menu Cliente - ${client.nome}")
            io.println("[1] Realizar Novo Pedido")
            io.println("[2] Ver Pedidos em Andamento")
            io.println("[3] Ver Pedidos Finalizados")
            io.println("[0] Sair")
            when (io.ask("Escolha: ") ?: return) {
                "1" -> newOrder(client)
                "2" -> viewOrders(client, includeFinished = false)
                "3" -> viewOrders(client, includeFinished = true)
                "0" -> return
                else -> io.println("Opcao invalida")
            }
        }
    }

    private fun newOrder(client: Client) {
        val restaurants = storage.loadAllRestaurants()
        if (restaurants.isEmpty()) {
            io.println("Nenhum restaurante cadastrado")
            return
        }

        io.println()
        io.println("Restaurantes disponiveis:")
        restaurants.forEachIndexed { index, restaurant ->
            io.println("[${index + 1}] ${restaurant.nome} - ${restaurant.email}")
        }
        val selection = io.askRequired("Escolha o restaurante: ")?.toIntOrNull() ?: 0
        val restaurant = restaurants.getOrNull(selection - 1)
        if (restaurant == null) {
            io.println("Restaurante invalido")
            return
        }
        if (restaurant.menu.isEmpty()) {
            io.println("Este restaurante ainda nao possui cardapio")
            return
        }

        io.println()
        io.println("Cardapio de ${restaurant.nome}:")
        restaurant.menu.sortedBy { it.numeroItem }.forEach { item ->
            io.println("${item.numeroItem} - ${item.descricao} - R$ ${JsonSupport.number(item.preco)}")
        }

        val selected = mutableListOf<Pair<MenuItem, Int>>()
        io.println()
        io.println("Informe os itens. Deixe o numero em branco e pressione Enter para encerrar.")
        while (true) {
            val numeroInput = io.ask("Numero item (Enter para encerrar): ") ?: break
            if (numeroInput.isEmpty()) break
            val numero = numeroInput.toIntOrNull()
            if (numero == null) {
                io.println("Numero invalido")
                continue
            }
            val item = restaurant.menu.firstOrNull { it.numeroItem == numero }
            if (item == null) {
                io.println("Item nao encontrado no cardapio")
                continue
            }
            val quantidade = io.askRequired("Quantidade: ")?.toIntOrNull() ?: 0
            if (quantidade <= 0) {
                io.println("Quantidade invalida")
                continue
            }
            selected.add(item to quantidade)
        }

        if (selected.isEmpty()) {
            io.println("Pedido vazio")
            return
        }

        val total = selected.sumOf { (item, quantidade) -> item.preco * quantidade }
        io.println()
        io.println("Resumo do pedido - ${restaurant.nome}")
        selected.forEach { (item, quantidade) ->
            io.println(
                "${item.numeroItem} - ${item.descricao} x$quantidade = R$ ${JsonSupport.number(item.preco * quantidade)}"
            )
        }
        io.println("Total R$ ${JsonSupport.number(total)}")
        val confirm = (io.ask("Confirmar [S/N]? ") ?: "N").uppercase()
        if (confirm != "S") {
            io.println("Pedido cancelado")
            return
        }

        val idPedido = storage.nextOrderId()
        val dataHora = LocalDateTime.now().format(dateFormatter)
        val orders = selected.map { (item, quantidade) ->
            Order(
                idPedido = idPedido,
                dataHora = dataHora,
                emailRestaurante = restaurant.email,
                nomeRestaurante = restaurant.nome,
                telefoneCliente = client.telefone,
                nomeCliente = client.nome,
                enderecoCliente = client.endereco,
                numeroItem = item.numeroItem,
                quantidade = quantidade,
                descricaoItem = item.descricao,
                valorUnitario = item.preco,
                valorTotalItem = item.preco * quantidade,
                status = 0
            )
        }
        storage.appendOrders(orders)
        io.println("Pedido $idPedido registrado com status 0 (SOLICITADO)")
    }

    private fun viewOrders(client: Client, includeFinished: Boolean) {
        val orders = storage.loadOrdersByClient(client.telefone)
        val filtered = if (includeFinished) {
            orders.filter { it.status == 4 }
        } else {
            orders.filter { it.status < 4 }
        }

        if (filtered.isEmpty()) {
            io.println("Nenhum pedido encontrado")
            return
        }

        val titulo = if (includeFinished) "Pedidos Finalizados" else "Pedidos em Andamento"
        io.println()
        io.println(titulo)
        filtered.groupBy { it.idPedido }.forEach { (id, lines) ->
            val first = lines.first()
            io.println(
                "Pedido $id | ${first.dataHora} | ${first.nomeRestaurante} | " +
                    "Status ${first.status} (${statusLabel(first.status)})"
            )
            lines.forEach { order ->
                io.println(
                    "  ${order.descricaoItem} x${order.quantidade} = R$ ${JsonSupport.number(order.valorTotalItem)}"
                )
            }
        }
    }
}
