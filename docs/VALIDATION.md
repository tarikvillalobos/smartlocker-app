# Validação executada

Registro de 26/09/2026 em macOS Apple Silicon, JDK 21, Gradle 8.14.3 e Xcode 26.2.
A implementação real da API permanece pendente; nenhuma verificação abaixo prova
segurança ou funcionamento de um backend ou de hardware externo.

## Testes compartilhados e desktop

`./gradlew :core:jvmTest :app:desktopTest`: **26 testes aprovados, sem falhas**.

| Suíte | Testes | Verificação |
| --- | ---: | --- |
| DomainTest | 4 | CPF, telefone, e-mail, métricas e validade de credenciais |
| DemoRepositoryTest | 8 | OTP, sessão, retirada, persistência, isolamento e paginação |
| ControllerTest | 5 | Consistência, rotas e respostas atrasadas após mudança de contexto |
| UseCaseTest | 1 | Credencial rejeitada quando pertence a outra encomenda |
| HttpAndQrTest | 4 | HTTP controlado, erros, cancelamento e decodificação do QR |
| ResponsiveUiTest | 2 | Matriz visual e percurso de login, cópia e retirada manual |
| AdaptiveScenarioTest | 2 | Redimensionamento, paisagem, campos e conteúdo extremo |

O fluxo de retirada distingue marcação manual, desfazimento e evento físico
simulado. O desfazimento nunca reativa códigos. Métricas são calculadas sobre
dados completos mesmo quando a listagem mostra apenas 20 de 73 registros.
Cofres em memória são usados nos testes; isso não valida os cofres dos sistemas.

O transporte é testado com Ktor MockEngine, sem endpoints inventados no produto.
Os testes verificam respostas 401/403/409/429/503, cancelamento e ausência de
retentativa automática. ZXing decodifica o QR e confirma o payload de origem.
Relatórios: `core/build/reports/tests/jvmTest/index.html` e
`app/build/reports/tests/desktopTest/index.html`.

## Layout e acessibilidade

As cinco telas são renderizadas em 320, 390, 430, 600, 840 e 1200 dp, com fontes
em 100% e 200%: 60 imagens. Mais oito capturas verificam conteúdo longo, vazio,
73 encomendas e mudanças da mesma composição entre 320/390/600/840/1200 dp,
incluindo janelas de 840 × 390 e 390 × 400 dp. Total: **68 capturas** em
`app/build/reports/screenshots/`.

O redimensionamento mantém contato digitado, filtro e encomenda selecionada.
Ações de retirada e cópia continuam alcançáveis por rolagem. A navegação inferior
quebra em linhas completas com fonte ampliada, sem comprimir o texto.
O teste de altura reduzida simula área disponível; sozinho não testa um IME nativo.

Os cinco templates HTML foram renderizados no navegador. Uma seleção das capturas
Compose foi comparada visualmente: login, início, histórico, detalhe e perfil,
mais layouts amplos, fonte ampliada, paisagem e conteúdo longo. A revisão resultou
em correções de paleta, QR imediato, colunas e navegação. Não há comparação de
pixels automatizada nem aprovação manual de cada uma das 68 imagens.

Os testes usam semântica de acessibilidade e verificam limites horizontais de
ações visíveis. Não substituem uma auditoria integral com TalkBack/VoiceOver,
nem ensaios de dobradiça física ou de foco por teclado em cada sistema.

## Builds e recursos nativos

| Plataforma | Evidência | Limites |
| --- | --- | --- |
| Desktop macOS | Compilação e testes Compose/JVM aprovados | DMG e assinatura não ensaiados |
| Android | APK, lint e abertura no emulador API 34 aprovados | Distribuição e aparelho físico pendentes |
| iOS | Build, abertura e teste de teclado/rotação no iPhone 17 Pro simulado | Assinatura e aparelho físico pendentes |
| Windows/Linux | Testes Compose/JVM aprovados na CI | Cofres nativos e instaladores não ensaiados |

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
valor ao girar de retrato para paisagem e voltar. iPhone 17 Pro: 402 × 874 pt.
Resultado local total: 26 testes JVM/Compose e 1 teste XCTest aprovados.

## Integração contínua e histórico

A CI configura auditoria de commits, desktop em macOS/Windows/Linux, build/lint
Android e build do simulador iOS. A primeira execução encontrou o pacote Android
obsoleto `tools`; os jobs agora especificam os pacotes suportados e isolam o SDK.
Na [execução 36238579513](https://github.com/tarikvillalobos/smartlocker-app/actions/runs/36238579513),
commit `f3497c4`, Android, auditoria e testes desktop nos três sistemas passaram.
Os ajustes seguintes de marca/paleta e do teste XCTest foram novamente validados
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
