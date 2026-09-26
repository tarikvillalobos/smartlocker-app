# Componentes de terceiros

SmartLocker App permanece privado e proprietário.

Fontes: Plus Jakarta Sans e Sora, obtidas do repositório Google Fonts, sob
SIL Open Font License 1.1. O script de recursos baixa os arquivos OFL originais
para `.tools/fonts`. Inclua esses avisos nos pacotes distribuídos.

Bibliotecas: Kotlin, Compose Multiplatform, Ktor Client, kotlinx.coroutines,
kotlinx.serialization, kotlinx.datetime, AndroidX, JNA e QRCode-Kotlin.
ZXing é usado nos testes de decodificação. Preserve as licenças e os avisos
exigidos por cada dependência ao preparar uma distribuição.

Versões escolhidas após consulta em 25/09/2026:

- [Compatibilidade KMP](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html).
- [Compatibilidade Compose](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).
- [Compose 1.10.3](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.10.3).
- [Kotlin 2.3.21](https://github.com/JetBrains/kotlin/releases/tag/v2.3.21).
- [Versões Ktor](https://ktor.io/docs/releases.html).
- [QRCode-Kotlin](https://github.com/g0dkar/qrcode-kotlin).

Foram preferidas versões estáveis compatíveis com o ambiente instalado,
não necessariamente as versões mais novas. Material 3 foi fixado em 1.9.0
para evitar a versão alpha sugerida pelo catálogo padrão do plugin Compose.
