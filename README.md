# 角色卡与六维（RoleCard）

[![Forge 1.20.1 CI](https://github.com/SGSxingchen/rolecard-forge-1.20.1/actions/workflows/ci.yml/badge.svg)](https://github.com/SGSxingchen/rolecard-forge-1.20.1/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/SGSxingchen/rolecard-forge-1.20.1)](https://github.com/SGSxingchen/rolecard-forge-1.20.1/releases/latest)

适用于 **Minecraft 1.20.1 / Forge / Java 17** 的角色身份卡与六维属性模组。

玩家可以设置角色名称、年龄和性别；角色名可显示在聊天栏、头顶和 Tab 玩家列表中。模组还提供壮硕、力量、协调、反应、精神、幸运六项属性，并将它们转换为实际的原版属性增益。

## 一分钟上手

### 1. 下载

前往 [Releases](https://github.com/SGSxingchen/rolecard-forge-1.20.1/releases/latest)，下载：

```text
rolecard-forge-1.20.1-1.0.0.jar
```

不要下载 Source code，也不要下载 `.sha256` 校验文件当作模组。

### 2. 安装

需要：

- Minecraft 1.20.1
- Forge 1.20.1（开发和 CI 使用 Forge 47.4.10）
- Java 17

把 Jar 放进游戏目录的 `mods` 文件夹。

多人服务器必须在 **服务端和所有玩家客户端** 都安装同一个 Jar。它没有额外前置模组。

### 3. 打开角色卡

进入游戏或服务器后，按 **I** 打开“角色档案”界面。

在界面中填写：

- 角色名称
- 年龄（0–999）
- 性别

点击“确认保存”即可。默认按键可以在：

```text
选项 → 控制 → 按键绑定 → 角色卡与六维
```

中修改。

### 4. 六维怎么加

当前版本不允许玩家自行分配六维，由服务器管理员通过命令调整。

六维包括：

- 壮硕 `bulk`
- 力量 `strength`
- 协调 `coordination`
- 反应 `reflex`
- 精神 `spirit`
- 幸运 `luck`

玩家可以在角色卡界面查看当前数值；将鼠标悬停在属性上可以查看实际增益。

## 常用管理员命令

修改命令需要 OP 权限等级 2。

查看自己的角色卡：

```text
/rolecard card
```

查看指定玩家：

```text
/rolecard card <玩家>
```

设置玩家身份，参数顺序是“玩家、年龄、性别、角色名称”：

```text
/rolecard identity set <玩家> <年龄> <性别> <角色名称>
```

示例：

```text
/rolecard identity set Steve 24 男 林默
```

直接设置六维：

```text
/rolecard stat set <玩家> <六维键> <数值>
```

示例：

```text
/rolecard stat set Steve strength 20
```

增加或扣除六维：

```text
/rolecard stat add <玩家> <六维键> <增量>
```

示例：

```text
/rolecard stat add Steve luck 5
/rolecard stat add Steve spirit -3
```

只重置六维：

```text
/rolecard stat reset <玩家>
```

重置整张角色卡：

```text
/rolecard reset <玩家>
```

六维最终保存范围为 **0–100**。

## 名称显示配置

首次启动服务器后，配置文件位于：

```text
world/serverconfig/rolecard-server.toml
```

主要配置：

```toml
[display]
showOverheadRoleName = true
showChatRoleName = true
tabNameMode = "ROLE"
```

- `showOverheadRoleName`：是否在玩家头顶显示角色名。
- `showChatRoleName`：聊天栏是否使用角色名。
- `tabNameMode = "VANILLA"`：Tab 显示原版游戏名。
- `tabNameMode = "ROLE"`：Tab 显示角色名，默认模式。
- `tabNameMode = "HIDDEN"`：隐藏 Tab 中的名称文字，但不会删除玩家条目。

修改后重启服务器使配置生效。

模组只修改显示名称，不会修改玩家的 GameProfile、UUID、登录名、白名单、封禁记录、命令选择器或正版验证身份。

## 六维实际效果

| 六维 | 当前影响的原版属性 |
| --- | --- |
| 壮硕 | 最大生命、护甲、击退抗性 |
| 力量 | 攻击伤害、攻击击退 |
| 协调 | 移动速度 |
| 反应 | 攻击速度、移动速度 |
| 精神 | 护甲韧性、少量幸运 |
| 幸运 | 幸运 |

一项六维可以映射到多个 Forge Attribute。映射使用稳定 Modifier UUID，不会反复叠加，也不会清空其他模组添加的属性修正。

## 常见问题

### 按 I 没反应

确认客户端也安装了模组，并在按键设置中检查“打开角色档案”是否与其他按键冲突。

### 服务器装了，玩家还需要装吗

需要。服务端负责保存和校验数据，客户端负责角色卡界面和名称显示，双方都要安装。

### 为什么不能自己加六维

v1.0.0 默认由管理员统一管理六维，避免玩家自行修改数值。管理员使用 `/rolecard stat` 命令调整。

### 为什么聊天名、头顶名或 Tab 名没有变化

先确认角色名称不是空白，再检查 `rolecard-server.toml`。如果整合包中还有其他修改玩家名称的模组，最终显示可能受 Forge 事件顺序影响，需要实测兼容性。

### 会修改正版账号名吗

不会。角色名只是展示层名称，不影响登录和服务器身份系统。

### 数据保存在哪里

数据由服务端按玩家 UUID 持久化。死亡重生、退出重连和切换维度时会复制、同步并重新应用属性。

### 包含跨任务世界、周目和道具继承吗

不包含。这些功能由外部任务系统负责，本模组只处理角色卡、六维、名称显示、管理命令和玩家数据。

## 下载与校验

正式版本：[v1.0.0 Release](https://github.com/SGSxingchen/rolecard-forge-1.20.1/releases/tag/v1.0.0)

```text
文件：rolecard-forge-1.20.1-1.0.0.jar
SHA-256：635be07f05bf8443e6a1fc75814803be89d850b326c7b5003b99b9cf5b30a22e
```

## 开发与验证

CI 会执行静态质量门禁、Java 17 构建、真实 Forge 专用服务器启动、Xvfb + Mesa 客户端冒烟和回归报告。v1.0.0 对应 CI 已全部通过。

仍建议服主人工验证聊天栏、头顶、Tab 三种模式、角色卡 UI、双客户端显示，以及与整合包内其他名称/属性模组的兼容性。

更多资料：

- [完整使用与扩展说明](docs/使用与扩展说明.md)
- [持续集成与验证矩阵](docs/持续集成与验证矩阵.md)
- [构建与人工验收](docs/构建与验证.md)
- [未来远端同步设计](docs/未来同步设计.md)

## 许可

以仓库中的许可证文件为准。
