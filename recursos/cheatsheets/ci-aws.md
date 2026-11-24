# Cheatsheet: GitHub Actions e AWS CLI (J10)

## Actions
```yaml
on: { pull_request: , push: { branches: [main] } }
permissions: { contents: read }
concurrency: { group: ci-${{ github.workflow }}-${{ github.ref }}, cancel-in-progress: ${{ github.event_name == 'pull_request' }} }
jobs.<id>.runs-on: ubuntu-24.04      timeout-minutes: 20      needs: [outro-job]      environment: production
steps: - uses: dono/action@<SHA> # vX.Y.Z      - run: ./mvnw -B -ntp verify      - env: { X: ${{ ... }} }   # dado de terceiro SEMPRE por env
if: ${{ !cancelled() }}    # roda mesmo se um passo anterior falhou
```
`actionlint` (`docker run --rm -v "$PWD":/repo -w /repo rhysd/actionlint:1.7.12 -no-color .github/workflows/*.yml`) · `$GITHUB_STEP_SUMMARY` · `$GITHUB_OUTPUT`.

## AWS CLI
| Tarefa | Comando |
|---|---|
| Quem sou eu | `aws sts get-caller-identity` |
| Entrar (credenciais temporárias) | `aws login` |
| Região padrão | `aws configure set region us-east-1` |
| Tipos gratuitos da conta | `aws ec2 describe-instance-types --filters Name=free-tier-eligible,Values=true --query 'InstanceTypes[].InstanceType' --output text` |
| Listar por tag | `aws ec2 describe-instances --filters Name=tag:project,Values=payflow` |
| Instância no SSM | `aws ssm describe-instance-information` |
| Túnel | `aws ssm start-session --target ID --document-name AWS-StartPortForwardingSession --parameters '{"portNumber":["8080"],"localPortNumber":["8080"]}'` |
| Parâmetro seguro | `aws ssm put-parameter --name /payflow/x --type SecureString --value ...` |
| Esperar | `aws rds wait db-instance-available --db-instance-identifier ID` |
| Destruir e provar | `CONFIRM=payflow bash scripts/aws/90-teardown.sh && bash scripts/aws/91-verify-empty.sh` |
