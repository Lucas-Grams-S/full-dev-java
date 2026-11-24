{{/* Image reference: by DIGEST when image.digest is set (pipeline), else repository:tag (laboratory). */}}
{{- define "payflow.image" -}}
{{- if .Values.image.digest -}}
{{ .Values.image.repository }}@{{ .Values.image.digest }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}
{{- end -}}
{{- end -}}
