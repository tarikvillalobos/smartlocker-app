# Validação executada

Registro de 26/09/2026 em macOS Apple Silicon, JDK 21, Gradle 8.14.3 e Xcode 26.2,
com verificações adicionais nos runners Windows e Linux do GitHub Actions.
A API operacional não existe. A OpenAPI é uma proposta autorizada; nenhuma
verificação abaixo prova integração com backend, fornecedor ou hardware externo.

## Testes compartilhados e desktop

`./gradlew :core:jvmTest :app:desktopTest`: **35 testes executados e aprovados**.
Dois testes de cofre nativo são descobertos, mas pulados nessa suíte por padrão;
sua execução separada e os sistemas verificados estão registrados abaixo.

| Suíte | Testes executados | Verificação |
| --- | ---: | --- |
| DomainTest | 4 | CPF, telefone, e-mail, métricas e validade de credenciais |
| DemoRepositoryTest | 8 | OTP, sessão, retirada, persistência, isolamento e paginação |
| ControllerTest | 5 | Consistência, rotas e respostas atrasadas após mudança de contexto |
| ControllerRecoveryTest | 6 | Recuperação de sessão/local, páginas, recentes, contatos e falha posterior |
| ControllerActionTest | 3 | Confirmação de ações, avisos e concorrência |
| UseCaseTest | 1 | Credencial rejeitada quando pertence a outra encomenda |
| HttpAndQrTest | 4 | HTTP controlado, erros, cancelamento e decodificação do QR |
| ResponsiveUiTest | 2 | Matriz visual e percurso de login, cópia e retirada manual |
| AdaptiveScenarioTest | 2 | Redimensionamento, paisagem, campos e conteúdo extremo |

As regressões cobrem retorno seguro ao vínculo após reautenticar, manutenção das
páginas carregadas ao abrir detalhes, recentes independentes do filtro histórico
e reenvio de verificação de contato depois de navegar. Uma ação já confirmada
continua confirmada se a atualização posterior falhar, evitando relatos repetidos.

A retirada manual permanece distinta do evento físico simulado. Desfazer uma
marcação nunca reativa códigos. Métricas usam o conjunto completo mesmo quando
a listagem mostra apenas 20 de 73 registros. O transporte usa Ktor MockEngine
para verificar 401/403/409/429/503, cancelamento e ausência de repetição automática.
ZXing decodifica o QR e confirma o payload sintético de origem.
Relatórios: `core/build/reports/tests/jvmTest/` e `app/build/reports/tests/desktopTest/`.

## Contrato OpenAPI proposto

A base em `docs/api/openapi.yaml` contém **23 caminhos, 26 operações e 38 schemas**.
A validação formal de OpenAPI 3.1.1, referências locais e oito exemplos passou.
Também passaram **21 casos positivos e negativos de JSON Schema**, cobrindo
CPF, contatos/canais, OTP, preferências, métricas, UTC e cadastro push.

Execute `scripts/validate_openapi.py` e `scripts/check_openapi_cases.py` no ambiente
Python descrito em [OPENAPI.md](OPENAPI.md). A CI executa as mesmas verificações.
Isso valida a estrutura e exemplos da proposta; não testa um servidor nem indica
que os endpoints foram implementados. O app continua sem adaptador dessa minuta.

## Layout e acessibilidade

As cinco telas são renderizadas em 320, 390, 430, 600, 840 e 1200 dp, com fontes
em 100% e 200%: 60 imagens. Mais oito capturas verificam conteúdo longo, vazio,
73 encomendas e mudanças da mesma composição entre 320/390/600/840/1200 dp,
incluindo janelas de 840 × 390 e 390 × 400 dp. Total: **68 capturas** em
`app/build/reports/screenshots/`.

Redimensionar mantém contato digitado, filtro e encomenda selecionada. Ações de
retirada e cópia continuam alcançáveis por rolagem. A navegação inferior quebra
em linhas completas com fonte ampliada, sem comprimir o texto. A redução de
altura em testes Compose simula área disponível; sozinha não testa um IME nativo.

Os cinco templates HTML foram renderizados no navegador. Uma seleção das capturas
Compose foi comparada visualmente: login, início, histórico, detalhe e perfil,
mais layouts amplos, fonte ampliada, paisagem e conteúdo longo. A revisão levou
a correções de paleta, QR imediato, colunas e navegação. Não há comparação de
pixels automatizada nem aprovação manual de cada uma das 68 imagens.

Os testes usam semântica de acessibilidade e verificam limites de ações visíveis.
Não substituem auditoria integral com TalkBack/VoiceOver, ensaios em dobradiça
física nem verificação completa de foco por teclado em cada sistema.

## Cofres nativos desktop

`python3 scripts/run_native_secure_tests.py` executa **dois testes nativos** com
chaves sintéticas exclusivas, cobrindo gravação, leitura por outra instância,
atualização, isolamento, remoção e tratamento de valores longos sem truncamento.
Esses testes passaram no macOS local e nos três runners da CI: Keychain no macOS,
DPAPI no Windows e Secret Service no Linux.

O runner Linux abre uma sessão D-Bus e um cofre temporários, sem reutilizar o
cofre pessoal. Os testes removem apenas suas próprias entradas. Relatório separado:
`app/build/reports/tests/desktopNativeVaultTest/`. Isso verifica os caminhos do
aplicativo nos ambientes testados, sem constituir auditoria do sistema operacional.
Limites de tamanho dos cofres devem ser compatibilizados com os tokens reais do
futuro fornecedor antes de integrar a API; não há fallback para texto puro.

## Android

Build do APK e lint foram executados. A execução registrada do lint teve zero
erros e dois avisos sobre atualização do SDK 35. APK de desenvolvimento:
`androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

A suíte nativa ampliada teve **sete testes aprovados**: cinco de Keystore e dois
de fluxo da Activity. Cobriu ciphertext aleatório, restauração, isolamento por
AAD, rejeição de adulteração, logout, login/OTP, rotação e retirada manual.

O teste exige IME realmente visível e os campos Celular/CPF inteiros acima dele
em retrato. Após girar, verifica o CPF em paisagem antes de qualquer refoco ou
rolagem manual; depois também refoca os dois campos em paisagem. O OTP parcial
sobrevive ao retorno para retrato e a uma recriação explícita da Activity antes
de concluir o login e a retirada manual. Fechar/reabrir a Activity restaura a
sessão, e logout impede sua restauração. Isso não simula morte do processo.

A ampliação identificou perda de foco/IME quando a rotação recriava a Activity.
O manifesto agora permite ao Compose tratar diretamente orientação e tamanho;
fonte e densidade continuam sujeitas à recriação normal. O teste explícito de
recriação mantém a verificação de estado independente dessa decisão de layout.
Veja [RESPONSIVE.md](RESPONSIVE.md). O lint final manteve zero erros e dois avisos.

O app foi aberto no emulador Google APIs Android 34 arm64, aproximadamente
411 × 731 dp. Para repetir testes nativos, use um emulador dedicado e
`./gradlew :androidApp:connectedDebugAndroidTest -PapplicationId=app.smartlocker.validation`.
O identificador separado evita alterar dados de outra instalação.

O lint apresentou incompatibilidade ao enumerar Android 37.0 no SDK global.
`scripts/prepare_android_sdk.py` prepara a cópia isolada de Android 35 usada na
validação e na CI, sem modificar o SDK global nem ocultar regras do lint.

## iOS

O app foi compilado, aberto e testado em simulador. A suíte mais recente passou
em um simulador isolado: **três testes**, sendo dois de Keychain e um de UI.
O fluxo de UI também foi repetido com sucesso; repetições não contam como novos
testes. Keychain verifica persistência, atualização, exclusão repetida e isolamento.

O teste de UI digita com o teclado aberto, gira retrato/paisagem/retrato e exige
que o contato permaneça preenchido e inteiramente acima do teclado. Essa prova
complementa as simulações de altura do desktop. Ensaios em iPhone/iPad físicos
continuam pendentes.

`python3 scripts/test_ios.py` cria e remove apenas seu simulador temporário.
`--device UUID` usa um simulador existente e o preserva; `--output` permite guardar
cada resultado `.xcresult` sem sobrescrever o anterior. A execução isolada
registrada está em `artifacts/ios-isolated-tests.xcresult`. O build utiliza assinatura
local (`CODE_SIGNING_ALLOWED=YES CODE_SIGN_IDENTITY=-`) para habilitar o Keychain.
Isso não é assinatura comercial, provisioning para aparelho nem exportação IPA.
O plist contém `CADisableMinimumFrameDurationOnPhone=true`, exigido pelo Compose.

## Instaladores e integração contínua

| Entrega | Evidência | Limite |
| --- | --- | --- |

Android: `:androidApp:assembleDebug :androidApp:lintDebug`, com **0 erros e
2 avisos** sobre atualização do SDK 35. APK em
`androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
O APK abriu sem crash no emulador Google APIs Android 34 arm64, aproximadamente
411 × 731 dp (1080 × 1920 px, 420 dpi). Login/teclado/rotação Android não foram
exercitados nesse smoke test. O AVD temporário foi encerrado após a verificação.

O lint do AGP utilizado falhou ao enumerar a plataforma global Android 37.0.
A validação passou com cópia isolada dos componentes Android 35, sem modificar
o SDK global. `scripts/prepare_android_sdk.py` reproduz essa preparação; a CI
também usa essa cópia. Não foram ocultadas regras de lint.

O build iOS usa `CODE_SIGNING_ALLOWED=NO`. O plist final contém a chave booleana
`CADisableMinimumFrameDurationOnPhone=true`, exigida pelo Compose. O app abriu
e renderizou o login no iOS 26.2. O target `SmartLockerUITests` passou: **1 teste
nativo adicional**, digitando um contato com o teclado aberto e preservando seu
valor ao girar de retrato para paisagem e voltar. O teste também exige que o
campo inteiro permaneça acima do teclado. Essa verificação revelou e validou
a correção de rolagem do foco após a rotação. iPhone 17 Pro: 402 × 874 pt.
As capturas do XCTest acompanham `artifacts/ios-keyboard-final`;
`artifacts/native-keyboard-screenshots/` contém a exportação. Artefatos são ignorados pelo Git.
Resultado local total: 26 testes JVM/Compose e 1 teste XCTest aprovados.

## Integração contínua e histórico

A CI configura auditoria de commits, desktop em macOS/Windows/Linux, build/lint
Android e build do simulador iOS. A primeira execução encontrou o pacote Android
obsoleto `tools`; os jobs agora especificam os pacotes suportados e isolam o SDK.
Na [execução 36238579513](https://github.com/tarikvillalobos/smartlocker-app/actions/runs/36238579513),
commit `f3497c4`, todos os seis jobs passaram: Android, iOS, auditoria e desktop
nos três sistemas.
Os ajustes seguintes de marca/paleta, foco com teclado e do XCTest foram validados
localmente. Consulte os Actions da revisão desejada para o estado do último push.

A auditoria exige um arquivo textual e até 20 linhas alteradas por commit de
implementação, com autoria `tarik.villalobos@gmail.com`. O README inicial é
preexistente e está fora dessa contagem. Nenhum binário foi adicionado ao Git.

## Pendências externas

Documentação, credenciais e homologação da API não foram fornecidas. Não há
integração real validada com autenticação, lockers, SMS, e-mail, WhatsApp ou push.
A demonstração contém dados fictícios e nunca substitui a API após falha.
Continuam pendentes aparelhos físicos, leitores de tela, cofres Windows/Linux,
permissões/push reais, assinatura de pacotes e distribuição.
O produto ainda não está pronto para produção.
