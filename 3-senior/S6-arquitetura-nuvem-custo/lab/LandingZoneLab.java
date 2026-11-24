import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * S6.05 — Landing zone: SCPs como guardrails, avaliadas por um MODELO da lógica documentada
 * (SCP em todos os níveis da raiz até a conta; negação explícita vence; SCP não concede; conta de gestão e
 * papéis vinculados a serviço não são afetados). Não é a AWS: subconjunto de ações e condições, sem RCP,
 * sem políticas de recurso nem limites de permissão. As SCPs são os JSON de samples/scp (os mesmos da entrega).
 */
public class LandingZoneLab {

    // ------------------------------------------------------------ organização
    record Node(String name, Node parent, List<String> scps) {}

    static final Map<String, Node> OUS = new LinkedHashMap<>();
    static final Map<String, Node> ACCOUNTS = new LinkedHashMap<>();
    static final String MANAGEMENT = "management";

    static void buildOrg() {
        OUS.clear(); ACCOUNTS.clear();
        Node root = ou("Root", null, "FullAWSAccess", "deny-leave-org", "deny-root-user", "deny-outside-regions", "protect-audit");
        Node security = ou("Security", root, "FullAWSAccess");
        Node backup = ou("Backup", root, "FullAWSAccess");
        Node workloads = ou("Workloads", root, "FullAWSAccess", "require-cost-tags");
        Node prod = ou("Prod", workloads, "FullAWSAccess");
        Node nonProd = ou("NonProd", workloads, "FullAWSAccess");
        Node sandbox = ou("Sandbox", root, "FullAWSAccess", "sandbox-limits");
        Node suspended = ou("Suspended", root, "deny-all"); // toda entidade precisa de >= 1 SCP (cota do Organizations)
        account("log-archive", security, "FullAWSAccess");
        account("audit", security, "FullAWSAccess");
        account("payflow-backup", backup, "FullAWSAccess", "protect-backup");
        account("payflow-prod", prod, "FullAWSAccess");
        account("payflow-staging", nonProd, "FullAWSAccess");
        account("study", sandbox, "FullAWSAccess");
        account("old-poc", suspended, "FullAWSAccess");
    }

    static Node ou(String name, Node parent, String... scps) {
        Node n = new Node(name, parent, new ArrayList<>(List.of(scps)));
        OUS.put(name, n);
        return n;
    }

    static void account(String name, Node parent, String... scps) {
        ACCOUNTS.put(name, new Node(name, parent, new ArrayList<>(List.of(scps))));
    }

    // ------------------------------------------------------------ políticas
    static final Map<String, String> SCP_TEXT = new LinkedHashMap<>();
    static final Map<String, Object> SCP = new LinkedHashMap<>();
    static final Map<String, Object> IDENTITY = new LinkedHashMap<>();

    static void loadPolicies(UnaryOperator<Map<String, String>> mutate) throws Exception {
        SCP_TEXT.clear(); SCP.clear();
        try (var s = Files.list(Path.of("samples/scp"))) {
            for (Path p : s.sorted().toList())
                SCP_TEXT.put(p.getFileName().toString().replace(".json", ""), Files.readString(p));
        }
        mutate.apply(SCP_TEXT).forEach((k, v) -> SCP.put(k, Json.parse(v)));
        IDENTITY.put("admin", Json.parse("{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]}"));
        IDENTITY.put("payflow-breakglass", IDENTITY.get("admin"));
        IDENTITY.put("developer", Json.parse("""
                {"Statement":[{"Effect":"Allow","Resource":"*",
                  "Action":["ec2:*","ecs:*","rds:*","logs:*","cloudwatch:*","s3:GetObject","s3:ListBucket"]}]}"""));
    }

    // ------------------------------------------------------------ avaliação
    record Request(String account, String role, boolean serviceLinked, String action, Map<String, String> ctx) {}

    record Decision(boolean allowed, String reason) {}

    static Decision evaluate(Request r) {
        Node acc = ACCOUNTS.get(r.account());
        boolean scpsApply = acc != null && !r.serviceLinked();
        if (scpsApply) {
            List<Node> path = new ArrayList<>();
            for (Node n = acc; n != null; n = n.parent()) path.addFirst(n);
            for (Node level : path)
                for (String name : level.scps())
                    if (matches((Map<?, ?>) SCP.get(name), "Deny", r)) return new Decision(false, "explicit deny in SCP " + name + " @" + level.name());
            for (Node level : path) {
                boolean allowedHere = level.scps().stream().anyMatch(n -> matches((Map<?, ?>) SCP.get(n), "Allow", r));
                if (!allowedHere) return new Decision(false, "no SCP Allow @" + level.name() + " (implicit deny)");
            }
        }
        if (r.serviceLinked()) return new Decision(true, "service-linked role: SCPs do not apply");
        Map<?, ?> id = (Map<?, ?>) IDENTITY.get(r.role());
        if (id == null || matches(id, "Deny", r)) return new Decision(false, "identity policy denies");
        if (!matches(id, "Allow", r)) return new Decision(false, "identity policy does not allow (SCPs never grant)");
        return new Decision(true, scpsApply ? "allowed by identity, inside every SCP" : "management account: SCPs do not apply");
    }

    static boolean matches(Map<?, ?> policy, String effect, Request r) {
        for (Object o : (List<?>) policy.get("Statement")) {
            Map<?, ?> st = (Map<?, ?>) o;
            if (!effect.equals(st.get("Effect"))) continue;
            boolean actionOk = st.containsKey("Action") ? anyAction(st.get("Action"), r.action()) : !anyAction(st.get("NotAction"), r.action());
            if (actionOk && conditions((Map<?, ?>) st.get("Condition"), r.ctx())) return true;
        }
        return false;
    }

    static boolean anyAction(Object spec, String action) {
        List<?> list = spec instanceof List<?> l ? l : List.of(spec);
        return list.stream().anyMatch(a -> wildcard(a.toString(), action, true));
    }

    static boolean wildcard(String pattern, String value, boolean ignoreCase) {
        String regex = Pattern.quote(pattern).replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q");
        return Pattern.compile(regex, ignoreCase ? Pattern.CASE_INSENSITIVE : 0).matcher(value).matches();
    }

    /** Todas as condições do bloco precisam valer. Chave ausente: operador positivo = falso; negado = verdadeiro (docs do IAM). */
    static boolean conditions(Map<?, ?> cond, Map<String, String> ctx) {
        if (cond == null) return true;
        for (var op : cond.entrySet()) {
            for (var kv : ((Map<?, ?>) op.getValue()).entrySet()) {
                String key = kv.getKey().toString(), actual = ctx.get(key);
                List<String> wanted = (kv.getValue() instanceof List<?> l ? l : List.of(kv.getValue())).stream().map(Object::toString).toList();
                boolean ok = switch (op.getKey().toString()) {
                    case "StringEquals" -> actual != null && wanted.contains(actual);
                    case "StringNotEquals" -> actual == null || !wanted.contains(actual);
                    case "StringLike", "ArnLike" -> actual != null && wanted.stream().anyMatch(w -> wildcard(w, actual, false));
                    case "StringNotLike", "ArnNotLike" -> actual == null || wanted.stream().noneMatch(w -> wildcard(w, actual, false));
                    case "Null" -> (actual == null) == Boolean.parseBoolean(wanted.getFirst());
                    default -> throw new IllegalArgumentException("operador não modelado: " + op.getKey());
                };
                if (!ok) return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------ casos
    record Case(int id, String desc, Request req, boolean expected) {}

    static Request req(String account, String role, String action, String region, String... kv) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("aws:RequestedRegion", region);
        String acctId = String.valueOf(100000000000L + Math.abs(account.hashCode() % 1000));
        ctx.put("aws:PrincipalArn", role.equals("root") ? "arn:aws:iam::" + acctId + ":root" : "arn:aws:iam::" + acctId + ":role/" + role);
        for (int i = 0; i < kv.length; i += 2) ctx.put(kv[i], kv[i + 1]);
        return new Request(account, role, role.equals("AWSServiceRoleForECS"), action, ctx);
    }

    static List<Case> cases() {
        return List.of(
                new Case(1, "dev updates the ECS service in prod", req("payflow-prod", "developer", "ecs:UpdateService", "us-east-1"), true),
                new Case(2, "dev launches an instance in eu-west-1", req("payflow-prod", "developer", "ec2:RunInstances", "eu-west-1", "aws:RequestTag/cost-center", "CC-1001", "ec2:InstanceType", "t4g.small"), false),
                new Case(3, "admin creates an IAM role (global, us-east-1)", req("payflow-prod", "admin", "iam:CreateRole", "us-east-1"), true),
                new Case(4, "admin stops CloudTrail in prod", req("payflow-prod", "admin", "cloudtrail:StopLogging", "us-east-1"), false),
                new Case(5, "admin stops CloudTrail in the MANAGEMENT account", req(MANAGEMENT, "admin", "cloudtrail:StopLogging", "us-east-1"), true),
                new Case(6, "admin schedules deletion of the backup key", req("payflow-backup", "admin", "kms:ScheduleKeyDeletion", "us-east-1"), false),
                new Case(7, "break-glass schedules deletion of the backup key", req("payflow-backup", "payflow-breakglass", "kms:ScheduleKeyDeletion", "us-east-1"), true),
                new Case(8, "dev launches in staging without cost-center", req("payflow-staging", "developer", "ec2:RunInstances", "sa-east-1", "ec2:InstanceType", "t4g.small"), false),
                new Case(9, "dev launches in staging with cost-center", req("payflow-staging", "developer", "ec2:RunInstances", "sa-east-1", "aws:RequestTag/cost-center", "CC-1001", "ec2:InstanceType", "t4g.small"), true),
                new Case(10, "dev launches m7i.24xlarge in the sandbox", req("study", "developer", "ec2:RunInstances", "us-east-1", "ec2:InstanceType", "m7i.24xlarge"), false),
                new Case(11, "dev launches t4g.small in the sandbox", req("study", "developer", "ec2:RunInstances", "us-east-1", "ec2:InstanceType", "t4g.small"), true),
                new Case(12, "service-linked role acts in eu-west-1", req("payflow-prod", "AWSServiceRoleForECS", "ec2:CreateNetworkInterface", "eu-west-1"), true),
                new Case(13, "dev deletes a bucket (SCP allows, identity does not)", req("payflow-prod", "developer", "s3:DeleteBucket", "us-east-1"), false),
                new Case(14, "root user of a member account reads S3", req("payflow-prod", "root", "s3:GetObject", "us-east-1"), false),
                new Case(15, "admin tries to leave the organization", req("payflow-staging", "admin", "organizations:LeaveOrganization", "us-east-1"), false),
                new Case(16, "admin in a suspended account reads S3", req("old-poc", "admin", "s3:GetObject", "us-east-1"), false),
                new Case(17, "admin deletes the trail in log-archive", req("log-archive", "admin", "cloudtrail:DeleteTrail", "us-east-1"), false));
    }

    static int run(boolean print) {
        int wrong = 0;
        for (Case c : cases()) {
            Decision d = evaluate(c.req());
            boolean ok = d.allowed() == c.expected();
            if (!ok) wrong++;
            if (print) System.out.printf("%2d %-4s %-5s %-50s %s%n", c.id(), ok ? "ok" : "FAIL", d.allowed() ? "ALLOW" : "DENY", c.desc(), d.reason());
        }
        return wrong;
    }

    /** Regras estruturais das cotas do Organizations: >= 1 e <= 10 SCPs anexadas em cada raiz, OU e conta. */
    static List<String> validateOrg() {
        List<String> problems = new ArrayList<>();
        List<Node> all = new ArrayList<>(OUS.values());
        all.addAll(ACCOUNTS.values());
        for (Node n : all) {
            if (n.scps().isEmpty()) problems.add(n.name() + " has no SCP attached (Organizations requires at least one)");
            if (n.scps().size() > 10) problems.add(n.name() + " has " + n.scps().size() + " SCPs (max 10)");
        }
        return problems;
    }

    record Mutant(String id, String desc, UnaryOperator<Map<String, String>> change, Runnable orgChange) {}

    public static void main(String[] args) throws Exception {
        buildOrg();
        loadPolicies(m -> m);
        System.out.println("Organization: " + OUS.keySet() + "; accounts " + ACCOUNTS.keySet() + " + " + MANAGEMENT);
        List<String> structure = validateOrg();
        System.out.println(structure.isEmpty() ? "Structure: every root, OU and account has 1 to 10 SCPs" : "Structure problems: " + structure);
        int wrong = run(true);
        System.out.printf("RESULT: %d/%d cases as expected%n%n", cases().size() - wrong, cases().size());

        List<Mutant> mutants = List.of(
                new Mutant("V0", "variant: only sa-east-1, global-service exception kept", m -> replace(m, "deny-outside-regions", "\"us-east-1\", ", ""), () -> {}),
                new Mutant("M1", "only sa-east-1 and NO global-service exception (NotAction -> Action *)", m -> replace(replace(m, "deny-outside-regions", "\"us-east-1\", ", ""),
                        "deny-outside-regions", "\"NotAction\": [\"cloudfront:*\", \"iam:*\", \"route53:*\", \"support:*\"]", "\"Action\": \"*\""), () -> {}),
                new Mutant("M2", "protect-backup without the break-glass exception", m -> replace(m, "protect-backup",
                        ",\n      \"Condition\": { \"ArnNotLike\": { \"aws:PrincipalArn\": \"arn:aws:iam::*:role/payflow-breakglass\" } }", ""), () -> {}),
                new Mutant("M3", "FullAWSAccess detached from the Workloads OU", m -> m, () -> OUS.get("Workloads").scps().remove("FullAWSAccess")),
                new Mutant("M4", "protect-audit attached to Workloads instead of Root", m -> m, () -> {
                    OUS.get("Root").scps().remove("protect-audit");
                    OUS.get("Workloads").scps().add("protect-audit");
                }),
                new Mutant("M5", "Suspended OU with no SCP at all (my first design)", m -> m, () -> OUS.get("Suspended").scps().clear()));
        System.out.println("Mutants and variants (a guardrail test that never fails proves nothing):");
        for (Mutant mu : mutants) {
            buildOrg();
            mu.orgChange().run();
            loadPolicies(mu.change());
            List<String> invalid = validateOrg();
            if (!invalid.isEmpty()) {
                System.out.printf("  %s %-72s -> REJECTED: %s%n", mu.id(), mu.desc(), invalid.getFirst());
                continue;
            }
            List<Integer> flipped = new ArrayList<>();
            for (Case c : cases()) if (evaluate(c.req()).allowed() != c.expected()) flipped.add(c.id());
            System.out.printf("  %s %-72s -> %s%n", mu.id(), mu.desc(), flipped.isEmpty() ? "no case changed" : "cases " + flipped + " changed");
        }
    }

    static Map<String, String> replace(Map<String, String> m, String policy, String from, String to) {
        String before = m.get(policy), after = before.replace(from, to);
        if (before.equals(after)) throw new IllegalStateException("mutante não alterou " + policy + ": '" + from + "'");
        m.put(policy, after);
        return m;
    }

    // ------------------------------------------------------------ JSON mínimo (objetos, listas, strings, números, true/false/null)
    static final class Json {
        private final String s;
        private int i;

        private Json(String s) { this.s = s; }

        static Object parse(String text) {
            Json j = new Json(text);
            Object v = j.value();
            j.ws();
            if (j.i != j.s.length()) throw new IllegalArgumentException("JSON com sobra na posição " + j.i);
            return v;
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        private Object value() {
            ws();
            char c = s.charAt(i);
            if (c == '{') {
                Map<String, Object> m = new LinkedHashMap<>();
                i++; ws();
                if (s.charAt(i) == '}') { i++; return m; }
                while (true) {
                    ws(); String k = string(); ws(); expect(':'); m.put(k, value()); ws();
                    if (s.charAt(i) == ',') { i++; continue; }
                    expect('}'); return m;
                }
            }
            if (c == '[') {
                List<Object> l = new ArrayList<>();
                i++; ws();
                if (s.charAt(i) == ']') { i++; return l; }
                while (true) {
                    l.add(value()); ws();
                    if (s.charAt(i) == ',') { i++; continue; }
                    expect(']'); return l;
                }
            }
            if (c == '"') return string();
            int start = i;
            while (i < s.length() && "{}[],: \n\r\t".indexOf(s.charAt(i)) < 0) i++;
            String lit = s.substring(start, i);
            return switch (lit) {
                case "true" -> Boolean.TRUE;
                case "false" -> Boolean.FALSE;
                case "null" -> null;
                default -> Double.parseDouble(lit);
            };
        }

        private String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (s.charAt(i) != '"') {
                char c = s.charAt(i++);
                if (c == '\\') {
                    char e = s.charAt(i++);
                    b.append(switch (e) { case 'n' -> '\n'; case 't' -> '\t'; case 'u' -> { char u = (char) Integer.parseInt(s.substring(i, i + 4), 16); i += 4; yield u; } default -> e; });
                } else b.append(c);
            }
            i++;
            return b.toString();
        }

        private void expect(char c) {
            if (s.charAt(i) != c) throw new IllegalArgumentException("esperava '" + c + "' na posição " + i + ", achei '" + s.charAt(i) + "'");
            i++;
        }
    }
}
