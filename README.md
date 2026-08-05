# 角色卡与六维（RoleCard）

[![Forge 1.20.1 CI](https://github.com/SGSxingchen/rolecard-forge-1.20.1/actions/workflows/ci.yml/badge.svg)](https://github.com/SGSxingchen/rolecard-forge-1.20.1/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/SGSxingchen/rolecard-forge-1.20.1)](https://github.com/SGSxingchen/rolecard-forge-1.20.1/releases/latest)

适用于 **Minecraft 1.20.1 / Forge / Java 17** 的角色身份卡与六维属性模组。v1.1.2 在 v1.1.1 档案界面基础上，修复属性增益文本泄漏内部注册键的问题。

> v1.1.2 让属性名称按每位玩家客户端语言显示；本轮只推送代码并等待 CI，不创建 tag 或 Release。

## 一分钟上手：从发点到批准

1. **管理员发点**（权限等级 2）：
   ```text
   /rolecard points set Steve 12
   ```
2. **玩家按 I** 打开“角色档案”，依次填写身份、人物生平，并在六维页使用 `+` / `-` 分配自己剩余的点数。
3. 玩家确认预览无误后点击 **提交角色卡**。服务端重新核算本次支出，卡片进入“待审核”。
4. 在线管理员会收到醒目、可点击的审核通知；点击即可打开该玩家的管理面板。管理员离线时，登录会收到待审核数量提示。
5. 管理员查看身份、生平、六维、剩余点数和状态后，批准或退回。退回可填写简短原因；玩家收到结果后修改并再次提交。

服务端是唯一权威：客户端只提交编辑意图，不能伪造总点数、目标玩家、审核结论或旧版本覆盖新资料。

## 安装

需要：

- Minecraft 1.20.1
- Forge 1.20.1（开发和 CI 使用 Forge 47.4.10）
- Java 17

把同一个 Jar 放进服务器和所有玩家客户端的 `mods` 文件夹；没有额外前置模组。专用服务器只保存与校验数据，不会加载客户端界面类。

当前正式版下载仍见 [Releases](https://github.com/SGSxingchen/rolecard-forge-1.20.1/releases/latest)。v1.0.0 文件和校验信息保留在本文“下载与校验”一节，不能把 Source code 或 `.sha256` 文件当成模组安装。

默认按键为 **I**，可在：

```text
选项 → 控制 → 按键绑定 → 角色卡与六维
```

中修改。

## v1.1.2 档案与属性显示

玩家按 **I** 后会看到居中的“角色档案册”：标题栏只显示状态与修订号，六维页单独突出剩余点数；身份、生平、六维使用统一页签，底部的关闭、保存草稿、提交角色卡有清晰主次。小屏会保留安全边距，六维属性卡会切换为单列并在内容区滚动。

- 身份页：名称为主字段，年龄和性别并排；待审核/批准时改为只读排版，不伪装成可输入框。
- 生平页：支持换行、滚动、字数计数；阅读状态使用裁剪阅读视图。
- 六维页：每项都有当前/拟调整值、实际增益摘要和可键盘操作的 `− 数值 +` 步进器；摘要会随本轮草案即时预览。
- 属性名称优先采用 Minecraft 客户端自身翻译，所以中文客户端显示“最大生命值”、英文客户端显示“Max Health”；没有原版翻译的未来属性会使用角色卡的语言兜底，再退回为友好名称。
- 悬停属性卡会按行显示全部增益：固定值使用 `+2.40`，乘法使用 `+2.4%`；按住 Shift 才会显示高级内部标识，普通界面不会显示 `max_health` 一类技术键。
- 管理员面板：资料、六维、审核分区，六维同样预览编辑框中的拟提交属性增益，并显示原版名、UUID 摘要、状态、修订号与退回原因。

自动检查只验证布局与界面初始化；请按[UI 重构说明](docs/UI重构说明.md)在实际客户端人工检查中文换行、HUD、tooltip 与视觉效果。仓库不包含用户聊天截图。

## 角色卡内容与默认规则

### 身份与生平

- 身份包含角色名称、年龄（0–999）和性别。
- 人物生平/人设介绍支持换行和滚动编辑，默认最多 **1500** 个字符；界面显示字数。服务端过滤控制字符和格式字符，并限制网络文本长度。
- 空角色名称会回退显示原版游戏名；角色名和生平都不会改变 GameProfile、UUID、登录名、白名单、封禁记录、命令选择器或正版验证身份。

### 六维与点数

六维为壮硕 `bulk`、力量 `strength`、协调 `coordination`、反应 `reflex`、精神 `spirit`、幸运 `luck`。每项范围都是 **0–100**。

- `availablePoints`（剩余点数）由服务器保存和发放；不是客户端可填写的字段。
- `+` 只能消费实际剩余点数，`-` 只能撤回当前卡中可撤回的数值。界面同时显示当前值、拟提交变化、剩余点数及基于拟提交值的悬停实际增益。
- 管理员直接 `stat set/add/reset` 是管理操作，不等同于玩家从池中消费点数；管理员可以另行调整剩余点数。

### 审核状态

| 状态 | 含义与玩家权限 |
| --- | --- |
| `DRAFT`（草稿） | 可编辑身份、生平和六维，尚未进入审核。 |
| `PENDING`（待审核） | 已提交等待管理员。默认锁定，避免同一提交在审核期间被静默改写。 |
| `APPROVED`（已批准） | 玩家资料与点数分配锁定，玩家不可自行洗点或改身份/生平。 |
| `REJECTED`（被退回） | 显示管理员原因；默认允许修改并重新提交。 |

`PENDING` 是否允许玩家继续改草稿由服务端配置 `workflow.allowDraftEditsWhilePending` 控制，默认 `false`；被退回后的编辑由 `workflow.allowRejectedEdits` 控制，默认 `true`。管理员可以退回或解锁已批准的卡。每次有效更新都会增加修订号，旧窗口、断线前数据或重放旧包不能覆盖新版本。

## 管理员命令

所有修改、审核和指定玩家查看命令均要求原版权限等级 2；命令会提供玩家与六维键补全，并对离线目标给出明确错误。既有命令继续可用。

```text
/rolecard card [玩家]                 查看自己的卡；管理员可查看指定玩家
/rolecard identity set <玩家> <年龄> <性别> <角色名称...>
/rolecard stat set <玩家> <六维键> <数值>
/rolecard stat add <玩家> <六维键> <增量>
/rolecard stat reset <玩家>
/rolecard reset <玩家>

/rolecard points set <玩家> <数值>    设置剩余可分配点数
/rolecard points add <玩家> <数值>    增减剩余可分配点数
/rolecard review list                 列出持久化待审核卡
/rolecard review open <玩家>          打开在线目标的审核面板
/rolecard review approve <玩家>       批准角色卡
/rolecard review reject <玩家> [原因...]
/rolecard review unlock <玩家>        解锁/退回后允许玩家再编辑
```

示例：

```text
/rolecard points add Steve 5
/rolecard review reject Steve 生平请补充与队伍的关系
/rolecard review approve Steve
```

管理员通知包含原版游戏名、角色名、提交时间和状态。点击通知使用原生可点击文本并通过命令打开目标卡；管理面板的所有保存、批准、退回仍会回到服务端二次验权与验版本，不能信任客户端传来的 DTO。

## 名称显示与实际属性效果

首次启动服务器后，配置文件位于：

```text
world/serverconfig/rolecard-server.toml
```

显示配置：

```toml
[display]
showOverheadRoleName = true
showChatRoleName = true
tabNameMode = "ROLE"
```

- `showOverheadRoleName`：是否在玩家头顶显示角色名。
- `showChatRoleName`：聊天栏是否使用角色名。
- `tabNameMode = "VANILLA"`：Tab 显示原版游戏名。
- `tabNameMode = "ROLE"`：Tab 显示角色名（默认）。
- `tabNameMode = "HIDDEN"`：隐藏 Tab 的名称文字，不会删除玩家条目。

流程配置：

```toml
[workflow]
maxBiographyLength = 1500
allowDraftEditsWhilePending = false
allowRejectedEdits = true
```

六维会映射为原版属性：壮硕（最大生命、护甲、击退抗性）、力量（攻击伤害、攻击击退）、协调（移动速度）、反应（攻击速度、移动速度）、精神（护甲韧性、少量幸运）、幸运（幸运）。名称在客户端翻译，普通摘要不显示注册键；Shift 高级 tooltip 才显示标识。映射使用稳定 Modifier UUID：只移除并重加本模组自己的修正，不清空其他模组修正，也不会反复叠加。

## 旧档迁移与边界

角色卡数据版本升级为 **2**。读取 v1.0.0 NBT 时，既有角色名称、年龄、性别和六维全部保留；新增生平、剩余点数、退回原因和修订号使用安全默认值。含既有六维的旧卡默认标为已批准并锁定，防止升级后把原管理员数值洗成可用点数；全零六维旧卡为草稿。缺键、损坏文本和超范围六维会被过滤或钳制，不会把可编辑资料混入真实 UUID 或游戏名。

待审核记录与角色卡持久化，因此玩家退出后待审核不会丢失；同一卡反复提交是幂等更新，不会产生无限通知或队列条目。审核界面当前以在线目标为交互对象；离线卡可通过 `review list` 发现，待目标在线后再打开审核面板。详见[使用与扩展说明](docs/使用与扩展说明.md)。

本模组不实现数据库、网页、跨任务世界、周目、奖励、道具继承或远端账号验证。两客户端名称显示与其他同类模组的兼容性仍需人工联机验收。

## 开发与验证

真实 Gradle、Forge 专服、Xvfb/Mesa 客户端与逻辑探针仅在 GitHub Hosted Runner 执行；本机不运行 Gradle、Java、Forge 或 Minecraft。CI 必需 Job 为 `quality-gates`、`build`、`dedicated-server`、`client-smoke` 和 `regression-report`，最后一个会拒绝任一 Job 失败、跳过或缺失。

自动探针覆盖 v1 迁移、生平边界、点数守恒、伪造/重放提交、状态机、修订冲突、重复待审幂等、管理员权限、属性 Modifier 不重复，以及 v1.1.2 的九项中英属性名兜底、运算格式、预览和多行 tooltip；真实多人交互、GUI 各分辨率和整合包兼容仍需要人工验收。详细范围见：[持续集成与验证矩阵](docs/持续集成与验证矩阵.md) 与 [构建与验证](docs/构建与验证.md)。

## 下载与校验（v1.0.0 历史正式版）

正式版：[v1.0.0 Release](https://github.com/SGSxingchen/rolecard-forge-1.20.1/releases/tag/v1.0.0)

```text
文件：rolecard-forge-1.20.1-1.0.0.jar
SHA-256：635be07f05bf8443e6a1fc75814803be89d850b326c7b5003b99b9cf5b30a22e
```

更多资料：

- [完整使用与扩展说明](docs/使用与扩展说明.md)
- [持续集成与验证矩阵](docs/持续集成与验证矩阵.md)
- [构建与人工验收](docs/构建与验证.md)
- [未来远端同步设计](docs/未来同步设计.md)

## 许可

以仓库中的许可证文件为准。
