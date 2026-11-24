# Landing zone mínima: OUs e SCPs do PayFlow (S6.05). Aplica-se na CONTA DE GESTÃO, que não roda carga de trabalho.
# 💸 Organizations e SCPs não têm cobrança própria; as contas criadas têm. Teste primeiro numa OU de "Policy Staging".
terraform {
  required_version = ">= 1.11"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.73" }
  }
}

provider "aws" { region = "us-east-1" }

data "aws_organizations_organization" "this" {}

locals {
  root_id = data.aws_organizations_organization.this.roots[0].id
  ous     = toset(["Security", "Backup", "Workloads", "Sandbox", "Suspended"])
  scp_dir = "${path.module}/../../samples/scp"
  # política -> alvos (nomes de OU; "Root" = raiz)
  attachments = {
    "deny-leave-org"       = ["Root"]
    "deny-root-user"       = ["Root"]
    "deny-outside-regions" = ["Root"]
    "protect-audit"        = ["Root"]
    "require-cost-tags"    = ["Workloads"]
    "sandbox-limits"       = ["Sandbox"]
    "deny-all"             = ["Suspended"]
  }
}

resource "aws_organizations_organizational_unit" "top" {
  for_each  = local.ous
  name      = each.value
  parent_id = local.root_id
}

resource "aws_organizations_organizational_unit" "workloads" {
  for_each  = toset(["Prod", "NonProd"])
  name      = each.value
  parent_id = aws_organizations_organizational_unit.top["Workloads"].id
}

resource "aws_organizations_policy" "scp" {
  for_each = local.attachments
  name     = each.key
  type     = "SERVICE_CONTROL_POLICY"
  content  = file("${local.scp_dir}/${each.key}.json")
}

resource "aws_organizations_policy_attachment" "scp" {
  for_each = merge([for p, targets in local.attachments : { for t in targets : "${p}@${t}" => { policy = p, target = t } }]...)

  policy_id = aws_organizations_policy.scp[each.value.policy].id
  target_id = each.value.target == "Root" ? local.root_id : aws_organizations_organizational_unit.top[each.value.target].id
}

# A OU Suspended recebe deny-all e, DEPOIS, perde a FullAWSAccess que o Organizations anexa sozinho (toda entidade
# precisa de pelo menos uma SCP: não dá para tirar a última). Desanexar a gerenciada é passo manual/import, a conferir.
