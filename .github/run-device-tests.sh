#!/usr/bin/env bash
# Keep the test exit status in one shell; emulator-runner splits script lines.
set -uo pipefail
mode="${1:?phone or fold}"
if [[ "$mode" == "phone" ]]; then
  classes=com.d1inthechamber.blackjack.CasinoSmokeTest,com.d1inthechamber.blackjack.TableSmokeTest,com.d1inthechamber.blackjack.UpgradeSmokeTest
  evidence=screenshots
else
  classes=com.d1inthechamber.blackjack.FoldableSmokeTest
  evidence=foldable-screenshots
fi
test_status=0
gradle :app:connectedDebugAndroidTest --no-daemon --stacktrace "-Pandroid.testInstrumentationRunnerArguments.class=$classes" || test_status=$?
mkdir -p "$evidence"
evidence_status=0
pull_shot() {
  adb pull "/sdcard/Download/$1.png" "$evidence/$2.png" || evidence_status=1
}
if [[ "$mode" == "phone" ]]; then
  for name in portrait-hit landscape-hit; do pull_shot "royal-felt-$name" "$name"; done
  for room in carnival egypt iron west punk green; do pull_shot "royal-felt-room-$room" "room-$room"; done
  for game in lobby slots solitaire poker settings rooms craps craps-win craps-player-pull craps-opponent-pull craps-opponent-receive slots-vegas slots-carnival slots-egypt slots-iron slots-west slots-punk slots-green solitaire-draw-three solitaire-win-motion solitaire-win-stack solitaire-no-moves craps-opponent-hold craps-opponent-throw craps-hand-vegas craps-hand-carnival craps-hand-egypt craps-hand-iron craps-hand-west craps-hand-punk craps-hand-green; do
    pull_shot "chaos-$game" "chaos-$game"
  done
  adb logcat -d -s AndroidRuntime > android-runtime.log || true
else
  for name in unfolded folded landscape solitaire-unfolded solitaire-folded solitaire-landscape poker-unfolded poker-folded poker-landscape; do pull_shot "fold-$name" "$name"; done
  adb logcat -d -s AndroidRuntime > foldable-runtime.log || true
fi
if [[ "$test_status" -ne 0 ]]; then exit "$test_status"; fi
if [[ "$evidence_status" -ne 0 ]]; then
  echo "Required visual evidence is missing" >&2
  exit 1
fi
python3 .github/check-device-results.py "$mode"
