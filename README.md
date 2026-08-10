# Sistema Delivery

Este projeto é uma solução de delivery operada via console em Kotlin, com duas aplicações independentes:

- App Restaurante: cadastro, autenticação, gestão de cardápio e acompanhamento de pedidos.
- App Cliente: cadastro, autenticação, realização de pedidos e consulta do status.

## Funcionalidades principais

### Aplicação Restaurante
- Entrar com e-mail cadastrado ou criar novo cadastro.
- Gerenciar o cardápio com opções para visualizar, adicionar e remover itens.
- Visualizar pedidos filtrados por status.
- Alterar o status de um pedido no arquivo CSV.

### Aplicação Cliente
- Entrar com telefone cadastrado ou criar novo cadastro.
- Listar restaurantes cadastrados e consultar seus cardápios.
- Criar pedidos selecionando itens e quantidades.
- Consultar pedidos em andamento e pedidos finalizados.

## Persistência

O sistema utiliza arquivos locais para armazenar os dados:

- Arquivos JSON individuais para cada restaurante, no formato restaurante_ID.json.
- Arquivo JSON central com os clientes, em clientes.json.
- Arquivo CSV central com os pedidos, em pedidos.csv.

## Estrutura do projeto

- src/main/kotlin/Main.kt: implementação principal das duas interfaces CLI e da persistência.
- src/test/kotlin/DeliveryStorageTest.kt: testes de armazenamento e leitura de restaurantes, clientes e pedidos.

## Como executar

### Executar a aplicação de restaurante
```bash
./gradlew run --args='restaurant'
```

### Executar a aplicação de cliente
```bash
./gradlew run --args='client'
```

### Executar os testes
```bash
./gradlew test
```

## Observações

O fluxo de pedidos é sincronizado por meio dos arquivos locais, permitindo que restaurante e cliente operem de forma independente, mas compartilhando os mesmos dados em disco.
