#!/usr/bin/env bash
# 在 GitHub Hosted Runner 的 Xvfb + Mesa 中运行真实 Forge 开发客户端；不要在开发机运行。
set -Eeuo pipefail
: "${GITHUB_WORKSPACE:?本脚本只允许在 GitHub Actions 中运行}"
ROOT="$GITHUB_WORKSPACE"
WORK="$ROOT/ci-artifacts/client-smoke"
LOG="$WORK/client-console.log"
GAME_LOG="$ROOT/run/logs/latest.log"
mkdir -p "$WORK"
rm -f "$GAME_LOG"

export LIBGL_ALWAYS_SOFTWARE=1
export MESA_GL_VERSION_OVERRIDE=3.3
export _JAVA_AWT_WM_NONREPARENTING=1
cd "$ROOT"
# 禁用 MIT-SHM：Hosted Runner 的 Xvfb 共享内存路径会令 LWJGL 报 BadDrawable。
# src/ci 的探针确认 TitleScreen 连续稳定 40 tick 后调用 Minecraft.stop()，不直接销毁 X 窗口。
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX -extension MIT-SHM' ./gradlew --no-daemon runClient > "$LOG" 2>&1 &
CLIENT_PID=$!

cleanup() {
  if kill -0 "$CLIENT_PID" 2>/dev/null; then
    kill -TERM -- -"$CLIENT_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# CI 专用探针仅在真实 Client 到达 TitleScreen 并稳定 40 tick 后写入标记，再由主循环正常退出。
ready=0
for _ in {1..240}; do
  if grep -q 'ROLECARD_CI_TITLE_SCREEN_READY' "$LOG" "$GAME_LOG" 2>/dev/null; then
    ready=1
    break
  fi
  if ! kill -0 "$CLIENT_PID" 2>/dev/null; then echo "Forge 客户端初始化前退出" >&2; exit 1; fi
  sleep 1
done
[[ "$ready" -eq 1 ]] || { echo "未观察到 Forge 客户端主菜单探针成功标志" >&2; exit 1; }
for _ in {1..90}; do kill -0 "$CLIENT_PID" 2>/dev/null || break; sleep 1; done
if kill -0 "$CLIENT_PID" 2>/dev/null; then echo "客户端未在主菜单探针后正常退出" >&2; exit 1; fi
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
echo "Client smoke 通过：真实 Forge 客户端到达主菜单并由 CI 专用探针正常退出。"
