# SmartLocker App

Aplicativo white-label para usuários de armários inteligentes, em Kotlin
Multiplatform e Compose Multiplatform, com interface em português brasileiro.

Consulte encomendas, códigos e QR Codes de retirada, histórico, notificações,
perfil, contatos, preferências de comunicação e solicitações de suporte.
A identidade visual segue o HTML fornecido: Sora, Plus Jakarta Sans e a paleta
verde da marca padrão. Aurora Lockers demonstra uma segunda personalização.

## Status real

O cliente HTTP está implementado: login e renovação de sessão, encomendas,
credenciais, histórico, avisos, perfil, contatos, preferências e suporte usam o
contrato em [OpenAPI](docs/OPENAPI.md). O backend está sendo finalizado em outro
projeto. Este repositório entrega o aplicativo e não cria esse servidor.

Com endpoint HTTPS configurado, o modo de produção usa a API externa. Sem
configuração válida, apresenta indisponibilidade; erros nunca ativam dados
fictícios. A demonstração permanece uma opção explícita e funciona localmente.
Testes de cliente com respostas controladas não comprovam homologação externa.

A liberação para produção depende de homologação com o backend, canais e lockers,
além de assinatura e distribuição. Push nativo ainda não está integrado. SMS,
e-mail e WhatsApp são responsabilidades do backend e dos fornecedores; o app
não envia comandos de abertura de portas ou confirmações físicas de retirada.

Consulte [o registro de progresso](docs/PROGRESS.md) e
[a matriz de validação](docs/VALIDATION.md) para distinguir configuração,
compilação, testes e integração real.

## Pré-requisitos

- JDK 21 e Python 3.9 ou superior. No macOS, o inicializador Gradle
  seleciona automaticamente o JDK 21 instalado; em outros sistemas,
  configure JAVA_HOME para ele antes de executar o Gradle.
- Android SDK 35, com `ANDROID_HOME` configurado.
- macOS e Xcode para iOS; XcodeGen para gerar o projeto.
- Linux: ambiente gráfico para executar a UI; `secret-tool` e Secret Service
  desbloqueado para persistir sessões. Há testes em memória e testes nativos isolados.
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
Sem `--demo`, a primeira abertura usa o ambiente externo. Para uma API disponível:

```sh
export SMARTLOCKER_API_BASE_URL="https://api.seu-dominio.example/v1"
./gradlew :app:run
```

Substitua o domínio de exemplo pela URL real fornecida pelo projeto do backend.
A escolha explícita de ambiente e marca é lembrada no dispositivo; se a instalação
estava em demonstração, selecione “Usar API externa” na tela de login.
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
Para apontar um build para o backend, use `-PapiBaseUrl`:

```sh
./gradlew :androidApp:assembleDebug -PapiBaseUrl="$SMARTLOCKER_API_BASE_URL"
```

A variável deve conter uma URL HTTPS real. O endpoint é configuração pública,
sem credenciais de usuário ou chaves de fornecedor.

## iPhone e iPad

```sh
brew install xcodegen
python3 scripts/prepare_resources.py
python3 scripts/prepare_icons.py
xcodegen generate --spec iosApp/project.yml
open iosApp/SmartLocker.xcodeproj
```

Selecione o scheme SmartLocker e um simulador. O build phase compila e incorpora
o framework Compose. Configure uma equipe Apple e o bundle ID para um aparelho
físico. O teste nativo `SmartLockerUITests` pode ser executado com Product → Test
ou `xcodebuild test` selecionando um simulador instalado. O projeto Xcode é gerado
e ignorado pelo Git; edite `project.yml`.

Para o backend, defina `SMARTLOCKER_API_BASE_URL` em Build Settings ou passe
`SMARTLOCKER_API_BASE_URL="$SMARTLOCKER_API_BASE_URL"` ao `xcodebuild`.
Esse build setting preenche o endpoint no Info.plist; apenas definir uma variável
no shell, sem transmiti-la ao build, não altera um aplicativo iOS já compilado.

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
A CI valida OpenAPI, histórico, Android, iOS e desktop em Linux, Windows e macOS.
As suítes verificam domínio, contrato HTTP, sessão rotativa, armazenamento
em partes, paginação, revogação de acesso e percurso de produção com respostas
controladas. Consulte [VALIDATION.md](docs/VALIDATION.md) para contagens, resultados
e revisões efetivamente executadas, incluindo cofres nativos separados.

Para verificar os cofres nativos com entradas sintéticas isoladas, execute
`python3 scripts/run_native_secure_tests.py`. Para Keychain e UI no simulador iOS,
use `python3 scripts/test_ios.py`; ele cria e remove somente seu simulador temporário.
Os testes Android usam um identificador separado:

```sh
./gradlew :androidApp:connectedDebugAndroidTest -PapplicationId=app.smartlocker.validation
```

Execute esse comando em emulador dedicado. Sete casos nativos passaram em
26/09/2026 após a integração do cliente, incluindo Keystore, teclado visível,
rotação e recriação explícita da Activity. Eles usam o fluxo demonstrativo;
a suíte HTTP usa respostas controladas, sem homologação com servidor real.
A validação do contrato proposto é descrita em [OPENAPI.md](docs/OPENAPI.md).

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

[Instruções de instalação e distribuição](docs/DISTRIBUTION.md) descrevem APK,
DMG, MSI, DEB e app de simulador iOS. Execute `python3 scripts/prepare_icons.py`
antes de empacotar. O workflow **Build installable artifacts** configura pacotes
das duas marcas, com Java incluído no desktop, como artefatos privados de CI.
A versão 1.1.0 inclui o cliente HTTP. Os sete jobs de instaladores e os sete jobs
de verificação passaram na revisão registrada em [VALIDATION.md](docs/VALIDATION.md).
A entrega local está em `artifacts/delivery/1.1.0/`, com instruções, manifesto e hashes.
Assinatura comercial, notarização e exportação para aparelhos iOS dependem das
identidades do proprietário. Não inclua segredos no repositório.

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
