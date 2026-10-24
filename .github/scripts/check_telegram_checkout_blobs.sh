#!/usr/bin/env bash
set -euo pipefail
# Evidence-only: GIT_NO_LAZY_FETCH prevents the probe from downloading blobs itself.
scope="${1:-none}"
case "$scope" in none|ui|full) ;; *) echo "Unknown Telegram archive scope: $scope" >&2; exit 2;; esac
names=(Telegram-master.zip Telegram-master-rest-{1..5}.zip)
echo "CHECKOUT_ZIP_SCOPE=$scope"
for name in "${names[@]}"; do
  oid="$(git ls-tree HEAD -- "$name" | awk '{print $3}')"
  [[ -n "$oid" ]] || { echo "ZIP_BLOB_PROOF MISSING_TREE_ENTRY name=$name"; exit 1; }
  local_blob=false
  if GIT_NO_LAZY_FETCH=1 git cat-file -e "${oid}^{blob}" 2>/dev/null; then
    local_blob=true
  fi
  working_tree=false
  if [[ -f "$name" ]]; then working_tree=true; fi
  needed=false
  if [[ "$scope" == full ]] || { [[ "$scope" == ui ]] && [[ "$name" == Telegram-master.zip ]]; }; then
    needed=true
  fi
  echo "ZIP_BLOB_PROOF name=$name needed=$needed local_blob=$local_blob working_tree=$working_tree"
  if [[ "$needed" == true ]]; then
    [[ "$local_blob" == true && "$working_tree" == true ]] || { echo "Missing required ZIP $name"; exit 1; }
  else
    [[ "$local_blob" == false && "$working_tree" == false ]] || { echo "Unnecessary ZIP was fetched or checked out: $name"; exit 1; }
  fi
done
echo "ZIP_BLOB_PROOF_RESULT=PASS scope=$scope"
