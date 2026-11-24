#!/usr/bin/env python3
"""Mutantes do Resource Server: estraga UMA regra de segurança por vez e confere que algum teste do ResourceServerTest FALHA.
Um teste que nunca falhou não prova nada (lição da J8). Roda numa cópia temporária; o laboratório não é alterado."""
import os, re, shutil, subprocess, sys, tempfile

LAB = os.path.join(os.path.dirname(os.path.abspath(__file__)), "identity-lab")
ENV = dict(os.environ, JAVA_HOME="/usr/lib/jvm/java-25-openjdk-amd64")

def patch(path, old, new, count=1):
    s = open(path).read()
    assert old in s, f"pattern not found in {path}: {old[:60]}"
    open(path, "w").write(s.replace(old, new, count))

MUTANTS = [
    ("M1 sem `audiences`", "src/main/resources/application.yml",
     "          audiences: payflow-api                  # o token precisa ter aud = payflow-api\n", ""),
    ("M2 sem `jws-algorithms`", "src/main/resources/application.yml",
     "          jws-algorithms: RS256                   # só RS256 (nunca \"none\", nunca HS256 com a chave pública)\n", ""),
    ("M3 sem o validador de `typ`", "src/main/java/com/payflow/security/SecurityConfig.java",
     "return new TokenTypeValidator();", "return jwt -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();"),
    ("M4a regra da cadeia para /admin frouxa (authenticated), fica o @PreAuthorize", "src/main/java/com/payflow/security/SecurityConfig.java",
     '.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")', '.requestMatchers("/api/v1/admin/**").authenticated()'),
    ("M4b regra frouxa E sem o @PreAuthorize", None, None, None),
    ("M5 sem a lista de clientes (azp) no /internal", "src/main/java/com/payflow/security/SecurityConfig.java",
     "return hasScope && knownClient;", "return hasScope;"),
    ("M6 sem o filtro de dono (IDOR)", "src/main/java/com/payflow/web/ApiController.java",
     "return accounts.find(id).filter(a -> a.ownerId().equals(me))\n                .map(ResponseEntity::ok)", "return accounts.find(id)\n                .map(ResponseEntity::ok)"),
    ("M7 escopo de escrita não exigido", "src/main/java/com/payflow/security/SecurityConfig.java",
     '.requestMatchers(HttpMethod.POST, "/api/v1/transfers").hasAuthority("SCOPE_transfers:write")', '.requestMatchers(HttpMethod.POST, "/api/v1/transfers").authenticated()'),
]

def run(copy):
    p = subprocess.run(["mvn", "-B", "-ntp", "-q", "test", "-Dtest=ResourceServerTest"], cwd=copy, env=ENV,
                       capture_output=True, text=True, timeout=280)
    out = p.stdout + p.stderr
    failed = sorted(set(re.findall(r"ResourceServerTest\.(\w+)(?::\d+)? ", out) + re.findall(r"ResourceServerTest\.(\w+) --", out)))
    return p.returncode, [f for f in failed]

def main():
    results = []
    for name, rel, old, new in MUTANTS:
        tmp = tempfile.mkdtemp(prefix="p10-mutant-")
        copy = os.path.join(tmp, "identity-lab")
        shutil.copytree(LAB, copy, ignore=shutil.ignore_patterns("target"))
        if rel is None:                                    # M4b: duas edições
            patch(os.path.join(copy, "src/main/java/com/payflow/security/SecurityConfig.java"),
                  '.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")', '.requestMatchers("/api/v1/admin/**").authenticated()')
            patch(os.path.join(copy, "src/main/java/com/payflow/web/ApiController.java"),
                  "    @PreAuthorize(\"hasRole('ADMIN')\")\n", "")
        else:
            patch(os.path.join(copy, rel), old, new)
        code, failed = run(copy)
        verdict = "KILLED (a suíte falhou)" if code != 0 else "SURVIVED (a suíte continuou verde)"
        print(f"{name}: {verdict}" + (f" -> {', '.join(failed[:4])}" if failed else ""))
        sys.stdout.flush()
        shutil.rmtree(tmp, ignore_errors=True)
        results.append((name, code != 0))
    print("mutants killed: %d/%d" % (sum(1 for _, k in results if k), len(results)))

if __name__ == "__main__":
    main()
