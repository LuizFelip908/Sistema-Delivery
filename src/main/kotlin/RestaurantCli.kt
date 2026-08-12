import java.io.File

class RestaurantCli(
    dataDir: File,
    private val io: Io = SystemIo()
) {
    private val storage = FileStorage(dataDir)

    fun start() {
        while (true) {
            io.println()
            io.println("========================================")
            io.println("     APP RESTAURANTE - DELIVERY")
            io.println("========================================")
            io.println("[1] Entrar como Restaurante Existente")
            io.println("[2] Novo Cadastro")
            io.println("[0] Sair")
            when (io.ask("Escolha: ") ?: return) {
                "1" -> loginRestaurant()
                "2" -> registerRestaurant()
                "0" -> {
                    io.println("Encerrando App Restaurante.")
                    return
                }
                else -> io.println("Opcao invalida")
            }
        }
    }

    private fun loginRestaurant() {
        val email = io.askRequired("E-mail: ") ?: return
        val restaurant = storage.loadRestaurant(email)
        if (restaurant != null) {
            io.println("Bem-vindo, ${restaurant.nome}")
            showRestaurantMenu(restaurant)
        } else {
            io.println("Restaurante nao encontrado")
        }
    }

    private fun registerRestaurant() {
        val nome = io.askRequired("Nome: ") ?: return
        val email = io.askRequired("E-mail: ") ?: return
        if (storage.emailExists(email)) {
            io.println("E-mail já cadastrado")
            return
        }
        val endereco = io.askRequired("Endereco: ") ?: return

        io.println()
        io.println("--- Cadastro inicial do cardapio ---")
        io.println("Deixe o numero do item em branco e pressione Enter para encerrar.")
        val menu = collectMenuItems()

        val restaurant = Restaurant(nome, email, endereco, menu)
        storage.saveRestaurant(restaurant)
        io.println("Restaurante cadastrado com sucesso")
    }

    private fun collectMenuItems(): MutableList<MenuItem> {
        val menu = mutableListOf<MenuItem>()
        while (true) {
            val numeroInput = io.ask("Numero item (Enter para encerrar): ") ?: break
            if (numeroInput.isEmpty()) break
            val numero = numeroInput.toIntOrNull()
            if (numero == null) {
                io.println("Numero invalido")
                continue
            }
            if (menu.any { it.numeroItem == numero }) {
                io.println("Ja existe um item com esse numero")
                continue
            }
            val descricao = io.askRequired("Descricao: ") ?: break
            val preco = readPrice() ?: continue
            menu.add(MenuItem(numero, descricao, preco))
            io.println("Item ${numero} adicionado")
        }
        return menu
    }

    private fun showRestaurantMenu(initial: Restaurant) {
        var restaurant = initial
        while (true) {
            io.println()
            io.println("Menu Restaurante - ${restaurant.nome}")
            io.println("[1] Gerenciar Cardapio")
            io.println("[2] Visualizar Pedidos por Status")
            io.println("[3] Alterar Status do Pedido")
            io.println("[0] Sair")
            when (io.ask("Escolha: ") ?: return) {
                "1" -> restaurant = manageMenu(restaurant)
                "2" -> viewOrders(restaurant)
                "3" -> changeOrderStatus(restaurant)
                "0" -> return
                else -> io.println("Opcao invalida")
            }
        }
    }

    private fun manageMenu(initial: Restaurant): Restaurant {
        var restaurant = initial
        while (true) {
            io.println()
            io.println("Gerenciar Cardapio")
            io.println("[A] Ver Cardapio")
            io.println("[B] Adicionar Item")
            io.println("[C] Remover Item")
            io.println("[0] Voltar")
            when ((io.ask("Escolha: ") ?: return restaurant).uppercase()) {
                "A", "1" -> showMenu(restaurant)
                "B", "2" -> restaurant = addMenuItem(restaurant)
                "C", "3" -> restaurant = removeMenuItem(restaurant)
                "0" -> return restaurant
                else -> io.println("Opcao invalida")
            }
        }
    }

    private fun showMenu(restaurant: Restaurant) {
        if (restaurant.menu.isEmpty()) {
            io.println("Cardapio vazio")
            return
        }
        io.println()
        io.println("Cardapio de ${restaurant.nome}")
        restaurant.menu.sortedBy { it.numeroItem }.forEach { item ->
            io.println("${item.numeroItem} - ${item.descricao} - R$ ${JsonSupport.number(item.preco)}")
        }
    }

    private fun addMenuItem(restaurant: Restaurant): Restaurant {
        val numero = io.askRequired("Numero item: ")?.toIntOrNull()
        if (numero == null) {
            io.println("Numero invalido")
            return restaurant
        }
        if (restaurant.menu.any { it.numeroItem == numero }) {
            io.println("Ja existe um item com esse numero")
            return restaurant
        }
        val descricao = io.askRequired("Descricao: ") ?: return restaurant
        val preco = readPrice() ?: return restaurant
        val updated = restaurant.copy(menu = restaurant.menu + MenuItem(numero, descricao, preco))
        storage.updateRestaurant(updated)
        io.println("Item adicionado")
        return updated
    }

    private fun removeMenuItem(restaurant: Restaurant): Restaurant {
        val numero = io.askRequired("Numero item: ")?.toIntOrNull()
        if (numero == null) {
            io.println("Numero invalido")
            return restaurant
        }
        if (restaurant.menu.none { it.numeroItem == numero }) {
            io.println("Item nao encontrado")
            return restaurant
        }
        val updated = restaurant.copy(menu = restaurant.menu.filterNot { it.numeroItem == numero })
        storage.updateRestaurant(updated)
        io.println("Item removido")
        return updated
    }

    private fun viewOrders(restaurant: Restaurant) {
        val orders = storage.loadOrdersByRestaurant(restaurant.email)
        if (orders.isEmpty()) {
            io.println("Nenhum pedido encontrado")
            return
        }

        io.println()
        io.println("Codigos de status:")
        OrderStatus.entries.forEach { io.println("  ${it.code} - ${it.label}") }
        val filterInput = io.ask("Filtrar por status (0-4) ou Enter para todos: ") ?: return
        val filtered = if (filterInput.isEmpty()) {
            orders
        } else {
            val status = filterInput.toIntOrNull()
            if (status == null || !OrderStatus.isValid(status)) {
                io.println("Status invalido")
                return
            }
            orders.filter { it.status == status }
        }

        if (filtered.isEmpty()) {
            io.println("Nenhum pedido encontrado para o status informado")
            return
        }

        filtered.groupBy { it.status }.toSortedMap().forEach { (status, items) ->
            io.println()
            io.println("===== ${status} - ${statusLabel(status)} =====")
            items.groupBy { it.idPedido }.forEach { (id, lines) ->
                io.println("Pedido $id | ${lines.first().dataHora} | Cliente: ${lines.first().nomeCliente} (${lines.first().telefoneCliente})")
                lines.forEach { order ->
                    io.println(
                        "  Item ${order.numeroItem} - ${order.descricaoItem} x${order.quantidade} " +
                            "= R$ ${JsonSupport.number(order.valorTotalItem)}"
                    )
                }
            }
        }
    }

    private fun changeOrderStatus(restaurant: Restaurant) {
        val restaurantOrders = storage.loadOrdersByRestaurant(restaurant.email)
        if (restaurantOrders.isEmpty()) {
            io.println("Nenhum pedido encontrado")
            return
        }

        io.println("Pedidos do restaurante:")
        restaurantOrders.groupBy { it.idPedido }.forEach { (id, lines) ->
            io.println("  $id | status ${lines.first().status} (${statusLabel(lines.first().status)}) | ${lines.first().nomeCliente}")
        }

        val id = io.askRequired("Id do pedido: ") ?: return
        io.println("Novo status (0-4):")
        OrderStatus.entries.forEach { io.println("  ${it.code} - ${it.label}") }
        val status = io.askRequired("Status: ")?.toIntOrNull()
        if (status == null || !OrderStatus.isValid(status)) {
            io.println("Status invalido")
            return
        }

        val updated = storage.updateOrderStatusForRestaurant(id, status, restaurant.email)
        if (updated) {
            io.println("Status atualizado")
        } else {
            io.println("Pedido nao encontrado para este restaurante")
        }
    }

    private fun readPrice(): Double? {
        val raw = io.askRequired("Preco: ") ?: return null
        val preco = raw.replace(",", ".").toDoubleOrNull()
        if (preco == null || preco < 0) {
            io.println("Preco invalido")
            return null
        }
        return preco
    }
}
