#!/usr/bin/env bash
set -euo pipefail

RESULT_FILE="${1:-}"
MATRIX_FILE="${2:-}"

if [[ -z "$RESULT_FILE" ]]; then
  echo "usage: $0 <results.ndjson> [matrix.json]" >&2
  exit 1
fi

if [[ ! -f "$RESULT_FILE" ]]; then
  echo "results file not found: $RESULT_FILE" >&2
  exit 1
fi

if [[ -n "$MATRIX_FILE" && ! -f "$MATRIX_FILE" ]]; then
  echo "matrix file not found: $MATRIX_FILE" >&2
  exit 1
fi

jq -s \
  --arg result_file "$RESULT_FILE" \
  --arg matrix_file "${MATRIX_FILE:-}" \
  '
  def safe_div($a; $b): if $b == 0 then 0 else ($a / $b) end;

  def confusion($rows):
    {
      tp: ($rows | map(select(.gt_attack == true and .detected == true)) | length),
      fn: ($rows | map(select(.gt_attack == true and .detected != true)) | length),
      fp: ($rows | map(select(.gt_attack != true and .detected == true)) | length),
      tn: ($rows | map(select(.gt_attack != true and .detected != true)) | length)
    };

  def finalize($c):
    $c + {
      detection_rate: safe_div($c.tp; ($c.tp + $c.fn)),
      false_positive_rate: safe_div($c.fp; ($c.fp + $c.tn))
    };

  . as $rows
  | ($rows | map(select(.scenario_id != null))) as $valid
  | (confusion($valid) | finalize(.)) as $request_level
  | ($valid
      | group_by(.scenario_id)
      | map({
          scenario_id: .[0].scenario_id,
          gt_attack: (.[0].gt_attack == true),
          incident_detected: (any(.[]; .detected == true)),
          requests: length,
          detections: (map(select(.detected == true)) | length),
          statuses: (group_by(.status) | map({status: .[0].status, count: length}))
        })
    ) as $incidents
  | (confusion($incidents | map({gt_attack, detected: .incident_detected})) | finalize(.)) as $incident_level
  | {
      generated_at: (now | todateiso8601),
      source: {
        results_file: $result_file,
        matrix_file: (if $matrix_file == "" then null else $matrix_file end)
      },
      request_level: $request_level,
      incident_level: $incident_level,
      scenario_breakdown: $incidents
    }
  ' "$RESULT_FILE"
