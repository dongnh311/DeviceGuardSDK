#!/usr/bin/env bash
# PreToolUse hook — block `gh pr create` if the branch changes public-facing
# code (.kt / .kts / version catalog / new module directory) without touching
# README.md, and no explicit bypass is recorded in a commit message.
#
# Allows: `gh pr view`, `gh pr list`, `gh pr comment`, `gh pr merge`, etc.
# Allows `gh pr create` when any of:
#   - README.md is part of the branch diff
#   - The diff contains only non-user-facing files (docs, tests, tooling)
#   - A commit in the last 10 commits carries a bypass sentinel:
#       `no-readme` | `skip-readme` | `readme: n/a` |
#       `readme: not applicable` | `readme: unchanged`

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/../.." || exit 0

payload="$(cat)"
cmd="$(printf '%s' "$payload" | jq -r '.tool_input.command // ""')"

first_line="$(printf '%s' "$cmd" | sed -n '1p')"
if ! printf '%s' "$first_line" | grep -Eq '(^|[[:space:];&|])gh[[:space:]]+pr[[:space:]]+create([[:space:]]|$)'; then
  exit 0
fi

base_ref="origin/main"
if ! git rev-parse --verify --quiet "$base_ref" >/dev/null 2>&1; then
  base_ref="main"
fi

changed="$(git diff --name-only "${base_ref}...HEAD" 2>/dev/null || true)"
[[ -z "$changed" ]] && exit 0

# README already touched → nothing to gate.
if printf '%s\n' "$changed" | grep -Fxq 'README.md'; then
  exit 0
fi

# User-facing files: Kotlin sources, Gradle build/config, version catalog, or a
# brand-new top-level module directory under deviceguard-*.
user_facing="$(
  printf '%s\n' "$changed" | grep -E \
    -e '\.(kt|kts)$' \
    -e '^gradle/libs\.versions\.toml$' \
    -e '^settings\.gradle\.kts$' \
    -e '^deviceguard-[^/]+/build\.gradle\.kts$' \
    || true
)"

if [[ -z "$user_facing" ]]; then
  exit 0
fi

# Bypass sentinel in any of the last 10 commit messages.
if git log -n 10 --format='%s%n%b' "$base_ref..HEAD" 2>/dev/null |
     grep -Eiq -- '(no-readme|skip-readme|readme:[[:space:]]*(n/a|not[[:space:]]+applicable|unchanged))'; then
  exit 0
fi

cat <<'JSON'
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "Blocked: this branch changes public-facing code (.kt / .kts / version catalog / new module) but README.md is untouched. Either (a) update README.md to reflect the change — installation snippet, module table, platform status, quick-start API, or feature list — and commit the edit, or (b) add a commit whose message contains `no-readme`, `skip-readme`, `readme: n/a`, `readme: not applicable`, or `readme: unchanged` if the change genuinely affects nothing a reader would care about. Then retry `gh pr create`."
  }
}
JSON
