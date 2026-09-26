# Instalação e distribuição

Os pacotes entregam a experiência local demonstrativa. A OpenAPI é uma proposta;
a API operacional, homologação, documentos legais e fornecedores ainda não existem.
A versão `1.0.0` identifica os pacotes, sem afirmar prontidão para produção.

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
