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

  def confusion($rows; $field):
    {
      tp: ($rows | map(select(.gt_attack == true and .[$field] == true)) | length),
      fn: ($rows | map(select(.gt_attack == true and .[$field] != true)) | length),
      fp: ($rows | map(select(.gt_attack != true and .[$field] == true)) | length),
      tn: ($rows | map(select(.gt_attack != true and .[$field] != true)) | length)
    };

  def finalize($c):
    $c + {
      detection_rate: safe_div($c.tp; ($c.tp + $c.fn)),
      false_positive_rate: safe_div($c.fp; ($c.fp + $c.tn))
    };

  . as $rows
  | ($rows | map(select(.scenario_id != null and .measurable != false))) as $valid
  | (confusion($valid; "blocked") | finalize(.)) as $security_block_level
  | (confusion($valid; "csrf_detected") | finalize(.)) as $csrf_filter_level
  | ($valid
      | group_by(.scenario_id)
      | map({
          scenario_id: .[0].scenario_id,
          title: .[0].title,
          gt_attack: (.[0].gt_attack == true),
          measurable: (.[0].measurable != false),
          incident_blocked: (any(.[]; .blocked == true)),
          incident_csrf_detected: (any(.[]; .csrf_detected == true)),
          requests: length,
          blocked_count: (map(select(.blocked == true)) | length),
          csrf_detect_count: (map(select(.csrf_detected == true)) | length),
          statuses: (group_by(.status) | map({status: .[0].status, count: length})),
          codes: (group_by(.body_code) | map({code: .[0].body_code, count: length}))
        })
    ) as $incidents
  | ($incidents | map(select(.measurable == true))) as $incident_valid
  | (confusion($incident_valid | map({gt_attack, blocked: .incident_blocked}); "blocked") | finalize(.)) as $incident_security_block_level
  | (confusion($incident_valid | map({gt_attack, csrf_detected: .incident_csrf_detected}); "csrf_detected") | finalize(.)) as $incident_csrf_filter_level
  | {
      generated_at: (now | todateiso8601),
      source: {
        results_file: $result_file,
        matrix_file: (if $matrix_file == "" then null else $matrix_file end)
      },
      request_level: {
        security_block_level: $security_block_level,
        csrf_filter_level: $csrf_filter_level
      },
      incident_level: {
        security_block_level: $incident_security_block_level,
        csrf_filter_level: $incident_csrf_filter_level
      },
      counts: {
        total_requests: ($rows | length),
        measurable_requests: ($valid | length),
        blocked_requests: ($rows | map(select(.blocked == true)) | length),
        csrf_detect_requests: ($rows | map(select(.csrf_detected == true)) | length),
        allowed_requests: ($rows | map(select(.blocked != true)) | length)
      },
      scenario_breakdown: $incidents
    }
  ' "$RESULT_FILE"
