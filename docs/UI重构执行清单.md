# UI 重构执行清单（v1.1.1）

本文是角色卡 UI 重构开始前的仓库审查结论与交付清单，不替代面向使用者的
[`UI重构说明.md`](UI重构说明.md)。

## 审查基线（2026-08-05）

- 当前分支为 `main`，与 `origin/main` 同步，基线提交为 `25220c9`，已有历史标签
  `v1.1.0`；该历史标签和既有 Release 不得移动、覆盖或删除。
- 当前版本唯一构建来源为 `gradle.properties` 的 `mod_version=1.1.0`；
  `build.gradle` 与 `src/main/resources/META-INF/mods.toml` 会从该属性展开版本。
- 当前没有 `src/test` 单元测试源集。已有自动检查为 Python 静态/规则探针，以及仅验证
  主菜单稳定初始化的 Forge 客户端 Smoke；后者尚未打开角色卡或管理员界面。
- 工作区起始时已存在未跟踪文件 `docs/发布v1.1.0核验记录.md`，它是历史核验记录，
  不应在本次 UI 修复中改写为 v1.1.1 的结果。

## v1.1.1 必须同步更新的位置

| 位置 | 必做改动 | 注意事项 |
| --- | --- | --- |
| `gradle.properties` | 将 `mod_version` 从 `1.1.0` 改为 `1.1.1`。 | 这是发布 Jar 与 `mods.toml` 的唯一版本来源。 |
| `README.md` | 更新简介中的版本叙述与旧的“候选/不创建 Release”提示；新增新版玩家界面使用说明、状态反馈和锁定/退回说明；链接到 UI 说明。 | 不提交聊天截图；如需截图仅写入“本地截屏位置/人工验收指引”。 |
| `AGENTS.md` | 在项目索引增加 v1.1.1 UI 重构说明链接，并把“实施范围”索引改为同时覆盖 v1.1.1。 | 这是仓库的文档总索引，代码与文档需同频。 |
| `docs/UI重构说明.md` | 按需求先落盘：玩家/管理员线框、视觉 token、间距、安全区、控件状态、响应式与验收边界。 | 这是面向维护者与验收者的正式 UI 说明。 |
| `docs/使用与扩展说明.md` | 更新玩家填写、锁定阅读、管理员审核、反馈与人工验收章节。 | 数据、网络和状态机语义不变时应明确写明“仅 UI 改造”。 |
| `docs/实施计划.md` | 新增 v1.1.1 UI 修复交付项和完成记录。 | 不篡改 v1.1.0 历史范围。 |
| `docs/构建与验证.md`、`docs/持续集成与验证矩阵.md` | 写入新增布局纯函数测试、四组分辨率断言、客户端界面探针的真实覆盖范围和仍需人工视觉验收项。 | 不得把“初始化成功”描述成“像素美观已验证”。 |
| `.github/workflows/ci.yml`、`scripts/ci/*` | 若加入布局/UI 探针，接入 `quality-gates` 或新增明确 Job，并同步工作流中的 v1.1.0 文案。 | 保留既有 v1.1.0 业务规则探针；其文件名可因历史语义继续保留。 |
| `docs/发布v1.1.0核验记录.md` | 不修改。 | 本轮明确不创建 v1.1.1 Release、tag 或发布核验记录。 |

## 实现与验证清单

- [ ] 完整审查 `RoleCardScreen`、`AdminRoleCardScreen`、`MultiLineBiographyBox` 及入口/网络
      调用点，先将结论写入 `docs/UI重构说明.md`，再改 UI。
- [ ] 新建共享视觉 token、绘制辅助层、可访问的档案风按钮与布局模型；避免 Screen 中散落
      颜色和坐标常量。
- [ ] 保持客户端类仅在 `com.rolecard.client`，不得改变服务端权威、包方向、状态机、DTO
      语义或存档格式，除非为 UI 交互适配所必需且有对应说明。
- [ ] 为布局模型添加不依赖 Minecraft 窗口的自动测试：320×240、427×240、854×480、
      1920×1080 均须断言安全区内、矩形不相交、页签不压说明、内容不压底栏，且交互矩形
      可点击。
- [ ] 扩展 CI 客户端探针时，依次验证玩家身份/生平/六维页与管理员 Screen 的初始化、切页、
      Widget 边界和稳定 tick；探针必须仍位于 `src/ci`，不得进入发布 Jar。
- [ ] 人工在实际 Forge 客户端复核中文换行、焦点/旁白/Tab、tooltip 避让、HUD 安全边距、
      小屏滚动与真实审批/退回反馈；自动测试不能替代此项。

## 可运行检查与 CI 命令

### 本地允许的静态检查

```bash
python3 scripts/ci/quality_gates.py
python3 scripts/ci/rolecard_v110_logic.py
git diff --check
git status --short --branch
```

本次审查已运行前两项，均通过。仓库文档规定本机**不运行** Gradle、Java、Forge 或
Minecraft 动态测试；完整构建与真实运行仅由 GitHub Hosted Runner 执行。

### GitHub Actions 必需全绿项

推送 `main` 后，工作流 `.github/workflows/ci.yml` 会运行：

1. `quality-gates`：资源、元数据、侧别、网络、Wrapper 与扩展后的 UI 静态/布局门禁；
2. `build`：Java 17 的 `./gradlew --no-daemon clean build` 与唯一发布 Jar 筛选；
3. `dedicated-server`：真实 Forge 47.4.10 专服 Smoke；
4. `client-smoke`：Xvfb/Mesa Forge 客户端及新增的界面探针；
5. `regression-report`：拒绝任一上述 Job 失败、跳过或缺失。

CI 地址固定为：
`https://github.com/SGSxingchen/rolecard-forge-1.20.1/actions/workflows/ci.yml`。
完成推送后应在最终汇报中提供本次具体 Actions run URL，不能引用 v1.1.0 的历史 run。

## Git、推送与发布边界

- 远端为 `origin`：`git@github.com:SGSxingchen/rolecard-forge-1.20.1.git`，目标分支为 `main`。
- 提交前先检查共享工作区是否有其他 Agent 的改动；只暂存本次 UI、测试与同步文档，避免把
  构建目录、缓存、聊天截图或无关未跟踪文件带入提交。
- 使用中文提交信息，例如 `fix: 重构角色卡档案界面`；推送前再次确认 `git diff --check` 和
  两项静态探针通过。
- 仅在工作树确认无冲突且 `main` 仍是目标分支时执行 `git push origin main`。推送触发 CI 后，
  等五个必需 Job 全绿再报告构建结果。
- 本轮禁止创建 tag、GitHub Release 或上传 Release 附件；待主人验收构建结果后另行决定。
