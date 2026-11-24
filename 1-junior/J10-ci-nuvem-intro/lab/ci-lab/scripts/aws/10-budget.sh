#!/usr/bin/env bash
# J10.04: orcamento mensal com alertas por e-mail. Uso: ALERT_EMAIL=voce@exemplo.com bash 10-budget.sh
source "$(dirname "$0")/env.sh"
: "${ALERT_EMAIL:?set ALERT_EMAIL}"
LIMIT="${BUDGET_USD:-10}"
budget="$(jq -n --arg n "${PROJECT}-monthly" --arg l "$LIMIT" \
  '{BudgetName:$n, BudgetLimit:{Amount:$l, Unit:"USD"}, TimeUnit:"MONTHLY", BudgetType:"COST"}')"
notifs="$(jq -n --arg e "$ALERT_EMAIL" '[
  {Notification:{NotificationType:"ACTUAL",     ComparisonOperator:"GREATER_THAN", Threshold:50,  ThresholdType:"PERCENTAGE"}, Subscribers:[{SubscriptionType:"EMAIL",Address:$e}]},
  {Notification:{NotificationType:"ACTUAL",     ComparisonOperator:"GREATER_THAN", Threshold:80,  ThresholdType:"PERCENTAGE"}, Subscribers:[{SubscriptionType:"EMAIL",Address:$e}]},
  {Notification:{NotificationType:"FORECASTED", ComparisonOperator:"GREATER_THAN", Threshold:100, ThresholdType:"PERCENTAGE"}, Subscribers:[{SubscriptionType:"EMAIL",Address:$e}]}
]')"
aws budgets create-budget --account-id "$ACCOUNT_ID" --budget "$budget" --notifications-with-subscribers "$notifs"
echo "budget ${PROJECT}-monthly: USD ${LIMIT}/month"
