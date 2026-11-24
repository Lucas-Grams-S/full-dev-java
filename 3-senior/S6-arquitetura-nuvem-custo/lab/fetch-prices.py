#!/usr/bin/env python3
"""Gera prices.csv com os preços unitários que o laboratório do S6 usa.

Fonte: AWS Price List Bulk API (arquivos públicos, sem conta e sem credenciais)
  https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/<Oferta>/current/<região>/index.csv
  https://pricing.us-east-1.amazonaws.com/savingsPlan/v1.0/aws/AWSComputeSavingsPlan/current/region_index.json
e os dados públicos que alimentam a página Spot Instance Advisor
  https://spot-bid-advisor.s3.amazonaws.com/spot-advisor-data.json

Uso:
  python3 -I fetch-prices.py --cache DIR [--out prices.csv]
  (DIR guarda os CSVs baixados; rode de novo e ele reaproveita o que já baixou.)

Atenção: os arquivos do EC2 (~300 MB por região) e dos Savings Plans (~300 MB) são lidos
em fluxo e filtrados (só as linhas usadas ficam no cache): leva alguns minutos.
Os preços mudam: o arquivo de saída registra a data de publicação de cada oferta.
"""
import argparse, csv, io, json, os, re, sys, urllib.request

BASE = "https://pricing.us-east-1.amazonaws.com"
REGIONS = ["us-east-1", "sa-east-1"]
SMALL = ["AmazonECS", "AWSLambda", "AmazonRDS", "AmazonVPC", "AWSELB", "awskms", "AmazonCloudWatch",
         "AWSSecretsManager", "AmazonS3", "AWSQueueService", "AmazonSNS", "AWSDataTransfer", "AWSConfig"]
EC2_KEEP = re.compile(r'"(t4g\.small|t4g\.medium|m7g\.large|c7g\.large|m7i\.large)"|NatGateway-(Hours|Bytes)"|EBS:VolumeUsage\.gp3"|EBS:SnapshotUsage"')
SP_KEEP = re.compile(r'Fargate-ARM-(vCPU-Hours:perCPU|GB-Hours),')

# chave, oferta, regex do usageType (fim), filtros {coluna: regex}, regex da descrição, faixa inicial
ONDEMAND = [
    ("fargate.arm.vcpu_h", "AmazonECS", r"Fargate-ARM-vCPU-Hours:perCPU$", {}, None, "0"),
    ("fargate.arm.gb_h", "AmazonECS", r"Fargate-ARM-GB-Hours$", {}, None, "0"),
    ("fargate.x86.vcpu_h", "AmazonECS", r"(^|-)Fargate-vCPU-Hours:perCPU$", {}, None, "0"),
    ("fargate.x86.gb_h", "AmazonECS", r"(^|-)Fargate-GB-Hours$", {}, None, "0"),
    ("lambda.arm.gb_s", "AWSLambda", r"Lambda-GB-Second-ARM$", {}, None, "0"),
    ("lambda.x86.gb_s", "AWSLambda", r"(^|-)Lambda-GB-Second$", {}, None, "0"),
    ("lambda.request", "AWSLambda", r"^([A-Z]{3}[0-9]-)?Request$", {}, None, "0"),
    ("ec2.t4g.small.od_h", "AmazonEC2", r"BoxUsage:t4g\.small$", {"CapacityStatus": "^Used$", "Pre Installed S/W": "^NA$"}, None, "0"),
    ("ec2.m7g.large.od_h", "AmazonEC2", r"BoxUsage:m7g\.large$", {"CapacityStatus": "^Used$", "Pre Installed S/W": "^NA$"}, None, "0"),
    ("ec2.c7g.large.od_h", "AmazonEC2", r"BoxUsage:c7g\.large$", {"CapacityStatus": "^Used$", "Pre Installed S/W": "^NA$"}, None, "0"),
    ("rds.pg.t4g.medium.single_h", "AmazonRDS", r"InstanceUsage:db\.t4g\.medium$", {"Database Engine": "^PostgreSQL$"}, None, "0"),
    ("rds.pg.t4g.medium.multi_h", "AmazonRDS", r"Multi-AZUsage:db\.t4g\.medium$", {"Database Engine": "^PostgreSQL$"}, None, "0"),
    ("rds.pg.m7g.large.single_h", "AmazonRDS", r"InstanceUsage:db\.m7g\.large$", {"Database Engine": "^PostgreSQL$"}, None, "0"),
    ("rds.pg.m7g.large.multi_h", "AmazonRDS", r"Multi-AZUsage:db\.m7g\.large$", {"Database Engine": "^PostgreSQL$"}, None, "0"),
    ("rds.gp3.single_gb_mo", "AmazonRDS", r"RDS:GP3-Storage$", {"Database Engine": "^PostgreSQL$"}, None, "0"),
    ("rds.gp3.multi_gb_mo", "AmazonRDS", r"RDS:Multi-AZ-GP3-Storage$", {"Database Engine": "^PostgreSQL$"}, None, "0"),
    ("rds.backup_gb_mo", "AmazonRDS", r"RDS:ChargedBackupUsage$", {"Database Engine": "^PostgreSQL$"}, None, "0"),
    ("vpc.endpoint_h", "AmazonVPC", r"VpcEndpoint-Hours$", {}, None, "0"),
    ("vpc.endpoint_gb", "AmazonVPC", r"VpcEndpoint-Bytes$", {}, None, "0"),
    ("vpc.ipv4_h", "AmazonVPC", r"PublicIPv4:InUseAddress$", {}, None, "0"),
    ("nat.h", "AmazonEC2", r"(^|-)NatGateway-Hours$", {}, None, "0"),
    ("nat.gb", "AmazonEC2", r"(^|-)NatGateway-Bytes$", {}, None, "0"),
    ("ebs.gp3_gb_mo", "AmazonEC2", r"EBS:VolumeUsage\.gp3$", {}, None, "0"),
    ("ebs.snapshot_gb_mo", "AmazonEC2", r"EBS:SnapshotUsage$", {}, None, "0"),
    ("alb.h", "AWSELB", r"(^|-)LoadBalancerUsage$", {}, r"Application LoadBalancer-hour", "0"),
    ("alb.lcu_h", "AWSELB", r"(^|-)LCUUsage$", {}, r"used Application load balancer capacity unit-hour", "0"),
    ("kms.key_mo", "awskms", r"-KMS-Keys$", {}, None, "0"),
    ("kms.request", "awskms", r"-KMS-Requests$", {}, None, "0"),
    ("cw.logs_ingest_gb", "AmazonCloudWatch", r"(^|-)DataProcessing-Bytes$", {}, r"Standard log class", "0"),
    ("cw.logs_store_gb_mo", "AmazonCloudWatch", r"(^|-)TimedStorage-ByteHrs$", {}, r"log storage", "0"),
    ("cw.metric_mo", "AmazonCloudWatch", r"(^|-)CW:MetricMonitorUsage$", {}, r"first 10,000 metrics", "0"),
    ("cw.alarm_mo", "AmazonCloudWatch", r"(^|-)CW:AlarmMonitorUsage$", {}, r"standard resolution", "0"),
    ("sm.secret_mo", "AWSSecretsManager", r"AWSSecretsManager-Secrets$", {}, None, "0"),
    ("sm.api", "AWSSecretsManager", r"AWSSecretsManagerAPIRequest$", {}, None, "0"),
    ("s3.std_gb_mo", "AmazonS3", r"(^|-)TimedStorage-ByteHrs$", {}, r"first 50 TB", "0"),
    ("s3.put", "AmazonS3", r"(^|-)Requests-Tier1$", {}, r"PUT, COPY, POST, or LIST", "0"),
    ("sqs.request", "AWSQueueService", r"(Requests-RBP|Requests-Tier1)$", {}, r"standard requests in Tier1", "0"),
    ("sns.request", "AmazonSNS", r"(^|-)Requests-Tier1$", {}, None, "1000000"),
    ("dt.out_gb", "AWSDataTransfer", r"(^|-)DataTransfer-Out-Bytes$", {}, None, "0"),
    ("dt.inter_az_gb", "AWSDataTransfer", r"(^|-)DataTransfer-Regional-Bytes$", {}, None, "0"),
    ("config.item", "AWSConfig", r"(^|-)ConfigurationItemRecorded$", {}, None, "0"),
]
# reservas: chave, oferta, usageType, filtros, contrato, opção de compra
RESERVED = [
    ("ec2.m7g.large.ri1y_nu_h", "AmazonEC2", r"BoxUsage:m7g\.large$", {"OfferingClass": "^standard$"}, "1yr", "No Upfront"),
    ("ec2.m7g.large.ri3y_nu_h", "AmazonEC2", r"BoxUsage:m7g\.large$", {"OfferingClass": "^standard$"}, "3yr", "No Upfront"),
    ("rds.pg.m7g.large.multi.ri1y_nu_h", "AmazonRDS", r"Multi-AZUsage:db\.m7g\.large$", {"Database Engine": "^PostgreSQL$"}, "1yr", "No Upfront"),
]
INTER_REGION = {("us-east-1", "sa-east-1"): "USE1-SAE1-AWS-Out-Bytes", ("sa-east-1", "us-east-1"): "SAE1-USE1-AWS-Out-Bytes"}


def get(url, path, keep=None):
    if os.path.exists(path):
        return
    print("baixando", url, file=sys.stderr)
    with urllib.request.urlopen(url, timeout=600) as r, open(path + ".part", "w", encoding="utf-8") as out:
        for i, raw in enumerate(io.TextIOWrapper(r, encoding="utf-8")):
            if keep is None or i < 8 or keep.search(raw):
                out.write(raw)
    os.replace(path + ".part", path)


def rows(path):
    with open(path, encoding="utf-8", newline="") as fh:
        lines = fh.read().splitlines()
    pub = next((l.split(",", 1)[1].strip('"') for l in lines[:8] if l.strip('"').startswith("Publication Date")), "?")
    start = next(i for i, l in enumerate(lines) if l.startswith('"SKU"') or l.startswith("SKU,"))
    return pub, list(csv.DictReader(lines[start:]))


def pick(rs, usage, filters, desc, term="OnDemand", lease=None, option=None, tier="0"):
    found = []
    for r in rs:
        if r.get("TermType") != term or not re.search(usage, r.get("usageType") or ""):
            continue
        if (r.get("usageType") or "").startswith("Global-"):  # cota gratuita global, não é o preço
            continue
        if any(not re.search(v, r.get(k) or "") for k, v in filters.items()):
            continue
        if desc and not re.search(desc, r.get("PriceDescription") or ""):
            continue
        if term == "Reserved":
            if r.get("LeaseContractLength") != lease or r.get("PurchaseOption") != option or r.get("Unit") != "Hrs":
                continue
        elif (r.get("StartingRange") or "0") != tier:
            continue
        found.append(r)
    prices = {r["PricePerUnit"] for r in found}
    if len(prices) != 1:
        raise SystemExit(f"esperava 1 preço para {usage} {filters} {desc}, achei {sorted(prices)}")
    return found[0]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cache", required=True)
    ap.add_argument("--out", default="prices.csv")
    a = ap.parse_args()
    os.makedirs(a.cache, exist_ok=True)
    out = []
    for region in REGIONS:
        data = {}
        for offer in SMALL:
            p = os.path.join(a.cache, f"{offer}-{region}.csv")
            get(f"{BASE}/offers/v1.0/aws/{offer}/current/{region}/index.csv", p)
            data[offer] = rows(p)
        p = os.path.join(a.cache, f"AmazonEC2-{region}.filtered.csv")
        get(f"{BASE}/offers/v1.0/aws/AmazonEC2/current/{region}/index.csv", p, EC2_KEEP)
        data["AmazonEC2"] = rows(p)
        for key, offer, usage, filters, desc, tier in ONDEMAND:
            pub, rs = data[offer]
            if offer == "AmazonEC2" and key.startswith("ec2."):
                filters = dict(filters, **{"Operating System": "^Linux$", "Tenancy": "^Shared$"})
            r = pick(rs, usage, filters, desc, tier=tier)
            out.append([key, region, r["PricePerUnit"], r["Unit"], r["PriceDescription"], r["EffectiveDate"], f"{offer} {pub[:10]}", r["usageType"]])
        for key, offer, usage, filters, lease, option in RESERVED:
            pub, rs = data[offer]
            if offer == "AmazonEC2":
                filters = dict(filters, **{"Operating System": "^Linux$", "Tenancy": "^Shared$", "Pre Installed S/W": "^NA$"})
            r = pick(rs, usage, filters, None, term="Reserved", lease=lease, option=option)
            out.append([key, region, r["PricePerUnit"], "Hrs", f"{lease} {option} standard: {r['PriceDescription']}", r["EffectiveDate"], f"{offer} {pub[:10]}", r["usageType"]])
        pub, rs = data["AWSDataTransfer"]
        for (src, dst), ut in INTER_REGION.items():
            if src == region:
                r = next(x for x in rs if x.get("usageType") == ut and x.get("TermType") == "OnDemand")
                out.append([f"dt.inter_region_to_{dst}_gb", region, r["PricePerUnit"], r["Unit"], r["PriceDescription"], r["EffectiveDate"], f"AWSDataTransfer {pub[:10]}", ut])
        # Compute Savings Plans (1 ano, sem pagamento antecipado) para o Fargate ARM
        idx = os.path.join(a.cache, "sp-region-index.json")
        get(f"{BASE}/savingsPlan/v1.0/aws/AWSComputeSavingsPlan/current/region_index.json", idx)
        ver = next(x["versionUrl"] for x in json.load(open(idx))["regions"] if x["regionCode"] == region)
        p = os.path.join(a.cache, f"SP-{region}.filtered.csv")
        get(BASE + ver.replace("index.json", "index.csv"), p, SP_KEEP)
        with open(p, encoding="utf-8", newline="") as fh:
            lines = fh.read().splitlines()
        pub = next(l.split(",", 1)[1] for l in lines[:8] if l.startswith("Publication Date"))
        rs = list(csv.DictReader(lines[next(i for i, l in enumerate(lines) if l.startswith("SKU,")):]))
        for key, ut in (("sp.fargate.arm.vcpu_h.1y_nu", "Fargate-ARM-vCPU-Hours:perCPU"), ("sp.fargate.arm.gb_h.1y_nu", "Fargate-ARM-GB-Hours")):
            r = [x for x in rs if x["DiscountedUsageType"].endswith(ut) and x["PurchaseOption"] == "No Upfront"
                 and x["LeaseContractLength"] == "1" and x["DiscountedServiceCode"] == "AmazonECS"]
            if len({x["DiscountedRate"] for x in r}) != 1:
                raise SystemExit(f"Savings Plan ambíguo para {ut}: {[x['DiscountedRate'] for x in r]}")
            out.append([key, region, r[0]["DiscountedRate"], "Hrs", r[0]["Description"] + " - " + ut, r[0]["EffectiveDate"][:10], f"AWSComputeSavingsPlan {pub[:10]}", r[0]["DiscountedUsageType"]])
    # Spot Instance Advisor: economia (%) e faixa de frequência de interrupção
    sp = os.path.join(a.cache, "spot-advisor.json")
    get("https://spot-bid-advisor.s3.amazonaws.com/spot-advisor-data.json", sp)
    adv = json.load(open(sp))
    labels = {r["index"]: r["label"] for r in adv["ranges"]}
    for region in REGIONS:
        for it in ("m7g.large", "c7g.large", "t4g.small"):
            v = adv["spot_advisor"][region]["Linux"][it]
            out.append([f"spot.{it}.savings_pct", region, str(v["s"]), "percent", f"Spot Instance Advisor: economia sobre sob demanda; interrupção {labels[v['r']]}", "", "spot-advisor-data.json", ""])
    with open(a.out, "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["key", "region", "usd", "unit", "description", "effective", "source", "usage_type"])
        for row in out:
            row[2] = f"{float(row[2]):.10g}"
            w.writerow(row)
    print(f"{len(out)} preços gravados em {a.out}", file=sys.stderr)


if __name__ == "__main__":
    main()
