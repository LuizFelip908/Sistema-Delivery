import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.io.File
import java.nio.file.Files

class DeliveryStorageTest {
    private fun tempDir(): File {
        val dir = Files.createTempDirectory("delivery-test-").toFile()
        dir.deleteOnExit()
        return dir
    }

    @Test
    fun `should manage restaurant and client data`() {
        val dataDir = File("build/test-data")
        dataDir.mkdirs()
        dataDir.listFiles()?.forEach { it.delete() }

        val storage = FileStorage(dataDir)

        val restaurant = Restaurant("Pizzaria", "pizzaria@test.com", "Rua A", listOf(MenuItem(1, "Pizza", 40.0)))
        storage.saveRestaurant(restaurant)
        val loadedRestaurant = storage.loadRestaurant("pizzaria@test.com")
        assertEquals(restaurant.email, loadedRestaurant?.email)

        val client = Client("Joao", "62999998888", "Av. B")
        storage.saveClient(client)
        val loadedClient = storage.loadClient("62999998888")
        assertEquals(client.telefone, loadedClient?.telefone)

        val order = Order(
            idPedido = "1",
            dataHora = "2026-08-10 10:00",
            emailRestaurante = restaurant.email,
            nomeRestaurante = restaurant.nome,
            telefoneCliente = client.telefone,
            nomeCliente = client.nome,
            enderecoCliente = client.endereco,
            numeroItem = 1,
            quantidade = 2,
            descricaoItem = "Pizza",
            valorUnitario = 40.0,
            valorTotalItem = 80.0,
            status = 0
        )
        storage.appendOrder(order)
        val orders = storage.loadOrdersByRestaurant(restaurant.email)
        assertTrue(orders.isNotEmpty())
    }

    @Test
    fun `restaurant files use numeric ids and unique email`() {
        val dir = tempDir()
        val storage = FileStorage(dir)
        storage.saveRestaurant(Restaurant("Pizzaria do Bairro", "contato@pizzariadobairro.com", "Rua das Flores, 123"))
        storage.saveRestaurant(Restaurant("Burger House", "contato@burger.com", "Av. 1"))

        val files = dir.listFiles { f: File -> f.name.endsWith(".json") }!!.map { it.name }.sorted()
        assertEquals(listOf("restaurante_1.json", "restaurante_2.json"), files)
        assertNotNull(storage.loadRestaurant("CONTATO@pizzariadobairro.com"))
        assertTrue(storage.emailExists("contato@burger.com"))
        assertNull(storage.loadRestaurant("naoexiste@test.com"))
    }

    @Test
    fun `restaurant json matches specification fields`() {
        val dir = tempDir()
        val storage = FileStorage(dir)
        storage.saveRestaurant(
            Restaurant(
                nome = "Pizzaria do Bairro",
                email = "contato@pizzariadobairro.com",
                endereco = "Rua das Flores, 123",
                menu = listOf(
                    MenuItem(1, "Pizza Calabresa", 45.00),
                    MenuItem(2, "Refrigerante 2L", 10.00)
                )
            )
        )

        val json = File(dir, "restaurante_1.json").readText()
        assertTrue(json.contains("\"nome\""))
        assertTrue(json.contains("\"email\""))
        assertTrue(json.contains("\"endereco\""))
        assertTrue(json.contains("\"menu\""))
        assertTrue(json.contains("\"numero_item\""))
        assertTrue(json.contains("\"descricao\""))
        assertTrue(json.contains("\"preco\""))

        val loaded = storage.loadRestaurant("contato@pizzariadobairro.com")
        assertEquals("Pizzaria do Bairro", loaded?.nome)
        assertEquals(2, loaded?.menu?.size)
        assertEquals("Pizza Calabresa", loaded?.menu?.first()?.descricao)
        assertEquals(45.00, loaded?.menu?.first()?.preco)
    }

    @Test
    fun `parses pretty printed restaurant json from specification`() {
        val dir = tempDir()
        File(dir, "restaurante_9.json").writeText(
            """
            {
              "nome": "Pizzaria do Bairro",
              "email": "contato@pizzariadobairro.com",
              "endereco": "Rua das Flores, 123",
              "menu": [
                {
                  "numero_item": 1,
                  "descricao": "Pizza Calabresa",
                  "preco": 45.00
                },
                {
                  "numero_item": 2,
                  "descricao": "Refrigerante 2L",
                  "preco": 10.00
                }
              ]
            }
            """.trimIndent()
        )
        val loaded = FileStorage(dir).loadRestaurant("contato@pizzariadobairro.com")
        assertNotNull(loaded)
        assertEquals(2, loaded.menu.size)
        assertEquals(10.00, loaded.menu[1].preco)
    }

    @Test
    fun `menu add and remove persist in the same restaurant file`() {
        val dir = tempDir()
        val storage = FileStorage(dir)
        storage.saveRestaurant(Restaurant("Lanchonete", "lanche@test.com", "Rua B", listOf(MenuItem(1, "X-Burguer", 20.0))))
        val loaded = storage.loadRestaurant("lanche@test.com")!!
        storage.updateRestaurant(loaded.copy(menu = loaded.menu + MenuItem(2, "Suco", 8.0)))
        storage.updateRestaurant(storage.loadRestaurant("lanche@test.com")!!.let { current ->
            current.copy(menu = current.menu.filterNot { it.numeroItem == 1 })
        })

        val finalRestaurant = storage.loadRestaurant("lanche@test.com")!!
        assertEquals(1, finalRestaurant.menu.size)
        assertEquals("Suco", finalRestaurant.menu.first().descricao)
        assertEquals(listOf("restaurante_1.json"), dir.listFiles()!!.map { it.name })
    }

    @Test
    fun `client phone must be unique`() {
        val storage = FileStorage(tempDir())
        storage.saveClient(Client("Joao Silva", "62999998888", "Av. Central, 500"))
        assertFailsWith<IllegalArgumentException> {
            storage.saveClient(Client("Outro", "62999998888", "Outra rua"))
        }
        assertEquals(1, storage.loadAllClients().size)
    }

    @Test
    fun `orders are filtered by restaurant email and client phone`() {
        val storage = FileStorage(tempDir())
        storage.appendOrder(sampleOrder("1", "a@rest.com", "111", 0))
        storage.appendOrder(sampleOrder("2", "b@rest.com", "111", 1))
        storage.appendOrder(sampleOrder("3", "a@rest.com", "222", 4))

        assertEquals(listOf("1", "3"), storage.loadOrdersByRestaurant("a@rest.com").map { it.idPedido })
        assertEquals(listOf("1", "2"), storage.loadOrdersByClient("111").map { it.idPedido })
        assertEquals(1, storage.loadOrdersByClient("111").count { it.status < 4 && it.idPedido == "1" })
        assertEquals(listOf("3"), storage.loadOrdersByClient("222").filter { it.status == 4 }.map { it.idPedido })
    }

    @Test
    fun `status update rewrites every line of the same order id`() {
        val storage = FileStorage(tempDir())
        storage.appendOrder(sampleOrder("10", "a@rest.com", "111", 0, item = 1))
        storage.appendOrder(sampleOrder("10", "a@rest.com", "111", 0, item = 2))
        storage.appendOrder(sampleOrder("11", "a@rest.com", "111", 0, item = 1))

        assertTrue(storage.updateOrderStatus("10", 3))
        val updated = storage.loadOrders().filter { it.idPedido == "10" }
        assertTrue(updated.all { it.status == 3 })
        assertEquals(0, storage.loadOrders().first { it.idPedido == "11" }.status)
        assertFalse(storage.updateOrderStatusForRestaurant("10", 4, "outro@rest.com"))
        assertTrue(storage.updateOrderStatusForRestaurant("10", 4, "a@rest.com"))
        assertTrue(storage.loadOrders().filter { it.idPedido == "10" }.all { it.status == 4 })
    }

    @Test
    fun `csv uses specification header and semicolon separator`() {
        val dir = tempDir()
        val storage = FileStorage(dir)
        storage.appendOrder(sampleOrder("1", "a@rest.com", "111", 0))
        val csv = File(dir, "pedidos.csv").readText()
        val header =
            "id_pedido;data_hora;email_restaurante;nome_restaurante;telefone_cliente;nome_cliente;endereco_cliente;numero_item;quantidade;descricao_item;valor_unitario;valor_total_item;status"
        assertTrue(csv.startsWith(header))
        assertTrue(csv.contains("a@rest.com;Restaurante;111;Cliente"))
        assertEquals("2", storage.nextOrderId())
    }

    private fun sampleOrder(
        id: String,
        email: String,
        phone: String,
        status: Int,
        item: Int = 1
    ): Order = Order(
        idPedido = id,
        dataHora = "2026-08-12 12:00:00",
        emailRestaurante = email,
        nomeRestaurante = "Restaurante",
        telefoneCliente = phone,
        nomeCliente = "Cliente",
        enderecoCliente = "Rua X",
        numeroItem = item,
        quantidade = 1,
        descricaoItem = "Item $item",
        valorUnitario = 10.0,
        valorTotalItem = 10.0,
        status = status
    )
}
