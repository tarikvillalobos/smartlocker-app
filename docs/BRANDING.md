# White-label

A configuração `Brand` é independente de `Membership`. Marcas definem aparência,
conteúdo, canais e funcionalidades. Vínculos pertencem ao usuário autenticado.

As configurações SmartLocker e Aurora Lockers estão em
`core/src/commonMain/kotlin/app/smartlocker/config/Brand.kt`. Escolha a marca
na tela de login. A Aurora muda nome, monograma, cores, texto institucional e remove a
funcionalidade de moradores; a rota também é bloqueada no controller.

## Adicionar uma marca

1. Adicione um `Brand` ao registro `Brands.all`, com identificador estável.
2. Defina cores principal/escura, fontes suportadas, texto de apresentação,
   canais, flags, contatos e URLs HTTPS dos documentos legais.
3. Inclua fontes em `composeResources/font` via geração controlada e atualize
   o registro do design system caso a família seja nova.
4. Selecione `Brand.mark` e `monogram`. Para novos logos vetoriais, amplie somente
   o registro `design/BrandSymbol.kt`; as telas permanecem genéricas. Personalize
   o vetor de launcher Android por recurso de build. Não há WebView.
5. Android: use `-PapplicationId=seu.pacote -PappName=SuaMarca` e recursos de ícone
7. Configure endpoint e autenticação somente com o contrato externo documentado.
8. Execute os testes e compare contrastes, textos longos e fonte a 200%.

Não há URLs de suporte ou documentos legais ficticiamente apresentados como
reais. A ausência de configuração é informada quando a ação é acionada.
As fontes e as cores de texto/superfície ficam centralizadas em `design/Tokens.kt`.
O campo applicationId registra a identidade esperada da marca; o empacotamento
nativo precisa ser configurado em cada build como descrito acima.
