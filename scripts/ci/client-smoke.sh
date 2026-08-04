#!/usr/bin/env bash
# 在 GitHub Hosted Runner 的 Xvfb + Mesa 中运行真实 Forge 开发客户端；不要在开发机运行。
set -Eeuo pipefail
: "${GITHUB_WORKSPACE:?本脚本只允许在 GitHub Actions 中运行}"
ROOT="$GITHUB_WORKSPACE"
WORK="$ROOT/ci-artifacts/client-smoke"
LOG="$WORK/client-console.log"
GAME_LOG="$ROOT/run/logs/latest.log"
mkdir -p "$WORK"
rm -f "$GAME_LOG" "$WORK/close-request"

export LIBGL_ALWAYS_SOFTWARE=1
export MESA_GL_VERSION_OVERRIDE=3.3
export _JAVA_AWT_WM_NONREPARENTING=1
cd "$ROOT"
set -m
# xdotool 必须与 Client 共享 xvfb-run 创建的 DISPLAY/Xauthority，因此将关闭动作置于同一子 shell。
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX' bash -c '
  set -Eeuo pipefail
  ./gradlew --no-daemon runClient &
  client=$!
  while [[ ! -f "ci-artifacts/client-smoke/close-request" ]]; do
    kill -0 "$client" 2>/dev/null || exit 1
    sleep 1
  done
  window=$(xdotool search --name "Minecraft" 2>/dev/null | head -n1 || true)
  [[ -n "$window" ]] || { echo "未找到 Minecraft 窗口，不能确认已进入可交互客户端" >&2; exit 1; }
  xdotool windowclose "$window"
  for _ in {1..90}; do kill -0 "$client" 2>/dev/null || break; sleep 1; done
  kill -0 "$client" 2>/dev/null && { echo "客户端未在窗口关闭后正常退出" >&2; exit 1; }
  wait "$client"
' > "$LOG" 2>&1 &
CLIENT_PID=$!

cleanup() {
  if kill -0 "$CLIENT_PID" 2>/dev/null; then
    kill -TERM -- -"$CLIENT_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# Forge 1.20.1 没有稳定的“主菜单”日志字样；以资源重载/音频初始化和 rolecard 被发现为可识别初始化标志，随后继续稳定 20 秒。
ready=0
for _ in {1..240}; do
  if [[ -f "$GAME_LOG" ]] \
    && grep -Eqi 'Sound engine started|OpenAL initialized|Reloading ResourceManager' "$GAME_LOG" \
    && grep -qi 'rolecard' "$GAME_LOG"; then
    ready=1
    break
  fi
  if ! kill -0 "$CLIENT_PID" 2>/dev/null; then echo "Forge 客户端初始化前退出" >&2; exit 1; fi
  sleep 1
done
[[ "$ready" -eq 1 ]] || { echo "未观察到 Forge 客户端初始化成功标志" >&2; exit 1; }
sleep 20
kill -0 "$CLIENT_PID" 2>/dev/null || { echo "客户端未能稳定运行至初始化后 20 秒" >&2; exit 1; }

# 通过与 Client 共用 Xvfb 的子 shell 请求关闭，等同用户关闭窗口；拒绝用 timeout 强杀制造假绿。
touch "$WORK/close-request"
for _ in {1..90}; do kill -0 "$CLIENT_PID" 2>/dev/null || break; sleep 1; done
if kill -0 "$CLIENT_PID" 2>/dev/null; then echo "客户端未在窗口关闭后正常退出" >&2; exit 1; fi
wait "$CLIENT_PID"
trap - EXIT

combined="$WORK/combined.log"
cat "$LOG" "$GAME_LOG" 2>/dev/null > "$combined" || cp "$LOG" "$combined"
if grep -Eqi 'FATAL|NoClassDefFoundError|Mixin apply (failed|error)|MixinApplyError|crash-report|crash report|Exception in thread' "$combined"; then
  echo "客户端日志含严重错误" >&2; grep -Ein 'FATAL|NoClassDefFoundError|Mixin|crash|Exception in thread' "$combined" >&2 || true; exit 1
fi
if find "$ROOT/run" -type f -path '*/crash-reports/*' -print -quit | grep -q .; then
  echo "客户端产生了 crash-report" >&2; exit 1
fi
echo "Client smoke 通过：真实 Forge 客户端在 Xvfb/Mesa 下完成 rolecard 初始化并经窗口请求正常退出。"
