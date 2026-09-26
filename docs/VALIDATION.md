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
| Android | APK debug e lint aprovados | Distribuição e aparelho físico pendentes |
