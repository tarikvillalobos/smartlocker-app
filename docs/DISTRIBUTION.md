# Instalação e distribuição

A versão `1.1.0` inclui o cliente HTTP da OpenAPI proposta e a demonstração local.
O backend está sendo finalizado em outro projeto; a homologação conjunta depende
do ambiente externo. Configure o endpoint conforme [API.md](API.md). Os pacotes
sem endpoint permitem selecionar a demonstração explicitamente. Documentos legais,
fornecedores e assinaturas de distribuição dependem da configuração do proprietário.

## Desktop

JDK 21 é necessário para construir. O usuário final recebe um runtime Java incluído.
Gere os recursos e construa no sistema de destino:

```sh
python3 scripts/prepare_resources.py
python3 scripts/prepare_icons.py
./gradlew :app:packageDistributionForCurrentOS -Pbrand=smartlocker
./gradlew :app:packageDistributionForCurrentOS -Pbrand=aurora
```

Use `python scripts/gradle.py` no Windows. O parâmetro `brand` seleciona nome,
ícones, identificador e marca inicial; não define local/unidade do usuário.
Os arquivos ficam em `app/build/compose/binaries/main/{dmg,msi,deb}/`.
Cada sistema gera seu próprio formato. macOS exige as ferramentas do Xcode;
Windows exige WiX 3 no PATH e `WIX_PATH` apontando para sua pasta `bin`; Linux exige `fakeroot` para o pacote DEB.

O workflow **Build installable artifacts**, acionado manualmente em Actions,
produz DMG, MSI e DEB de ambas as marcas e um ZIP do app iOS de simulador.
São artefatos privados do repositório, mantidos por 14 dias, sem publicação em loja.
Os pacotes não têm assinatura comercial nem notarização. Use identidades do
proprietário para assinar antes de distribuir externamente.

Para persistir sessões, macOS usa Keychain, Windows usa DPAPI e Linux usa Secret
Service. Linux precisa de `libsecret-tools` e de um cofre desbloqueado, como
`gnome-keyring`. Não há fallback de credenciais para arquivo em texto puro.

## Android

`./gradlew :androidApp:assembleDebug` gera o APK de desenvolvimento em
`androidApp/build/outputs/apk/debug/androidApp-debug.apk`, assinado com a chave
de debug local. O app abre no ambiente externo; escolha “Experimentar demonstração”.
Para produção, configure o keystore privado e a assinatura do responsável.
Não versione arquivos de chave, senhas, certificados privados ou tokens.

Os testes nativos usam um identificador separado para preservar dados pessoais:

```sh
./gradlew :androidApp:connectedDebugAndroidTest -PapplicationId=app.smartlocker.validation
```

Use um emulador dedicado. Essa variante limpa apenas seus próprios dados de teste.
As verificações cobrem Keystore, adulteração de ciphertext, rotação, login, retirada,
reabertura da Activity e logout; reabertura de Activity não simula morte do processo.

## iOS

Gere recursos e ícones antes de gerar o projeto Xcode. `python3 scripts/test_ios.py`
executa Keychain e UI em um iPhone temporário, removido ao final; `--device UUID`
usa um simulador existente e o preserva. `--output artifacts/nova-execucao` preserva resultados anteriores.

O ZIP de CI contém `SmartLocker.app` para simulador arm64, com assinatura local
para habilitar o Keychain. Não é um IPA nem uma assinatura de distribuição Apple.
Distribuição em aparelho exige equipe Apple, provisioning e exportação assinada.
O projeto inclui ícones; selecione `AuroraIcon` e ajuste nome/bundle ID para outra marca,
além de configurar a identidade inicial do runtime. Veja [BRANDING.md](BRANDING.md).

## Evidências

Consulte [VALIDATION.md](VALIDATION.md) para distinguir build, teste nativo,
inspeção visual e integração real. Binários, catálogos de ícones e relatórios são
gerados em diretórios ignorados; apenas suas fontes textuais são versionadas.
