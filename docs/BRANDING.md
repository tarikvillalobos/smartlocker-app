# White-label

A configuração `Brand` é independente de `Membership`. Marcas definem aparência,
conteúdo, canais e funcionalidades. Vínculos pertencem ao usuário autenticado.

As configurações SmartLocker e Aurora Lockers estão em
`core/src/commonMain/kotlin/app/smartlocker/config/Brand.kt`. Escolha a marca
na tela de login. A Aurora muda nome, cores, texto institucional e remove a
funcionalidade de moradores; a rota também é bloqueada no controller.

## Adicionar uma marca

1. Adicione um `Brand` ao registro `Brands.all`, com identificador estável.
2. Defina cores principal/escura, fontes suportadas, texto de apresentação,
   canais, flags, contatos e URLs HTTPS dos documentos legais.
3. Inclua fontes em `composeResources/font` via geração controlada e atualize
   o registro do design system caso a família seja nova.
4. Personalize o símbolo/wordmark de `LoginScreen` e o vetor de launcher Android
   por recurso de build. O logo padrão é vetorial e não depende de WebView.
5. Android: use `-PapplicationId=seu.pacote -PappName=SuaMarca` e recursos de ícone
