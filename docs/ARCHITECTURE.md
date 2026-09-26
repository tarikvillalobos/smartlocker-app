# Arquitetura

O módulo `core` não conhece Compose, HTTP, banco ou APIs de sistema. Entidades,
contratos e funções de domínio pertencem às funcionalidades auth, parcels,
profile e shared. O aplicativo organiza dados e apresentação por funcionalidade.

`AppRuntime` é a raiz de composição e faz injeção por construtor. A seleção do
ambiente é explícita: DEMO usa `DemoRepository`; PRODUCTION usa
`ApiLockerRepository` com endpoint válido ou `UnconfiguredRepository` quando
falta configuração. Um erro nunca ativa fallback demonstrativo.
`AppController` expõe `StateFlow<AppState>` imutável;
ações iniciam coroutines e publicam novos estados. Os composables apresentam
estado e disparam ações. Sucesso exige confirmação do repositório; a releitura
posterior atualiza as consultas. Se ela falhar, o app conserva a confirmação da
operação e informa separadamente que os dados precisam ser atualizados.

## Consistência

- Início, histórico, detalhe e indicadores compartilham o mesmo estado.
- Troca de local cancela leituras e incrementa uma geração; respostas antigas
  não substituem o contexto atual. Filtros e seleção permanecem no controller.
- ViewModel retém o runtime Android durante recriação de Activity.
- Sessão expirada cancela ações/leituras e remove dados visíveis, mantendo rota,
  vínculo, seleção e filtro para recuperar o destino. Esses dados só são
  reutilizados após login do mesmo usuário e revalidação dos vínculos retornados.
- Erros preservam os metadados visíveis como desatualizados e removem códigos.
- Credenciais só são exibidas enquanto ativas, não expiradas e verificadas
  recentemente. Em produção, `revalidateAfter` define quando ocultá-las até nova
  consulta. A demonstração mantém sua janela local de 60 segundos.
- Não existe fila offline de confirmação com sucesso otimista.

## Ciclo de vida Android

A tela Android contém apenas Compose. O manifesto permite à UI tratar rotação e
mudanças de tamanho sem recriar a Activity, preservando o foco enquanto restrições
e insets são atualizados. Fonte, densidade e outras configurações não declaradas
mantêm a recriação normal; ViewModel e rememberSaveable continuam necessários.
O teste nativo inclui recriação explícita durante o OTP, além da rotação. Isso
não substitui persistência para morte do processo. Critérios e fundamento oficial
estão em [RESPONSIVE.md](RESPONSIVE.md).

## Persistência e proteção

Demonstração: snapshots versionados de dados fictícios, separados por marca e
usuário de demonstração, filtrados por vínculo e destinatário nas consultas.
`ScopedCache` oferece chaves por marca/usuário/unidade para metadados futuros.
Credenciais de retirada não são armazenadas nesse cache.

Sessões: Android usa AES-GCM com AAD por chave e Android Keystore, com backup
desativado. iOS usa Keychain `AfterFirstUnlockThisDeviceOnly`. Desktop usa
Keychain, DPAPI ou Secret Service. Não há fallback para arquivo em texto claro.
A indisponibilidade do cofre é apresentada ao usuário.

`ChunkedSecureStore` divide o registro de sessão em itens protegidos de até
1.500 caracteres. Manifesto e journal mantêm uma geração confirmada e permitem
limpar gravações interrompidas. O limite do registro é 256 KiB, sem truncamento.
A checagem de integridade das partes detecta corrupção acidental; a proteção
criptográfica permanece responsabilidade do cofre nativo.

Dados locais do desktop ficam em `~/.smartlocker`, com diretório 0700 e arquivos
0600 em sistemas POSIX. No Windows, as sessões usam DPAPI do usuário corrente.
O logout apaga o registro de sessão; na demonstração também remove o snapshot.
Em produção, tenta revogar a sessão remotamente após limpar o estado local.
Uma falha de rede não comprova revogação no servidor. A chave de criptografia
do sistema pode permanecer, sem conteúdo de sessão.

## Regras de retirada

Marcação manual e retirada física são eventos diferentes. Na demonstração,
a marcação revoga a credencial, permite desfazer por dez minutos e nunca é
apresentada como confirmação do hardware. Desfazer não reativa a credencial.
Retirada física consome a credencial, é idempotente e não permite desfazer.
No cliente HTTP, permissões de ação vêm do detalhe da encomenda e das
capacidades do vínculo e da marca. `If-Match` usa a versão previamente validada;
um conflito não provoca atualização de versão e repetição automática.

Indicadores usam a coleção completa dos últimos 30 dias no repositório demo,
independentemente da página exibida. Média inclui apenas timestamps físicos
válidos, com início inclusivo e fim exclusivo. Em produção, os indicadores vêm
de `/parcel-metrics` e preservam `complete=false` com valores desconhecidos.
Datas usam o fuso IANA do vínculo ativo; sem vínculo, a composição usa UTC.

## Cliente HTTP e sessão

`ApiSessionClient` carrega a configuração pública, normaliza login, verifica OTP
e guarda access/refresh tokens com marca, usuário, sessionId e prazos. O refresh
é serializado antes de uma requisição protegida quando o access token precisa
ser renovado. Sua chave pendente de idempotência permanece no cofre até a
confirmação. O controller usa o prazo renovável da sessão para encerrar o acesso.

`ApiLockerRepository` mapeia DTOs validados para o domínio e confere IDs de
usuário, vínculo e recurso nas respostas. Perfil combina `/me` e a lista completa
de vínculos. Identificadores são opacos; um vínculo não é o ID físico do local.
Unidades podem estar ausentes para lockers avulsos.

Avisos e solicitações expõem páginas explícitas. O estado conserva as páginas
carregadas e os próximos cursores; `unreadCount` vem do total fornecido pelo
servidor. Capacidades restringem navegação e comandos, sem conceder autorização.

## Extensão

Novas integrações devem implementar portas existentes. Não adicionar supostos
contratos de fornecedor. A lista de moradores não concede acesso às encomendas
de outra pessoa. Flags escondem e bloqueiam rotas, mas não substituem autorização.
