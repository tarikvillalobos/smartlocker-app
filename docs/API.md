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

