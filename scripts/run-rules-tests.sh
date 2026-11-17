#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
npx --yes firebase-tools emulators:exec --config firebase.rules-tests.json \
    --only auth,firestore --project demo-campushub "node --test scripts/firestore-rules.test.mjs"
