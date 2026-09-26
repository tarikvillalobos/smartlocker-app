# Layout adaptativo

A UI mede as restrições da janela. Não utiliza uma tela fixa de 390 × 844.

- Abaixo de 600 dp: navegação inferior, coluna única e detalhe em rota própria.
- A partir de 600 dp, se largura/escala de fonte ≥ 440: navigation rail.
- A partir de 1000 dp, se largura/escala ≥ 800: histórico e detalhe lado a lado.
- Formulários de login: máximo 480 dp. Páginas comuns: máximo 600 dp.
- Todos os conteúdos extensos rolam; não há altura fixa de card ou campo de texto.
- Chips, indicadores e grupos numéricos usam quebra de linha.
- QR Code usa módulos quadrados inteiros, preto/branco, quatro módulos livres
  em cada lado, proporção 1:1 e máximo de 208/232 dp.
- Safe areas e IME insets são aplicados à área disponível.
- Android observa FoldingFeature separadora e usa o maior painel livre da
  dobradiça, recalculando o layout pela largura desse painel.
- Estado e seleção ficam fora da composição; mudar o tamanho não reinicia login,
  filtros ou a encomenda selecionada. Campos usam rememberSaveable.
- Botões têm altura mínima de 48/52 dp; ícones acionáveis têm descrições.
- Status têm rótulos textuais. Material oferece foco e navegação por Tab;
  Escape volta ao início no desktop e o botão voltar Android é tratado.
