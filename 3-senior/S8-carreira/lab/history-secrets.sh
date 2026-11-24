#!/usr/bin/env bash
# S8.01: a secret deleted in a NEW commit is still in the history; rewriting the history does not reach clones made before.
# Everything happens in a throwaway repository under mktemp -d. The "secret" is FAKE. Dates are fixed so commit ids repeat.
set -euo pipefail
export GIT_AUTHOR_NAME="S8 lab" GIT_AUTHOR_EMAIL="lab@example.invalid" GIT_COMMITTER_NAME="S8 lab" GIT_COMMITTER_EMAIL="lab@example.invalid"
export GIT_AUTHOR_DATE="2026-11-23T10:00:00Z" GIT_COMMITTER_DATE="2026-11-23T10:00:00Z"
export FILTER_BRANCH_SQUELCH_WARNING=1
FAKE='s8-lab-FAKE-not-a-real-secret'
tmp=$(mktemp -d)
trap 'rm -rf "${tmp:?}"' EXIT
cd "$tmp"
git -c init.defaultBranch=main init -q portfolio
cd portfolio
count() { git grep -c "$FAKE" $(git rev-list --all) 2>/dev/null | wc -l; }   # how many (commit, file) pairs still hold it

echo "# PayFlow" > README.md
git add README.md && git commit -qm "docs: readme"
printf 'spring.datasource.password=%s\n' "$FAKE" > application-local.properties
git add application-local.properties && git commit -qm "chore: local config"
git rm -q application-local.properties
echo "application-local.properties" > .gitignore
git add .gitignore && git commit -qm "chore: remove local config and ignore it"
git log --oneline

echo
echo "1) the latest commit (what the repository page shows):"
echo "   files in HEAD with the secret: $(git grep -c "$FAKE" HEAD | wc -l)"
echo "2) the whole history (what anyone who clones gets):"
echo "   (commit, file) pairs with the secret: $(count)"
git log --all --oneline -S "$FAKE" | sed 's/^/   added or removed in: /'

git clone -q . ../colleague          # someone cloned (or forked) BEFORE the cleanup

echo
echo "3) rewrite the history (git filter-branch; GitHub recommends git-filter-repo, not installed here):"
git filter-branch --index-filter 'git rm -q --cached --ignore-unmatch application-local.properties' -- --all >/dev/null 2>&1
git log --oneline
echo "   (commit, file) pairs with the secret, counting refs/original: $(count)"
git for-each-ref --format='%(refname)' refs/original/ | while read -r ref; do git update-ref -d "$ref"; done
git reflog expire --expire=now --all
git gc -q --prune=now
echo "   after deleting refs/original, expiring the reflog and gc: $(count)"
echo "   objects still in the repository with the secret: $(git cat-file --batch-all-objects --batch | grep -c "$FAKE" || true)"

echo
echo "4) the clone made before the rewrite:"
cd ../colleague
echo "   (commit, file) pairs with the secret: $(count)"
echo
echo "=> rewriting fixes YOUR copy; the secret already left. Revoke/rotate it first (GitHub docs), then clean the history."
