# Integração com a API externa

Nenhum contrato foi encontrado no repositório ou nos anexos. A documentação e a
URL de homologação foram solicitadas. Não foram inventados endpoints, DTOs de
produção, parâmetros de autenticação, renovação, idempotência ou payloads reais.
Não há projeto de servidor, banco de servidor, migrações ou simulador HTTP.

## Implementado independentemente do contrato

- Portas `LockerRepository`, `SecureStorage`, `LocalStorage` e `PlatformServices`.
- `HttpTransport`: Ktor Client, HTTPS obrigatório, JSON, timeouts, cancelamento,
  erros locais seguros e ausência de retentativas de operações mutáveis.
- Testes MockEngine com rotas exclusivamente fictícias dentro dos testes.
- `UnconfiguredRepository`: falha explícita no ambiente externo.
- Repositórios demonstrativos locais e persistentes, sem rede.

O transporte não presume Bearer, cookies ou renovação automática. Cabe ao futuro
adaptador fornecer exatamente o método, caminho e autenticação documentados.
Nenhum corpo de resposta de erro é exibido diretamente nem registrado em logs.

## Informações necessárias

1. OpenAPI/Swagger ou coleção com exemplos de sucesso e erro.
2. URLs de homologação e produção por cliente/ambiente.
3. Mecanismo de login por SMS/e-mail, desafio, verificação, expiração e reenvio.
4. Formato e armazenamento de sessão, renovação, logout e revogação.
5. Autorizações por usuário, cliente, local, unidade e destinatário.
6. Paginação, ordenação e definição do período dos indicadores.
7. Credencial de retirada: payload/imagem, validade, escopo, estados e atualização.
8. Capacidade de marcação manual e desfazer; eventos de confirmação física.
9. Preferências, disponibilidade dos canais e permissões de contatos.
10. Avisos, push, deep links, registro do dispositivo e identificadores permitidos.
11. Relatos e consulta de solicitações; disponibilidade por cliente.
12. Política de cache, ETags, erros, limites e idempotência, quando existentes.

## Trabalho após receber o contrato

Criar DTOs em `data` e mapeamentos validados para os modelos de domínio. Implementar
`LockerRepository` com o transporte e a autenticação documentados. Injetar esse
adaptador no ramo PRODUCTION de `AppRuntime`; nunca delegar para `DemoRepository`.
