# SmartLocker App

Aplicativo white-label para usuários de armários inteligentes, em Kotlin
Multiplatform e Compose Multiplatform, com interface em português brasileiro.

Consulte encomendas, códigos e QR Codes de retirada, histórico, notificações,
perfil, contatos, preferências de comunicação e solicitações de suporte.
A identidade visual segue o HTML fornecido: Sora, Plus Jakarta Sans e a paleta
verde da marca padrão. Aurora Lockers demonstra uma segunda personalização.

## Status real

Em desenvolvimento. A experiência demonstrativa funciona localmente dentro do
aplicativo, com persistência e fluxos interativos. Não existe servidor próprio.
A API externa, suas credenciais e documentação ainda não foram fornecidas.
O modo de produção informa essa indisponibilidade e nunca usa demonstração
como fallback. Nenhum SMS, e-mail, WhatsApp, push ou comando de hardware é enviado.
O produto ainda não está pronto para produção.

Consulte [o registro de progresso](docs/PROGRESS.md) e
[a matriz de validação](docs/VALIDATION.md) para distinguir configuração,
compilação, testes e integração real.

## Pré-requisitos

- JDK 21 e Python 3.9 ou superior.
- Android SDK 35, com `ANDROID_HOME` configurado.
- macOS e Xcode para iOS; XcodeGen para gerar o projeto.
- Linux: ambiente gráfico para executar a UI; `secret-tool` e Secret Service
  desbloqueado para persistir sessões. Os testes usam cofres em memória.
- Windows 10 ou posterior, macOS compatível com a JVM ou Linux com bibliotecas
  gráficas suportadas pelo Compose Desktop.

Versões fixadas em `gradle/libs.versions.toml`. O bootstrap baixa Gradle de sua
origem oficial, confere SHA-256 e não exige um JAR binário no repositório.
Fontes são baixadas de revisões fixas do Google Fonts e geradas localmente.


## Getting Started

Setup and development instructions will be added as the project evolves.

## License

Private and proprietary software.
