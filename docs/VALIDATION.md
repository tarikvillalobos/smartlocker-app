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

