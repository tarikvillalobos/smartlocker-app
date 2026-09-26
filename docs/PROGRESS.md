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
- [x] Builds Android/iOS e testes compartilhados executados.
- [x] Documentação de arquitetura, API, marcas, execução e distribuição.
- [x] Base OpenAPI 3.1.1 proposta, validada e documentada, solicitada pelo usuário.
- [x] Regressões de sessão/local, paginação, contato, avisos e concorrência corrigidas.
- [x] Sete testes nativos Android aprovados, incluindo Keystore e fluxo com rotação.
- [x] DMG local gerado; workflow de pacotes desktop das duas marcas e iOS configurado.
- [ ] API real: implementação externa, credenciais e homologação ainda indisponíveis.
- [ ] Integrações reais com locker, push, SMS, e-mail e WhatsApp.
- [ ] Ensaios em aparelhos físicos, TalkBack/VoiceOver e cofres Windows/Linux.
- [ ] Assinatura, notarização e distribuição para lojas.

Não há backend próprio nem fallback automático para demo. Os endpoints do OpenAPI
são uma proposta autorizada, não uma integração com serviço existente.
A implementação independente está entregue; o produto não está pronto para produção.
