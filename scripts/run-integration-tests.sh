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

integration_config="firebase.integration.json"
integration_filter=""
if [[ "${1:-}" == "--organizer-rules" ]]; then
    integration_config="firebase.rules-tests.json"
    integration_filter=" -Pandroid.testInstrumentationRunnerArguments.class=br.com.uri.campushub.integration.OrganizerFlowIntegrationTest"
elif [[ $# -gt 0 ]]; then
    echo "Uso: $0 [--organizer-rules]" >&2
    exit 1
fi

while IFS= read -r integration_device; do
    adb -s "$integration_device" reverse tcp:8080 tcp:8080
    adb -s "$integration_device" reverse tcp:9099 tcp:9099
done < <(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')

npx --yes firebase-tools emulators:exec \
    --config "$integration_config" \
    --only auth,firestore \
    --project demo-campushub \
    "./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.firebaseHost=127.0.0.1$integration_filter"
