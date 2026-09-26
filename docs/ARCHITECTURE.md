# Arquitetura

O módulo `core` não conhece Compose, HTTP, banco ou APIs de sistema. Entidades,
contratos e funções de domínio pertencem às funcionalidades auth, parcels,
profile e shared. O aplicativo organiza dados e apresentação por funcionalidade.

`AppRuntime` é a raiz de composição e faz injeção por construtor. A seleção do
ambiente é explícita. `AppController` expõe `StateFlow<AppState>` imutável;
ações iniciam coroutines e publicam novos estados. Os composables apresentam
estado e disparam ações. Mudanças em dados são relidas antes de mostrar sucesso.

## Consistência

- Início, histórico, detalhe e indicadores compartilham o mesmo estado.
- Troca de local cancela leituras e incrementa uma geração; respostas antigas
  não substituem o contexto atual. Filtros e seleção permanecem no controller.
- ViewModel retém o runtime Android durante recriação de Activity.
- Sessão expirada remove dados visíveis e mantém apenas destino e seleção.
  O destino só é reutilizado após login compatível com o usuário anterior.
- Erros preservam os metadados visíveis como desatualizados e removem códigos.
