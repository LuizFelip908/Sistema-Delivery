import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

class DeliveryStorageTest {
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
}
