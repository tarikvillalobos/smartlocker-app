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

## Configurar o endpoint

Use a URL base HTTPS disponibilizada pelo projeto do backend, incluindo o prefixo
de versão, como `/v1`. A configuração não aceita credenciais na URL, query ou
fragmento. Endpoint e identificador público de marca não são segredos.

- Android: informe `-PapiBaseUrl=https://api.seu-dominio.example/v1` ao Gradle.
  O valor entra no metadata `app.smartlocker.API_BASE_URL` do manifesto.
- iOS: configure o build setting `SMARTLOCKER_API_BASE_URL` no Xcode ou passe-o
  ao `xcodebuild`. Ele preenche `SmartLockerApiBaseUrl` no Info.plist gerado.
- Desktop: defina a variável de ambiente `SMARTLOCKER_API_BASE_URL` antes de
  iniciar o aplicativo ou seu launcher instalado.
- Testes ou composição programática: `AppConfiguration.apiBaseUrl` permite
  injetar o endpoint e `AppRuntime.engineFactory` permite usar MockEngine.

Adicionar testes de contrato para payloads desconhecidos, expiração, paginação,
autorização, respostas atrasadas e retirada concorrente. Implementar registro de
push somente quando a API determinar fornecedor e protocolo. A central local
não representa push integrado.

O servidor externo deverá emitir, consumir, revogar e autorizar credenciais;
nenhuma validação no app prova a segurança interna desse servidor. Os QR Codes
demonstrativos começam com `SMARTLOCKER-DEMO` e não funcionam em lockers reais.
