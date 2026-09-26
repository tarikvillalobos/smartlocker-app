# Integração com a API externa

O aplicativo possui cliente HTTP implementado para o contrato versionado em
[api/openapi.yaml](api/openapi.yaml). O backend está sendo finalizado em outro
projeto; este repositório contém o aplicativo, sem implementar esse servidor.
A especificação continua identificada como minuta `0.1.0-draft` até homologação.
Seu domínio `.invalid` é um placeholder não operacional.

A existência do cliente e dos testes com respostas controladas não comprova
conectividade com homologação, entrega de mensagens nem operação de lockers reais.
Consulte [OPENAPI.md](OPENAPI.md) e os registros datados de [VALIDATION.md](VALIDATION.md).

## Implementado no aplicativo

- `AppRuntime`: `ApiLockerRepository` em PRODUCTION quando existe endpoint HTTPS
  válido; `UnconfiguredRepository` quando a configuração está ausente ou inválida.
- `HttpTransport`: Ktor, HTTPS, JSON, timeouts, cancelamento, bloqueio de redirects,
  mensagens locais de erro e limite para o corpo de resposta.
- `ApiSessionClient`: configuração pública, OTP, reenvio explícito, verificação,
  Bearer, restauração, refresh rotativo serializado e logout.
- `ApiLockerRepository`: perfil, vínculos, encomendas, indicadores, credenciais,
  marcação manual/desfazer, preferências, contatos, avisos, suporte e destinatários.
- DTOs e mapeamentos validados: identidade e escopo, estados, versões, datas,
  intervalos, campos opcionais, capacidades e cursores.
- Testes MockEngine do transporte, autenticação e operações de negócio; testes
  controlados usam dados sintéticos, sem criar um servidor para o aplicativo.

A seleção de demonstração é explícita. Falhas de configuração, autenticação ou
rede em produção nunca trocam o repositório para dados demonstrativos.

1. Aprovação ou revisão da minuta pelo fornecedor que implementará a API.
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

## Trabalho após homologar e disponibilizar o contrato

Criar DTOs em `data` e mapeamentos validados para os modelos de domínio. Implementar
`LockerRepository` com o transporte e a autenticação documentados. Injetar esse
adaptador no ramo PRODUCTION de `AppRuntime`; nunca delegar para `DemoRepository`.
Configurar o endpoint em `AppConfiguration.apiBaseUrl` a partir do ambiente da
marca, sem segredos embutidos. Validar os fluxos em homologação com contas de teste.

Adicionar testes de contrato para payloads desconhecidos, expiração, paginação,
autorização, respostas atrasadas e retirada concorrente. Implementar registro de
push somente quando a API determinar fornecedor e protocolo. A central local
não representa push integrado.

O servidor externo deverá emitir, consumir, revogar e autorizar credenciais;
nenhuma validação no app prova a segurança interna desse servidor. Os QR Codes
demonstrativos começam com `SMARTLOCKER-DEMO` e não funcionam em lockers reais.
