#!/usr/bin/env python3
"""Cria, pela Admin REST API, os escopos próprios do PayFlow e os liga aos clientes (idempotente).
Por que não no JSON do realm: um `clientScopes` na importação SUBSTITUI os escopos embutidos
(basic, roles, profile, email): o token perdeu `sub` e `realm_access` (medido nesta aula)."""
import json, urllib.request, urllib.parse, sys

BASE = "http://127.0.0.1:8180"

def call(method, path, body=None, token=None, form=None):
    data = None; headers = {}
    if form is not None:
        data = urllib.parse.urlencode(form).encode()
    elif body is not None:
        data = json.dumps(body).encode(); headers["Content-Type"] = "application/json"
    if token: headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

_, tok = call("POST", "/realms/master/protocol/openid-connect/token",
              form={"grant_type": "password", "client_id": "admin-cli", "username": "admin", "password": "lab-admin-only"})
T = tok["access_token"]
R = "/admin/realms/payflow"

SCOPES = {
    "payflow-api-audience": {"mapper": True, "type": "default"},
    "transfers:read": {"type": "optional"},
    "transfers:write": {"type": "optional"},
    "internal:notify": {"type": "optional"},
}
_, existing = call("GET", R + "/client-scopes", token=T)
have = {s["name"]: s["id"] for s in existing}
for name, cfg in SCOPES.items():
    if name in have:
        continue
    body = {"name": name, "protocol": "openid-connect",
            "attributes": {"include.in.token.scope": "false" if cfg.get("mapper") else "true",
                           "display.on.consent.screen": "false" if name in ("payflow-api-audience", "internal:notify") else "true"}}
    if cfg.get("mapper"):
        body["protocolMappers"] = [{"name": "aud-payflow-api", "protocol": "openid-connect",
                                    "protocolMapper": "oidc-audience-mapper",
                                    "config": {"included.custom.audience": "payflow-api",
                                               "id.token.claim": "false", "access.token.claim": "true"}}]
    code, _ = call("POST", R + "/client-scopes", body, T)
    print("create scope", name, code)
_, existing = call("GET", R + "/client-scopes", token=T)
have = {s["name"]: s["id"] for s in existing}

_, clients = call("GET", R + "/clients", token=T)
cid = {c["clientId"]: c["id"] for c in clients}
PLAN = {
    "payflow-web": {"default": ["payflow-api-audience"], "optional": ["transfers:read", "transfers:write"]},
    "payflow-lab-direct": {"default": ["payflow-api-audience"], "optional": ["transfers:read", "transfers:write"]},
    "payflow-notifications": {"default": ["payflow-api-audience"], "optional": ["internal:notify"]},
    "payflow-reporting": {"default": ["payflow-api-audience"], "optional": []},
}
for client, plan in PLAN.items():
    for kind in ("default", "optional"):
        for s in plan[kind]:
            code, _ = call("PUT", f"{R}/clients/{cid[client]}/{kind}-client-scopes/{have[s]}", token=T)
            print(f"{client}: {kind} scope {s} -> {code}")
