data class Restaurant(
    val nome: String,
    val email: String,
    val endereco: String,
    val menu: List<MenuItem> = emptyList()
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

enum class OrderStatus(val code: Int, val label: String) {
    SOLICITADO(0, "SOLICITADO"),
    EM_PREPARACAO(1, "EM PREPARACAO"),
    AGUARDANDO_ENTREGADOR(2, "AGUARDANDO ENTREGADOR"),
    EM_TRANSITO(3, "EM TRANSITO"),
    ENTREGUE(4, "ENTREGUE");

    companion object {
        fun from(code: Int): OrderStatus? = entries.firstOrNull { it.code == code }

        fun label(code: Int): String = from(code)?.label ?: "DESCONHECIDO"

        fun isValid(code: Int): Boolean = from(code) != null
    }
}

fun statusLabel(status: Int): String = OrderStatus.label(status)
