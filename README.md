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
Se outras versões instaladas causarem falha do lint, isole o SDK testado:

```sh
python3 scripts/prepare_android_sdk.py --source "$ANDROID_HOME"
export ANDROID_HOME="$PWD/.tools/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

A cópia contém apenas Android 35 e ferramentas necessárias. O SDK original não
é alterado. Um `sdk.dir` em `local.properties` tem precedência sobre essas variáveis.

## Execução rápida no desktop

```sh
python3 scripts/prepare_resources.py
./gradlew :app:run --args="--demo"
```

Para Aurora: `./gradlew :app:run --args="--demo --aurora"`.
Sem `--demo`, a primeira abertura usa o ambiente externo não configurado.
A escolha explícita de ambiente e marca é lembrada no dispositivo.
No Windows, substitua `./gradlew` por `gradlew.bat` e `python3` por `python`.

## Android

```sh
python3 scripts/prepare_resources.py
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

Abra SmartLocker no dispositivo ou emulador. Na primeira tela, selecione
“Experimentar demonstração”. O APK está em
`androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
Há suporte a rotação, teclado, insets e janelas com dobradiça separadora.

## iPhone e iPad

```sh
brew install xcodegen
python3 scripts/prepare_resources.py
xcodegen generate --spec iosApp/project.yml
open iosApp/SmartLocker.xcodeproj
```

Selecione o scheme SmartLocker e um simulador. O build phase compila e incorpora
o framework Compose. Configure uma equipe Apple e o bundle ID para um aparelho

## Fluxo demonstrativo

1. Selecione a marca e “Experimentar demonstração”.
2. Use “Preencher dados de demonstração”. São identificadores fictícios.
3. Solicite o código por SMS ou e-mail e digite **123456**.
4. Selecione uma encomenda pendente e veja seu código, QR, prazo e compartimento.
5. Em Perfil → Cenários de demonstração, simule uma nova entrega.
6. Abra o aviso correspondente e selecione “Simular retirada física”.
7. Confira o status e os indicadores no histórico.
8. Em outra encomenda, experimente a marcação manual e sua confirmação.
   “Desfazer” remove apenas a marcação; o código revogado permanece inválido.
9. Altere preferências, verifique um novo contato e registre um relato.

O código de login demonstrativo expira em cinco minutos, permite cinco tentativas
e tem intervalo de reenvio de 30 segundos. Esses valores não definem o contrato
externo. O logout limpa a sessão e os dados fictícios sensíveis daquele contexto.

## Testes

```sh
./gradlew :core:jvmTest :app:desktopTest
./gradlew :androidApp:assembleDebug :androidApp:lintDebug
./gradlew :app:compileKotlinIosSimulatorArm64
python3 scripts/audit_commits.py
```

Testes desktop geram capturas em `app/build/reports/screenshots/` e relatórios
em `app/build/reports/tests/desktopTest/`. Use `xvfb-run` em Linux sem display.
A CI configura runners Linux, Windows e macOS, além de Android e simulador iOS.

## Organização

- `core`: domínio, entidades, contratos, validações e indicadores.
- `app/commonMain`: repositórios, transporte HTTP, demonstração, estado e UI.
- `app/androidMain`, `iosMain`, `desktopMain`: serviços específicos.
- `androidApp`: Activity, ViewModel, ícone, manifesto e tratamento de dobradiças.
- `iosApp`: host SwiftUI, Keychain e configuração XcodeGen.
- `scripts`: recursos, bootstrap e auditoria de commits.
- `docs`: arquitetura, API, personalização, responsividade e validação.

Veja [arquitetura e segurança local](docs/ARCHITECTURE.md),
[integração externa](docs/API.md), [white-label](docs/BRANDING.md) e
[responsividade](docs/RESPONSIVE.md).

## Distribuição

Desktop: `./gradlew :app:packageDistributionForCurrentOS`. Pacotes DMG, MSI e DEB
são gerados no sistema correspondente; assinatura e notarização não estão
configuradas. Android release exige keystore privado e política de assinatura.
iOS exige equipe Apple, provisioning, ícones de distribuição e documentos legais.
Não inclua segredos ou chaves privadas no repositório.

## Git

Cada commit de implementação altera exatamente um arquivo e até 20 linhas
adicionadas + removidas. Ative o hook após clonar:
`git config core.hooksPath .githooks`. A CI também audita o histórico.
O commit inicial do README precede esta implementação e fica fora da contagem.
Binários gerados, fontes, APKs e frameworks não são versionados.

## Licença

Software privado e proprietário. Todos os direitos reservados.
Licenças de componentes e fontes de terceiros não alteram a licença do produto;
veja [avisos de terceiros](docs/THIRD_PARTY.md).
