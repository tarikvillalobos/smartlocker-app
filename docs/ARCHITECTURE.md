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
- Credenciais só são exibidas enquanto ativas, não expiradas e verificadas
  recentemente. A janela conservadora de apresentação é de 60 segundos;
  a validade real deverá continuar sob controle da API.
- Não existe fila offline de confirmação com sucesso otimista.

## Persistência e proteção

Demonstração: snapshots versionados de dados fictícios, separados por marca e
usuário de demonstração, filtrados por vínculo e destinatário nas consultas.
`ScopedCache` oferece chaves por marca/usuário/unidade para metadados futuros.
Credenciais de retirada não são armazenadas nesse cache.

Sessões: Android usa AES-GCM com AAD por chave e Android Keystore, com backup
desativado. iOS usa Keychain `AfterFirstUnlockThisDeviceOnly`. Desktop usa
Keychain, DPAPI ou Secret Service. Não há fallback para arquivo em texto claro.
A indisponibilidade do cofre é apresentada ao usuário.

Dados locais do desktop ficam em `~/.smartlocker`, com diretório 0700 e arquivos
0600 em sistemas POSIX. No Windows, as sessões usam DPAPI do usuário corrente.
O logout apaga o registro de sessão e o snapshot demonstrativo; a chave de
