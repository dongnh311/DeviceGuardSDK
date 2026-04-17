#!/usr/bin/env bash
# PreToolUse hook — block `gh pr create` until a /simplify review pass has been
# recorded on the current branch.
#
# Allows: `gh pr view`, `gh pr list`, `gh pr comment`, `gh pr merge`, etc.
# Blocks: `gh pr create` (any flag variant) unless the last 10 commits on this
# branch (relative to origin/main) contain one whose message subject starts with
# `simplify:` or `review:`, or whose full message contains the phrase
# `simplify review`.
#
# The hook reads the tool payload from stdin as JSON and (on block) prints a
# PreToolUse `permissionDecision=deny` response with a reason Claude can act on.

set -euo pipefail

# Anchor to the repo root so git commands resolve regardless of invocation cwd.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/../.." || exit 0

payload="$(cat)"
cmd="$(printf '%s' "$payload" | jq -r '.tool_input.command // ""')"

# Only gate `gh pr create`. Everything else passes through.
if ! printf '%s' "$cmd" | grep -Eq '(^|[^[:alnum:]_/-])gh[[:space:]]+pr[[:space:]]+create([[:space:]]|$)'; then
  exit 0
fi

# Pick a base ref. Prefer origin/main; fall back to main for fresh clones.
base_ref="origin/main"
if ! git rev-parse --verify --quiet "$base_ref" >/dev/null 2>&1; then
  base_ref="main"
fi

# Look for the sentinel in the last 10 commits on this branch.
sentinel="$(
  git log -n 10 --format='%s%n%b%n--END--' "$base_ref..HEAD" 2>/dev/null |
    grep -Eim1 -- '^(simplify|review):|simplify review' || true
)"

if [[ -n "$sentinel" ]]; then
  exit 0
fi

# No sentinel → deny with a detailed reason so Claude knows what to do next.
cat <<'JSON'
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "Blocked: no /simplify review recorded on this branch. Before `gh pr create`, (1) run /simplify on the branch diff, (2) read the three agent reports (reuse / quality / efficiency), (3) fix the high-confidence findings, (4) commit the fixes with a subject starting `simplify:` or `review:` (or containing the phrase `simplify review`), (5) push, (6) retry `gh pr create`. This hook inspects the last 10 commits on this branch relative to origin/main for the sentinel."
  }
}
JSON
