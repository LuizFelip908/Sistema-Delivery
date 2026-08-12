# Sistema Delivery

Solucao de delivery em Kotlin operada via console, com duas aplicacoes independentes que se comunicam por arquivos locais.

- **App Restaurante**: cadastro com e-mail unico, gestao de cardapio e acompanhamento de pedidos.
- **App Cliente**: cadastro com telefone unico, realizacao de pedidos e consulta de status.

## Como executar

Requer JDK 21.

```bash
# App Restaurante
./gradlew run --args='restaurant'
# ou
./gradlew runRestaurant

# App Cliente
./gradlew run --args='client'
# ou
./gradlew runClient

# Testes
./gradlew test
```

As duas aplicacoes podem rodar em terminais separados. O fluxo de pedidos e sincronizado pelos arquivos da pasta `data/`.

## Persistencia

| Arquivo | Formato | Conteudo |
| --- | --- | --- |
| `data/restaurante_ID.json` | JSON | Cadastro e cardapio de cada restaurante (`restaurante_1.json`, `restaurante_2.json`, ...) |
| `data/clientes.json` | JSON | Cadastro central de clientes |
| `data/pedidos.csv` | CSV `;` | Pedidos compartilhados entre restaurante e cliente |

### Restaurante (`restaurante_1.json`)

```json
{
  "nome": "Pizzaria do Bairro",
  "email": "contato@pizzariadobairro.com",
  "endereco": "Rua das Flores, 123",
  "menu": [
    {
      "numero_item": 1,
      "descricao": "Pizza Calabresa",
      "preco": 45.00
    }
  ]
}
```

### Clientes (`clientes.json`)

```json
[
  {
    "nome": "Joao Silva",
    "telefone": "62999998888",
    "endereco": "Av. Central, 500"
  }
]
```

### Pedidos (`pedidos.csv`)

```
id_pedido;data_hora;email_restaurante;nome_restaurante;telefone_cliente;nome_cliente;endereco_cliente;numero_item;quantidade;descricao_item;valor_unitario;valor_total_item;status
```

Codigos de status:

| Codigo | Status |
| --- | --- |
| 0 | SOLICITADO |
| 1 | EM PREPARACAO |
| 2 | AGUARDANDO ENTREGADOR |
| 3 | EM TRANSITO |
| 4 | ENTREGUE |

## App Restaurante

1. Entrar com e-mail cadastrado ou criar novo cadastro (nome, e-mail, endereco e cardapio inicial).
2. Gerenciar cardapio: `[A]` ver, `[B]` adicionar, `[C]` remover.
3. Visualizar pedidos do restaurante logado, filtrados/organizados pelos status 0 a 4.
4. Alterar o status de um pedido pelo `id_pedido`, reescrevendo `pedidos.csv`.

O cadastro recusa e-mail ja existente. O loop do cardapio inicial termina com Enter vazio no `numero_item`.

## App Cliente

1. Entrar com telefone cadastrado ou criar novo cadastro (nome, telefone e endereco).
2. Realizar pedido: lista restaurantes da pasta `restaurante_ID.json`, exibe o cardapio, coleta itens ate Enter vazio e pede confirmacao `[S/N]`.
3. Ver pedidos em andamento (status 0 a 3) do telefone logado.
4. Ver pedidos finalizados (status 4) do telefone logado.

O cadastro recusa telefone ja existente. Itens do mesmo pedido compartilham o mesmo `id_pedido` e nascem com status `0`.

## Estrutura

```
src/main/kotlin/
  Main.kt            ponto de entrada das duas apps
  RestaurantCli.kt   interface do restaurante
  ClientCli.kt       interface do cliente
  FileStorage.kt     leitura/escrita de JSON e CSV
  Models.kt          restaurante, cliente, pedido e status
  JsonSupport.kt     serializacao JSON sem dependencias extras
  Io.kt              entrada/saida (console e testes)

src/test/kotlin/
  DeliveryStorageTest.kt
  DeliveryCliTest.kt
```
