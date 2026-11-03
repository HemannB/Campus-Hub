#!/usr/bin/env bash

set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
    echo "O adb não foi encontrado. Instale ou configure o Android SDK." >&2
    exit 1
fi

if ! command -v npx >/dev/null 2>&1; then
    echo "O npx não foi encontrado. Instale o Node.js com npm." >&2
    exit 1
fi

if ! adb devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit !found }'; then
    echo "Nenhum dispositivo Android está conectado. Inicie um emulador pelo Android Studio." >&2
    exit 1
fi

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"

npx --yes firebase-tools emulators:exec \
    --only auth,firestore \
    --project demo-campushub \
    "./gradlew connectedDebugAndroidTest"
