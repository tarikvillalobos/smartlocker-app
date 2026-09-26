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
