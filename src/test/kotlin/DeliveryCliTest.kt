import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.nio.file.Files

class DeliveryCliTest {
    @Test
    fun `restaurant and client apps share orders through local files`() {
        val dataDir = Files.createTempDirectory("delivery-cli-").toFile()

        val restaurantIo = ScriptedIo(
            listOf(
                "2",
                "Pizzaria do Bairro",
                "contato@pizzariadobairro.com",
                "Rua das Flores, 123",
                "1",
                "Pizza Calabresa",
                "45.00",
                "2",
                "Refrigerante 2L",
                "10.00",
                "",
                "2",
                "Outra Pizzaria",
                "contato@pizzariadobairro.com",
                "1",
                "contato@pizzariadobairro.com",
                "1",
                "B",
                "3",
                "Suco de Laranja",
                "8.50",
                "A",
                "0",
                "0",
                "0"
            )
        )
        RestaurantCli(dataDir, restaurantIo).start()

        val storage = FileStorage(dataDir)
        val restaurant = storage.loadRestaurant("contato@pizzariadobairro.com")
        assertEquals("Pizzaria do Bairro", restaurant?.nome)
        assertEquals(3, restaurant?.menu?.size)
        assertTrue(restaurantIo.text().contains("E-mail já cadastrado"))
        assertTrue(restaurantIo.text().contains("3 - Suco de Laranja"))

        val clientIo = ScriptedIo(
            listOf(
                "2",
                "Joao Silva",
                "62999998888",
                "Av. Central, 500",
                "2",
                "Maria",
                "62999998888",
                "1",
                "62999998888",
                "1",
                "1",
                "1",
                "2",
                "2",
                "1",
                "",
                "S",
                "2",
                "3",
                "0",
                "0"
            )
        )
        ClientCli(dataDir, clientIo).start()

        val orders = storage.loadOrdersByClient("62999998888")
        assertEquals(2, orders.size)
        assertTrue(orders.all { it.idPedido == "1" })
        assertTrue(orders.all { it.status == 0 })
        assertTrue(orders.all { it.emailRestaurante == "contato@pizzariadobairro.com" })
        assertTrue(clientIo.text().contains("Telefone já cadastrado"))
        assertTrue(clientIo.text().contains("Pedidos em Andamento"))
        assertTrue(clientIo.text().contains("Nenhum pedido encontrado"))

        val statusIo = ScriptedIo(
            listOf(
                "1",
                "contato@pizzariadobairro.com",
                "2",
                "0",
                "3",
                "1",
                "4",
                "0",
                "0"
            )
        )
        RestaurantCli(dataDir, statusIo).start()
        assertTrue(storage.loadOrders().all { it.status == 4 })
        assertTrue(statusIo.text().contains("SOLICITADO"))
        assertTrue(statusIo.text().contains("Status atualizado"))

        val finishedIo = ScriptedIo(
            listOf(
                "1",
                "62999998888",
                "2",
                "3",
                "0",
                "0"
            )
        )
        ClientCli(dataDir, finishedIo).start()
        assertTrue(finishedIo.text().contains("Pedidos Finalizados"))
        assertTrue(finishedIo.text().contains("ENTREGUE"))
    }
}
