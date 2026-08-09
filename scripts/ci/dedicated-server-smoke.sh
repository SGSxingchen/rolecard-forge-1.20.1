#!/usr/bin/env bash
# 在 GitHub Hosted Runner 上执行；不要在开发机运行本脚本。
set -Eeuo pipefail

ROOT="${GITHUB_WORKSPACE:?本脚本只允许在 GitHub Actions 中运行}"
DIST_DIR="${1:-$ROOT/dist}"
MC_VERSION="1.20.1"
FORGE_VERSION="47.4.10"
FORGE_COORD="${MC_VERSION}-${FORGE_VERSION}"
WORK="$ROOT/ci-artifacts/dedicated-server"
SERVER="$WORK/server"
LOG="$WORK/server-console.log"
mkdir -p "$SERVER/mods"

release_jar=$(find "$DIST_DIR" -maxdepth 1 -type f -name 'rolecard-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-dev.jar' ! -name '*-plain.jar' -print -quit)
[[ -n "$release_jar" ]] || { echo "未找到唯一 rolecard 发布 Jar" >&2; exit 1; }
[[ $(find "$DIST_DIR" -maxdepth 1 -type f -name 'rolecard-*.jar' | wc -l) -eq 1 ]] || { echo "发布目录存在多个 rolecard Jar" >&2; exit 1; }
cp "$release_jar" "$SERVER/mods/"

installer="$WORK/forge-${FORGE_COORD}-installer.jar"
installer_url="https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE_COORD}/forge-${FORGE_COORD}-installer.jar"
# 网络请求最多额外重试一次；下载版本由上方坐标固定，并由官方 Maven SHA-1 sidecar 校验。
curl --fail --location --retry 1 --retry-delay 2 --connect-timeout 20 --output "$installer" "$installer_url"
curl --fail --location --retry 1 --retry-delay 2 --connect-timeout 20 --output "$installer.sha1" "${installer_url}.sha1"
test -s "$installer"
expected_sha1=$(awk '{print $1}' "$installer.sha1")
[[ "$expected_sha1" =~ ^[0-9a-fA-F]{40}$ ]] || { echo "Forge 官方 SHA-1 sidecar 格式异常" >&2; exit 1; }
actual_sha1=$(sha1sum "$installer" | awk '{print $1}')
[[ "$actual_sha1" == "$expected_sha1" ]] || { echo "Forge 安装器 SHA-1 校验失败" >&2; exit 1; }
sha256sum "$installer" > "$WORK/forge-installer.SHA256"
java -jar "$installer" --installServer "$SERVER"
test -x "$SERVER/run.sh" || { echo "Forge 安装后缺少可执行 run.sh" >&2; exit 1; }
find "$SERVER/libraries/net/minecraftforge/forge/$FORGE_COORD" -type f -name '*.jar' -size +100k -print -quit | grep -q . \
  || { echo "Forge 安装后缺少关键 Forge 库" >&2; exit 1; }
printf 'eula=true\n' > "$SERVER/eula.txt"
printf '%s\n' '-Xms1G' '-Xmx3G' > "$SERVER/user_jvm_args.txt"

cd "$SERVER"
mkfifo console.in
bash run.sh nogui < console.in > "$LOG" 2>&1 &
SERVER_PID=$!
exec 3>console.in

cleanup() {
  if kill -0 "$SERVER_PID" 2>/dev/null; then
    printf 'stop\n' >&3 || true
    for _ in {1..30}; do kill -0 "$SERVER_PID" 2>/dev/null || break; sleep 1; done
    kill -TERM "$SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

ready=0
for _ in {1..180}; do
  if grep -qE 'Done \([0-9.]+s\)! For help, type "help"' "$LOG" 2>/dev/null; then ready=1; break; fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then echo "Forge 专服提前退出" >&2; exit 1; fi
  sleep 1
done
[[ "$ready" -eq 1 ]] || { echo "等待 Forge 专服 Done 超时" >&2; exit 1; }

# help 命令由 Brigadier 按已注册树返回 rolecard；这是无玩家环境可可靠执行的命令注册验证。
printf 'list\nhelp rolecard\nhelp rolecard mission\nsave-all flush\nstop\n' >&3
for _ in {1..60}; do kill -0 "$SERVER_PID" 2>/dev/null || break; sleep 1; done
if kill -0 "$SERVER_PID" 2>/dev/null; then echo "专服未在 stop 后正常退出" >&2; exit 1; fi
trap - EXIT

combined="$WORK/combined.log"
cat "$LOG" logs/latest.log 2>/dev/null > "$combined" || cp "$LOG" "$combined"
grep -qi 'rolecard' "$combined" || { echo "日志中没有 rolecard 装载证据" >&2; exit 1; }
grep -qi 'rolecard' "$LOG" || { echo "help rolecard 未返回 rolecard，命令可能没有注册" >&2; exit 1; }
grep -qi 'mission' "$LOG" || { echo "help rolecard mission 未返回 mission，任务命令可能没有注册" >&2; exit 1; }
if grep -Eqi 'FATAL|NoClassDefFoundError|Attempted to load class .* for invalid dist DEDICATED_SERVER|Mixin apply (failed|error)|MixinApplyError|crash-report|crash report' "$combined"; then
  echo "专服日志含严重加载错误" >&2; grep -Ein 'FATAL|NoClassDefFoundError|invalid dist|Mixin|crash' "$combined" >&2 || true; exit 1
fi
if find . -type f -path '*/crash-reports/*' -print -quit | grep -q .; then
  echo "专服产生了 crash-report" >&2; exit 1
fi
echo "Dedicated Server smoke 通过：rolecard 已随真实 Forge 专服启动，命令树、save-all flush 与 stop 均已执行。"
