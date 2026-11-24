# Cheatsheet: segurança intermediária (P10)

> Referência rápida. **Executado** contra o Keycloak 26.8.2, o PostgreSQL 18, o Trivy 0.76.2 e o cosign 3.1.4 (laboratório `2-pleno/P10-seguranca-intermediaria/lab`); veja as aulas P10.01–P10.07 para o que **não** foi executado.

## Tokens (Keycloak local)
```bash
B=http://127.0.0.1:8180/realms/payflow/protocol/openid-connect
# máquina → máquina (Client Credentials)
curl -s $B/token -d grant_type=client_credentials -d client_id=payflow-notifications -d client_secret=$SECRET -d scope=internal:notify
# decodificar um JWT (sem validar nada!)
python3 -c "import sys,json,base64;[print(json.dumps(json.loads(base64.urlsafe_b64decode(p+'='*(-len(p)%4))),indent=1)) for p in sys.stdin.read().strip().split('.')[:2]]" < token.txt
# o que o servidor publica
curl -s http://127.0.0.1:8180/realms/payflow/.well-known/openid-configuration | jq '{issuer,jwks_uri,code_challenge_methods_supported}'
```
| Claim | Conferir na API |
|---|---|
| `iss` | emissor (`issuer-uri`) |
| `aud` | `payflow-api` (`audiences`) |
| `typ` | `Bearer` (access) — recusar `ID` e `Refresh` |
| `exp`/`nbf` | tolerância de relógio (padrão 60 s) |
| `sub` | **dono** do objeto (nunca do corpo) |
| `scope` → `SCOPE_x` · `realm_access.roles` → `ROLE_x` | autorização |
| `azp` | **qual cliente** chamou (serviço × app) |

## Resource Server (Spring Boot 4.1 / Security 7.1)
```yaml
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: https://idp.example/realms/payflow     # descoberta preguiçosa (a API sobe sem o IdP)
  audiences: payflow-api
  jws-algorithms: RS256
```
`401` = não autenticado (`invalid_token`) · `403` = sem escopo/papel (`insufficient_scope`) · `404` = objeto alheio (IDOR) · compare `WWW-Authenticate` com `startsWith("Bearer")` (vem `resource_metadata`, RFC 9728). Keycloak **nunca resolvido** + token → `403` vazio (converta `JwtDecoderInitializationException` em **503**).

## Fluxos (o que cada parâmetro impede)
`code_challenge`/`code_verifier` (PKCE) → código roubado · `redirect_uri` exato → código desviado · `state` → CSRF do fluxo · `nonce` → ID token reaproveitado · `iss` (RFC 9207) → confusão de servidor.

## Rotação de credenciais do banco (dois usuários alternados)
```sql
CREATE ROLE payflow_app NOLOGIN;                                 -- permissões NO GRUPO
CREATE ROLE app_a LOGIN PASSWORD '…' IN ROLE payflow_app;        -- usuários que se alternam
CREATE ROLE app_b LOGIN PASSWORD '…' IN ROLE payflow_app;
GRANT SELECT, INSERT, UPDATE ON accounts TO payflow_app;
GRANT SELECT, INSERT ON ledger_entries TO payflow_app;           -- sem UPDATE/DELETE: regra 5 no banco
ALTER ROLE app_b PASSWORD '…';                                   -- 1) troca o INATIVO
-- 2) o pool passa a usar app_b (conexões NOVAS) · 3) deploy · 4) espera a janela
ALTER ROLE app_a PASSWORD '…nunca-entregue…';                    -- 5) invalida o antigo
SELECT usename, count(*) FROM pg_stat_activity GROUP BY 1;       -- 6) ninguém usa app_a
```
Janela ≥ releitura do segredo + duração do deploy + `max-lifetime` do pool. Usuário único = quebra **depois** (só conexões novas conferem a senha).

## Cadeia de suprimentos
```bash
mvn org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom                 # target/sbom.json (CycloneDX 1.6)
jq -r '.components[] | select(.name=="log4j-core") | "\(.name) \(.version) \(.purl)"' target/sbom.json
trivy sbom sbom.json --severity HIGH,CRITICAL --exit-code 1                      # PLANTE uma falha antes de confiar no 0
trivy image --input image.tar --format cyclonedx --output image-sbom.json        # SBOM da IMAGEM (SO + jars)
# cosign 3.x (laboratório, sem registro de transparência; em produção use um)
cosign generate-key-pair
cosign sign   --key cosign.key --use-signing-config=false --tlog-upload=false --yes REPO@sha256:…
cosign verify --key cosign.pub --insecure-ignore-tlog=true REPO@sha256:…          # por DIGEST, nunca por tag
cosign attest --key cosign.key --use-signing-config=false --tlog-upload=false --yes --type cyclonedx --predicate sbom.json REPO@sha256:…
```
Portão: `0` assinado · `1` sem assinatura válida · `2` referência por tag. JAR reprodutível: `<project.build.outputTimestamp>`.

## STRIDE por elemento
| Elemento | Letras |
|---|---|
| Entidade externa | S R |
| Processo | S T R I D E |
| Armazenamento | T R I D |
| Fluxo de dados | T I D |

S autenticidade · T integridade · R não repúdio · I confidencialidade · D disponibilidade · E autorização. Mitigada ⇒ **evidência verificável**; aceita/aberta ⇒ **dono + data**.
