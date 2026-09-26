# Base OpenAPI proposta para SmartLocker

A especificação em [`api/openapi.yaml`](api/openapi.yaml) foi criada como base de
integração do aplicativo. O cliente HTTP correspondente já está implementado;
o backend está sendo finalizado em outro projeto. A versão segue `0.1.0-draft`,
em OpenAPI 3.1.1/JSON Schema 2020-12, até homologação conjunta.

O host `.example.invalid` continua deliberadamente não operacional. Definir um
contrato e testar o cliente com respostas controladas não demonstra serviço
publicado, entrega de mensagens ou integração com hardware. Configuração e
limites da implementação atual estão em [API.md](API.md).

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
   A média usa segundos e só inclui retirada física com dados completos.
6. Paginação usa snapshot estável e cursor opaco vinculado a marca, usuário,
   vínculo, filtro e ordenação. Expiração retorna 410 e exige nova primeira página.
   Métricas e unreadCount são globais ao vínculo, nunca derivadas da página.
7. Credenciais só são devolvidas quando ativas; QR usa o payload exato do servidor.
   `revalidateAfter` determina quando ocultar o código sem nova consulta. Manual
   revoga código; desfazer jamais reativa; físico confirmado consome a credencial.
8. Mutações especificadas exigem UUID Idempotency-Key, retenção proposta de 24h.
   Antes do login, namespace estável é marca+operação+caminho+chave; o digest do
   corpo é comparado após lookup, permitindo 409 para chave reutilizada com outro
   corpo. Depois do login, o escopo também inclui o sujeito autenticado.
   Replay de tokens não prolonga prazos nem restaura sessão revogada/substituída.
   Seu cache exige criptografia em repouso, acesso restrito, ausência de logs e
   remoção do material secreto ao expirar/revogar a sessão ou vencer o TTL.
9. If-Match com a versão da encomenda protege marcação/reversão concorrentes.
   Uma reprodução idempotente é resolvida antes de comparar If-Match.
10. Erros usam application/problem+json, código estável e requestId seguro.
    Nenhum corpo contém logs internos ou eco de CPF, contatos, OTP ou credenciais.
11. Preferência, permissão do sistema e disponibilidade de canal são independentes.
    Push leva apenas IDs para navegação; o app reautentica e consulta a API.
12. FCM/APNs são propostas explícitas, sem integração implementada ou segredo de
    fornecedor embarcado. Capacidades podem desabilitar o registro por completo.
    X-Installation-Key propõe prova de posse local: 32 bytes CSPRNG no cofre, hash
    vinculado no servidor. Trocar destinatário exige a mesma prova; conhecer apenas
    installationId não autoriza reassociação. Logout não apaga essa proteção.

## Mapeamento implementado no cliente


## Lacunas e escolhas que exigem homologação

URLs e credenciais de ambientes; valores de TTL e rate limit; cadastro e recuperação
de identidade; validade do OTP; política de canais; política de undo; emissão e
renovação de credenciais; permissões exatas; limites/retencão de idempotência;
protocolo confiável dos eventos físicos; processos de suporte; fornecedor push e
seus certificados/chaves de backend. Nem este documento nem sua validação provam
implementação ou segurança do servidor futuro.

## Validação realizada

- openapi-spec-validator 0.7.2: OpenAPI 3.1.1 válido.
- YAML com chaves únicas; 453 referências locais resolvidas, sem referências remotas.
- JSON Schema: schemas válidos e oito exemplos válidos.
- 21 casos positivos/negativos de schema: CPF, telefone, canal, e-mail, OTP,
  preferências, métricas incompletas, UTC e combinação plataforma/provedor push.
- Máximo de 137 caracteres por linha; sem whitespace final.
- Nenhuma chamada a backend/hardware, nenhum teste de homologação.

## Executar a validação

```sh
python3 -m venv .tools/openapi
.tools/openapi/bin/python -m pip install -r scripts/openapi-requirements.txt
.tools/openapi/bin/python scripts/validate_openapi.py
.tools/openapi/bin/python scripts/check_openapi_cases.py
```

No Windows, use `.tools/openapi/Scripts/python.exe`. A CI executa as mesmas
verificações em cada push. Não é preciso iniciar nem acessar um servidor.

Referências normativas: [OpenAPI 3.1.1](https://spec.openapis.org/oas/v3.1.1.html)
e [Problem Details RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html).
