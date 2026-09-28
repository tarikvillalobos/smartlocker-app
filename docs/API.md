# Integração com a API externa

O aplicativo possui cliente HTTP para as rotas SmartLocker do contrato unificado
em [api/condo-platform-openapi.yaml](api/condo-platform-openapi.yaml). O contrato
anterior está em [api/openapi.yaml](api/openapi.yaml) para checar compatibilidade.
O backend está em outro projeto. A versão atual é `1.1.0-draft`.
Seu domínio `.invalid` é um placeholder não operacional.

A existência do cliente e dos testes com respostas controladas não comprova
conectividade com homologação, entrega de mensagens nem operação de lockers reais.
Consulte [OPENAPI.md](OPENAPI.md) e os registros datados de [VALIDATION.md](VALIDATION.md).

## Implementado no aplicativo

- `AppRuntime`: `ApiLockerRepository` em PRODUCTION quando existe endpoint HTTPS
  válido; `UnconfiguredRepository` quando a configuração está ausente ou inválida.
- `HttpTransport`: Ktor, HTTPS, JSON, timeouts, cancelamento, bloqueio de redirects,
  mensagens locais de erro e limite para o corpo de resposta.
- `ApiSessionClient`: configuração pública, OTP ou senha conforme `authMethods`,
  reenvio e verificação de OTP, Bearer, refresh rotativo e logout.
- `ApiLockerRepository`: perfil, vínculos, encomendas, indicadores, credenciais,
  marcação manual/desfazer, delegação em condomínio, preferências, contatos,
  avisos, suporte e destinatários.
- DTOs e mapeamentos validados: identidade e escopo, estados, versões, datas,
  destinatário, unidade, delegados, pessoa que retirou, capacidades e cursores.
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

O domínio de exemplo acima não oferece serviço. Substitua-o pelo ambiente real.
A configuração do endpoint não muda uma preferência de demonstração já salva;
na tela de login, selecione “Usar API externa” quando necessário.

## Autenticação e armazenamento

`X-Brand-Id` acompanha as requisições. O access token segue no header Bearer;
refresh token, sessionId, marca, usuário e os dois prazos ficam no cofre nativo.
O prazo exposto ao controller representa a validade da sessão renovável. Expirar
um access token isoladamente permite refresh antes da próxima leitura protegida.
Revogação ou expiração da sessão remove os dados autenticados.

O refresh é serializado. Sua chave de idempotência pendente é gravada no cofre
antes da chamada, permitindo repetir a mesma intenção após resposta incerta.
Operações mutáveis não são repetidas automaticamente. Chaves de outras intenções
incertas ficam em memória durante a sessão para uma repetição explícita.
Não existe fila offline que apresente confirmação antes da resposta do servidor.

`ChunkedSecureStore` grava a sessão em partes pequenas no próprio cofre, usando
manifesto e journal para substituição e recuperação. O limite do registro é
256 KiB; tokens não são truncados para caber em um único item nativo. Não existe
fallback para arquivo sem proteção. Logout limpa a sessão local mesmo se não
for possível confirmar a revogação remota.

## Consistência do contrato

O app consulta `/me` e a lista completa `/me/memberships`. Os IDs usados nos
caminhos são IDs de vínculo, distintos de IDs físicos de local ou unidade.
Lockers avulsos podem retornar `unitId` e `unitLabel` nulos em conjunto.
Capacidades públicas, capacidades do vínculo e configuração da marca limitam
as opções da interface; o servidor continua responsável por autorizar cada chamada.
O nome mostrado da marca vem de `/configuration.appName`; o nome da pessoa vem
de `/me.name`. O contrato não oferece edição do nome da pessoa pelo próprio app.
`authMethods` e a disponibilidade de SMS/e-mail controlam as opções de login.
Senha pode ser recuperada por desafio e alterada no perfil quando habilitada.
O nome do aplicativo instalado no sistema operacional continua definido no build.

Listas de encomendas, avisos e solicitações preservam cursores opacos. Cada ação
“Carregar mais” solicita uma página. O contador de avisos usa `unreadCount` global;
indicadores usam `/parcel-metrics`, incluindo valores desconhecidos quando
`complete=false`, sem inventar totais a partir das linhas visíveis.

Marcação manual e reversão enviam `If-Match` com a versão validada anteriormente.
Sem versão disponível, o cliente consulta o detalhe antes do comando. Conflitos
são apresentados ao usuário, sem adotar outra versão e repetir a mutação.
Código e QR usam somente o conteúdo autorizado pelo servidor, sem persistência
local; são ocultados ao atingir `revalidateAfter`, expirar ou perder frescor.

## Homologação e integrações pendentes

O projeto externo precisa disponibilizar ambientes e contas de teste, confirmar
compatibilidade com a minuta e homologar isolamento, refresh, concorrência,
idempotência, canais, prazos e eventos físicos. Esses resultados não podem ser
inferidos dos testes locais ou de um build aprovado.

SMS, e-mail e WhatsApp dependem do backend e de seus fornecedores. O contrato e
os DTOs descrevem push opcional, mas registro FCM/APNs, entrega nativa e navegação
por notificações externas ainda não estão integrados. A central de avisos via
HTTP é independente de push. Nenhuma chave de fornecedor deve entrar no app.

Não há endpoint do usuário para abrir portas, depositar encomendas ou confirmar
retirada física. Esses eventos são responsabilidade do sistema de lockers e do
backend. QR Codes demonstrativos começam com `SMARTLOCKER-DEMO` e são fictícios.
