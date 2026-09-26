# Layout adaptativo

A UI mede as restrições da janela. Não utiliza uma tela fixa de 390 × 844.

- Abaixo de 600 dp: navegação inferior, coluna única e detalhe em rota própria.
- A partir de 600 dp, se largura/escala de fonte ≥ 440: navigation rail.
- A partir de 1000 dp, se largura/escala ≥ 800: histórico e detalhe lado a lado.
- Fonte a partir de 150%: ações da navegação inferior quebram em linhas completas.
- Formulários de login: máximo 480 dp. Páginas comuns: máximo 600 dp.
- Todos os conteúdos extensos rolam; não há altura fixa de card ou campo de texto.
- Chips, indicadores e grupos numéricos usam quebra de linha.
- QR Code usa módulos quadrados inteiros, preto/branco, quatro módulos livres
  em cada lado, proporção 1:1 e máximo de 208/232 dp.
- Safe areas e IME insets são aplicados à área disponível.
- Campos focados solicitam rolagem ao mudar janela/teclado. No iOS, o Compose
  controla os insets, sem deslocamento adicional do controlador nativo.
- O XCTest exige que o campo fique inteiro acima do teclado após rotação.
- Android observa FoldingFeature separadora e usa o maior painel livre da
  dobradiça, recalculando o layout pela largura desse painel.
- Estado e seleção ficam fora da composição; mudar o tamanho não reinicia login,
  filtros ou a encomenda selecionada. Campos usam rememberSaveable.
- Botões têm altura mínima de 48/52 dp; ícones acionáveis têm descrições.
- Status têm rótulos textuais. Material oferece foco e navegação por Tab;
  Escape volta ao início no desktop e o botão voltar Android é tratado.

## Mudanças de janela no Android

A Activity hospeda apenas Compose; não incorpora AndroidView ou AndroidFragment.
O manifesto trata diretamente `orientation`, `screenSize`, `smallestScreenSize`
e `screenLayout`. Assim, rotação e redimensionamento atualizam a composição sem
recriar a Activity. A decisão busca manter foco e sessão do teclado durante a
mudança de janela; não fixa orientação nem impede tela dividida.

A UI já observa restrições em BoxWithConstraints, densidade em LocalDensity,
insets de safe area/IME e a geometria de FoldingFeature. Esses valores continuam
mudando e precisam atualizar o layout mesmo quando a Activity é mantida.
Se uma View nativa for incorporada no futuro, será necessário revisar como ela
recarrega recursos e configuração. Essa responsabilidade acompanha a opção de
tratar mudanças diretamente. Veja a [orientação oficial do Android para Compose](https://developer.android.com/guide/topics/resources/runtime-changes).

`fontScale`, `density` e outras alterações não declaradas continuam usando a
recriação normal. O runtime é retido pelo ViewModel; campos usam rememberSaveable.
O teste nativo também chama ActivityScenario.recreate() durante o OTP para
verificar restauração fora da rotação. Essa recriação explícita não equivale a
morte do processo. O resultado do teste ampliado permanece em [VALIDATION.md](VALIDATION.md).
## Verificação

Testes Compose exercitam 320, 390, 430, 600, 840 e 1200 dp, em escala 100% e 200%,
e geram capturas das cinco telas. Testes adicionais reduzem a altura até 390/400 dp,
redimensionam a mesma composição e exercitam conteúdo longo, 73 registros e vazio. A validação visual e as verificações adicionais
realmente executadas são registradas em VALIDATION.md. Simulação de janela não
substitui ensaio em um dobrável físico nem auditoria com VoiceOver/TalkBack.
