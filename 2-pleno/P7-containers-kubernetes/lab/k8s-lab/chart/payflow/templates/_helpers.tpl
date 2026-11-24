{{- define "payflow.name" -}}payflow{{- end -}}

{{- define "payflow.labels" -}}
app.kubernetes.io/name: {{ include "payflow.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
{{- end -}}

{{- define "payflow.selectorLabels" -}}
app.kubernetes.io/name: {{ include "payflow.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
