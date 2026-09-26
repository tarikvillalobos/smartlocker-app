# Base OpenAPI proposta para SmartLocker

A minuta em [`api/openapi.yaml`](api/openapi.yaml) foi criada a pedido do usuário como base de um
contrato que ainda não existe. Ela não descreve uma API já fornecida, não cria
servidor e não conecta o aplicativo. O host `.example.invalid` é deliberadamente
não operacional. Versão `0.1.0-draft`, formato OpenAPI 3.1.1/JSON Schema 2020-12.

## Cobertura

- Configuração pública da marca e capacidades efetivas por vínculo.
- Login por OTP SMS/e-mail, reenvio, verificação, refresh rotativo e logout.
- Perfil, lista completa de vínculos, preferências e troca verificada de contato.
- Destinatários visíveis conforme autorização, sem acesso implícito às encomendas.
- Histórico paginado por cursor, métricas completas, detalhes e credencial QR.
- Marcação manual e reversão permitida; retirada física somente como estado lido.
- Avisos paginados, contador global de não lidos e confirmação de leitura.
- Criação idempotente, lista e acompanhamento de solicitações de suporte.
- Registro push opcional para Android/FCM e iOS/APNs, condicionado às capacidades.

São 23 caminhos, 26 operações e 38 schemas. Não há endpoint de abertura de
porta, depósito, envio direto a fornecedor nem ingestão de evento de hardware
pelo usuário. Uma API operacional de hardware será outro contrato, autenticado
para esse papel; esta minuta define apenas o cliente de usuários finais.

## Decisões propostas

1. Bearer opaco em HTTPS; access token, refresh token e prazos explícitos.
   Token vinculado à marca e ao usuário. Refresh é rotativo e serializado pelo
   cliente; OTP e CPF não são tokens reutilizáveis. O contrato não presume OAuth.
2. `X-Brand-Id` é público e não autoriza nada. As permissões `x-required-permission`
   documentam política de aplicação; o servidor valida autorização em cada acesso.
   Recurso alheio usa 404, evitando confirmar sua existência.
3. Login por SMS exige celular brasileiro E.164; e-mail exige endereço válido.
   CPF tem onze dígitos, rejeita repetição uniforme no schema e continua exigindo
   dígitos verificadores no servidor. OTP proposto tem seis dígitos.
4. Reenvio usa o contato do desafio no servidor. O servidor informa expiresAt e
   resendAt; respostas de solicitação não revelam existência de conta nem garantem
   que o fornecedor entregou a mensagem. Cada desafio tem propósito explícito.
5. Datas RFC 3339 UTC com sufixo Z; o vínculo fornece o fuso IANA para apresentação.
   Métricas usam intervalo [since, until), com padrão de 30 dias quando omitido.
