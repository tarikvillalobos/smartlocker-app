# Registro de implementação

Atualizado em 26/09/2026. Consulte [VALIDATION.md](VALIDATION.md) para as evidências.

- [x] Cinco templates do HTML extraídos, inspecionados e renderizados.
- [x] Commits auditáveis: exatamente um arquivo e até 20 linhas por commit.
- [x] Domínio independente de UI/HTTP, casos de uso e repositórios demonstrativos.
- [x] Cinco telas Compose e fluxos auxiliares de autenticação, contatos e suporte.
- [x] QR decodificável, retirada manual distinta de confirmação física simulada.
- [x] Persistência demonstrativa, isolamento, sessão e tratamento de erros.
- [x] Serviços nativos e duas marcas, com logos, canais e rotas opcionais.
- [x] Layout adaptativo, fonte a 200%, paisagem e redimensionamento.
- [x] 35 testes compartilhados/desktop aprovados; 68 capturas de layout geradas.
- [x] Dois testes de cofres nativos aprovados em macOS, Windows e Linux.
- [x] Base OpenAPI 3.1.1 proposta, oito exemplos e 21 casos de schema validados.
- [x] Regressões de sessão/local, paginação, contato, avisos e concorrência corrigidas.
- [x] Sete testes Android ampliados aprovados: Keystore, IME real, rotação e recriação.
- [x] Três testes iOS aprovados em simulador isolado, incluindo UI/teclado e Keychain.
- [x] DMG, MSI e DEB das duas marcas gerados em CI; DMG também executado localmente.
- [x] Documentação de arquitetura, API, marcas, execução e distribuição.
- [ ] API real: implementação externa, credenciais e homologação indisponíveis.
- [ ] Integrações reais com locker, push, SMS, e-mail e WhatsApp.
- [ ] Ensaios em aparelhos físicos e auditoria com TalkBack/VoiceOver.
- [ ] Assinatura comercial, notarização e distribuição para lojas.

Não há backend próprio nem fallback automático para demo. Os endpoints do OpenAPI
são uma proposta autorizada, não uma integração com serviço existente.
