# JibaiWorkbench 使用 WIKI

> 面向服主的实操教程。全程只改 YAML、执行指令，不需要写 Java 代码。
> 作者：即白 · jibai0517@gamil.com

菜单文件都在 `plugins/JibaiWorkbench/menus/`，一个菜单一个 `.yml`。改完记得 `/jwb reload`。

---

## 目录

1. [5 分钟搭建主菜单](#一5-分钟搭建主菜单)
2. [如何创建商店](#二如何创建商店)
3. [如何创建 VIP 菜单](#三如何创建-vip-菜单)
4. [如何创建奖励页面](#四如何创建奖励页面)
5. [如何写按钮动作](#五如何写按钮动作)
6. [如何写显示条件与点击条件](#六如何写显示条件与点击条件)
7. [如何接入 Vault（经济）](#七如何接入-vault经济)
8. [如何接入 PlaceholderAPI](#八如何接入-placeholderapi)
9. [如何排查配置错误](#九如何排查配置错误)
10. [菜单配置字段速查](#十菜单配置字段速查)

---

## 一、5 分钟搭建主菜单

第 1 步：把 `JibaiWorkbench-1.0.0.jar` 放进 `plugins/`，重启服务器。首次启动会自动在 `plugins/JibaiWorkbench/menus/` 生成 8 套默认菜单。

第 2 步：游戏内输入 `/jwb open main`，主菜单就出来了。

第 3 步：想改？编辑 `menus/main.yml`。比如把标题改掉：

```yaml
title: "&b&l我的服务器 &8» &f主菜单"
```

第 4 步：保存后执行 `/jwb reload`，再 `/jwb open main` 看效果。

第 5 步：想校验有没有写错，执行 `/jwb validate`。

> 提示：所有 YAML 必须用 **UTF-8** 保存，否则中文会乱码。推荐用 VS Code / Notepad++，不要用 Windows 记事本另存为 ANSI。

---

## 二、如何创建商店

### 1. 从模板生成

```
/jwb create shop myshop
```

会生成 `menus/myshop.yml`。

### 2. 添加一个商品按钮

商品按钮的关键是 `shop:` 段落。插件会自动处理扣款、发货、限购、库存，你**不需要**自己写扣钱动作（那样容易扣款成功但发货失败）。

```yaml
buttons:
  diamond:
    slot: 10
    material: DIAMOND
    name: "&b钻石 x8"
    lore:
      - "&7购买价：&a100 金币"
      - "&7出售价：&e60 金币"
      - ""
      - "&e左键购买 &7/ &e右键出售"
    shop:
      buy-price: 100.0      # 购买价，>=0，0=免费领取
      sell-price: 60.0      # 出售价，省略=不可出售
      give-item: "DIAMOND:8" # 购买后给的物品 材质:数量
      daily-limit: 10       # 每日限购次数，0=不限
      cooldown-sec: 0       # 两次购买冷却秒数
      stock: -1             # 全服库存，-1=无限
      confirm: false        # true=购买前弹确认页
```

### 3. 需要执行指令的商品（如买 VIP）

```yaml
    shop:
      buy-price: 1000.0
      once: true            # 一次性购买
      confirm: true
      commands:
        - "lp user {player} parent add vip"   # {player} 会替换成买家名，控制台执行
```

### 4. 出售

玩家**右键**带 `sell-price` 的商品即出售。出售会检查玩家背包里是否真的有对应物品。

> 商店需要 Vault + 经济插件（如 EssentialsX）。没装时按钮会提示「经济功能不可用」，不会报错。

---

## 三、如何创建 VIP 菜单

```
/jwb create vip vip
```

VIP 菜单的核心是**按权限显示不同按钮**，用 `view-condition`：

```yaml
buttons:
  # 只有 VIP 才看得到这个按钮
  vip_only:
    slot: 15
    material: NETHER_STAR
    name: "&d&lVIP 专属指令"
    view-condition:
      - "permission: jibaiworkbench.reward.vip"
    actions:
      - "player-command: kit vip"

  # 显示玩家当前权限组
  status:
    slot: 4
    material: PLAYER_HEAD
    name: "&f%player_name% 的会员状态"
    lore:
      - "&7当前权限组：&b{group}"    # 接入 LuckPerms 后显示真实组名
      - "&7金币：&e{balance}"
```

VIP 每日奖励用 `reward:` 段落（见下一节），配合 `permission: jibaiworkbench.reward.vip` 限制领取。

---

## 四、如何创建奖励页面

```
/jwb create reward reward
```

奖励按钮的核心是 `reward:` 段落。插件自动记录领取状态与冷却，你**不需要**自己写「标记已领取」动作。

```yaml
buttons:
  daily:
    slot: 10
    material: CHEST
    name: "&a每日奖励"
    lore:
      - "&7每天可领取一次"
      - "&e» 点击领取"
    reward:
      key: "daily"           # 奖励唯一键，用来记录领取状态
      type: "daily"          # daily=每日 weekly=每周 once=一次性 playtime=在线时长
      give-item: "DIAMOND:2"
      commands:
        - "give {player} bread 8"

  playtime:
    slot: 16
    material: CLOCK
    name: "&e在线满 60 分钟奖励"
    reward:
      key: "playtime_60"
      type: "playtime"
      playtime-min: 60       # 在线满 60 分钟才能领
      give-item: "GOLD_INGOT:5"
```

奖励类型说明：

| type | 含义 | 可再次领取时间 |
|------|------|---------------|
| daily | 每日奖励 | 次日 0 点后 |
| weekly | 每周奖励 | 下周一 0 点后 |
| once | 一次性 | 永不 |
| playtime | 在线时长 | 达到 playtime-min 后可领一次 |

领取记录保存在 `data/players.yml`，玩家重进服务器后仍然有效。

---

## 五、如何写按钮动作

动作写在 `actions:` 列表里，格式统一为字符串 `"类型: 参数"`，按顺序执行。

```yaml
    actions:
      - "sound: UI_BUTTON_CLICK"
      - "message: &a欢迎光临！"
      - "open: shop"
```

全部动作类型：

| 动作 | 格式 | 说明 |
|------|------|------|
| 打开菜单 | `open: <菜单ID>` | 跳到另一个菜单 |
| 关闭菜单 | `close:` | 关闭当前菜单 |
| 返回菜单 | `back:` | 返回上一个菜单 |
| 玩家指令 | `player-command: <指令>` | 以玩家身份执行（不带 /） |
| 控制台指令 | `console-command: <指令>` | 控制台执行，受 config 安全开关控制 |
| 聊天消息 | `message: <文本>` | 发聊天消息 |
| 标题 | `title: <主标题>\|<副标题>` | 用 `\|` 分隔主副标题 |
| 动作栏 | `actionbar: <文本>` | 屏幕下方动作栏 |
| 音效 | `sound: <音效>[:音量:音调]` | 如 `sound: ENTITY_PLAYER_LEVELUP:1:1` |
| 扣钱 | `take-money: <数量>` | 需 Vault |
| 给钱 | `give-money: <数量>` | 需 Vault |
| 给物品 | `give-item: <材质>:<数量>` | 背包满会掉落地上 |
| 传送 | `teleport: <世界>,<x>,<y>,<z>[,<yaw>,<pitch>]` | 传送到坐标 |
| 跳服 | `server: <子服名>` | 需 BungeeCord/Velocity，且 config 开启 |
| 冷却 | `cooldown: <键>:<秒>` | 给某操作设冷却 |
| 标记奖励 | `mark-reward: <键>` | 手动标记奖励已领取 |

### 按点击类型区分动作

除了 `actions`（任意点击都触发），还支持：

```yaml
    left-actions:        # 只左键
      - "open: shop"
    right-actions:       # 只右键
      - "message: &7这是右键功能"
    shift-left-actions:  # Shift+左键
      - "player-command: spawn"
    shift-right-actions: # Shift+右键
      - "close:"
```

特定点击类型动作优先于通用 `actions`。

---

## 六、如何写显示条件与点击条件

- `view-condition`：不满足则按钮**不显示**。
- `click-condition`：不满足则**点击无效**并提示。

```yaml
    view-condition:
      - "permission: jibaiworkbench.reward.vip"   # 有此权限才显示
    click-condition:
      - "money: 100"                              # 金币>=100 才能点
```

条件类型：

| 条件 | 格式 | 说明 |
|------|------|------|
| 有权限 | `permission: <节点>` | 拥有该权限 |
| 无权限 | `not-permission: <节点>` | 不拥有该权限 |
| 金币 | `money: <数量>` | 金币不少于（需 Vault） |
| 冷却结束 | `cooldown: <键>` | 指定冷却已结束 |
| 奖励未领 | `reward-unclaimed: <键>` | 奖励尚未领取 |

多个条件需**全部满足**。

---

## 七、可选前置依赖详解

JibaiWorkbench 本体**零强制依赖**，单独放进 `plugins/` 就能跑（菜单、动作、传送、指令都可用）。以下依赖全部是**可选软依赖**——装了才启用对应功能，不装也不会报错、不会导致插件加载失败。

### 7.0 依赖总览表

| 依赖 | 类型 | 影响的功能 | 不装的后果 |
|------|------|-----------|-----------|
| Vault | 软依赖 | 商店购买/出售、`take-money`/`give-money`/`money:` 条件、`{balance}` 变量 | 商店提示「经济功能不可用」，金币动作被跳过 |
| 经济插件（EssentialsX 等） | Vault 的前置 | 同上（Vault 只是桥梁，真正的钱由经济插件管） | 只装 Vault 不装经济插件 = 经济仍不可用 |
| PlaceholderAPI | 软依赖 | 解析 `%xxx%` 外部变量 | `%xxx%` 保持原文本显示 |
| LuckPerms | 软依赖 | `{group}` 显示真实权限组名、VIP 菜单按组显示 | `{group}` 显示 `default`，权限判断退回 Bukkit 原生 |
| BungeeCord/Velocity | 运行环境 | `server:` 跳服动作 | 跳服动作无效（且需在 config 手动开启开关） |

> 关键点：**这些依赖之间没有互相绑定**。你可以只装 Vault 不装 PAPI，也可以只装 LuckPerms 不装 Vault，按需组合。

---

### 7.1 接入 Vault（经济）—— 商店必备

Vault 本身**不是经济插件**，它只是一个「桥梁」，把你的经济插件和本插件连起来。所以要让商店能用，需要**两个**东西：

1. 下载 [Vault](https://www.spigotmc.org/resources/vault.34315/)（认准 Vault，不是 Vault**API**）放进 `plugins/`。
2. 装一个**真正管钱的经济插件**，最常见的是 **EssentialsX**（见 7.5）。
3. 重启服务器。启动横幅里 `Vault 已启用` 即成功。
4. 现在商店购买/出售、`take-money`/`give-money`/`money:` 条件、`{balance}` 变量都能用了。

**常见问题：**

- **装了 Vault 商店还是不可用？** 十有八九是**没装经济插件**，或经济插件没注册经济服务。控制台会有 `检测到 Vault，但没有经济插件注册经济服务` 的警告。
- **横幅显示 Vault 未启用？** 检查 Vault jar 是否真的在 `plugins/`、版本是否兼容你的 MC 版本、服务器启动日志里 Vault 是否报错。
- **金额显示成一串数字没有货币符号？** 这是经济插件的 `format` 设置，和本插件无关，去经济插件配置里改。

---

### 7.2 接入 PlaceholderAPI（变量扩展）

PlaceholderAPI（简称 PAPI）让你在菜单文本里用**其他插件提供的变量**，比如玩家称号、领地数量、击杀数等。

1. 下载 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) 放进 `plugins/`。
2. 用 `/papi ecloud download <扩展名>` 下载需要的扩展（如 `/papi ecloud download Player`），再 `/papi reload`。
3. 现在菜单文本里可以用 `%player_name%`、`%vault_eco_balance%`、`%luckperms_primary_group_name%` 等变量。

**内置变量（不装 PAPI 也能用）：**

| 变量 | 含义 |
|------|------|
| `{player}` | 玩家名 |
| `{world}` | 当前世界 |
| `{online}` | 在线人数 |
| `{max_players}` | 最大人数 |
| `{balance}` | 当前金币（需 Vault） |
| `{group}` | 权限组（需 LuckPerms 更准确） |
| `{date}` | 当前日期 |
| `{time}` | 当前时间 |

**注意事项：**

- 内置的 `{xxx}` 用**花括号**，PAPI 的 `%xxx%` 用**百分号**，两者可以在同一行文本里混用。
- 没装 PAPI 时，`%xxx%` **原样显示**（不会报错，也不会变空白）。所以如果你看到菜单里显示 `%player_name%` 这种原文，说明 PAPI 没装或对应扩展没下。
- PAPI 变量需要对应扩展。`%player_name%` 是内置的，但 `%vault_eco_balance%` 需要先 `/papi ecloud download Vault`。

---

### 7.3 接入 LuckPerms（权限组）

LuckPerms 是主流权限管理插件。本插件**不强制**它——不装也能用 Bukkit 原生权限（`op` 和 `permissions.yml`）。装了 LuckPerms 后：

1. 下载 [LuckPerms](https://luckperms.net/download) 放进 `plugins/`，重启。
2. `{group}` 变量会显示玩家真实的主要权限组名（如 `vip`、`admin`）。
3. 用 LuckPerms 给玩家/组分配 `jibaiworkbench.xxx` 权限，控制菜单显示与指令使用。

**用权限节点控制 VIP 菜单示例：**

```bash
# 给 vip 组分配领取 VIP 奖励的权限
/lp group vip permission set jibaiworkbench.reward.vip true
# 给某玩家临时加权限
/lp user 玩家名 permission set jibaiworkbench.reward.vip true
```

配合菜单里的 `view-condition: ["permission: jibaiworkbench.reward.vip"]`，只有 vip 组能看到该按钮（见第三、六节）。

**注意事项：**

- 不装 LuckPerms 时，`{group}` 一律显示 `default`，但**权限判断照常工作**（走 Bukkit 原生 `hasPermission`）。
- LuckPerms 和其他权限插件（PermissionsEx、GroupManager）**不要同时装**，会冲突。本插件只对接 LuckPerms。

---

### 7.4 跳服动作的前置（BungeeCord / Velocity）

`server:` 动作（点击按钮跳到另一个子服）需要：

1. 你的服务器在 **BungeeCord 或 Velocity 群组**里运行。
2. config.yml 里把 `actions.allow-server-jump` 改成 `true`（默认 false，安全考虑）。
3. 菜单动作写 `server: 子服名`（子服名要和代理配置里的一致）。

**注意事项：**

- 单服（没有代理）时，`server:` 动作无效，不会报错。
- 就算装了代理，`allow-server-jump` 默认关闭，必须手动开。

---

### 7.5 与 EssentialsX 共存

EssentialsX 是最常见的服务器基础插件，通常用它作为经济插件配合本插件的商店。

- **经济**：装 EssentialsX + Vault，商店即可用（EssentialsX 自带经济模块，会自动注册到 Vault）。
- **指令动作**：菜单动作里可以调 EssentialsX 的指令，如 `player-command: spawn`、`player-command: home`、`console-command: give {player} diamond 1`。
- **传送**：本插件的 `teleport:` 动作直接传坐标；如果你用 EssentialsX 的传送点，改用 `player-command: warp 传送点名` 更方便。
- **无冲突**：本插件不接管任何 EssentialsX 指令，两者各管各的。

---

### 7.6 与其他 GUI / 菜单插件共存（DeluxeMenus、ChestCommands 等）

本插件可以和 DeluxeMenus、ChestCommands 等其他菜单插件**同时安装**，互不干扰：

- 各插件的菜单是独立的 Inventory，本插件用自己的 `InventoryHolder`（`MenuSession`）识别归属，**不会误处理**其他插件的菜单点击。
- 指令不冲突：本插件主指令是 `/jworkbench` / `/jwb`，和 DeluxeMenus 的 `/dm`、ChestCommands 的 `/cc` 不重名。
- **注意**：如果你在本插件的菜单动作里调用其他菜单插件的打开指令（如 `player-command: dm open xxx`），点击后会关闭本插件菜单、打开对方菜单，这是正常的跨插件跳转。
- **PAPI 变量共享**：装了 PAPI 后，本插件和其他插件都能用同一套 PAPI 变量，不会互相抢占。

---

## 八、注意事项大全

开发和使用中最容易踩的坑，按重要性排列。

### 8.1 文件编码必须 UTF-8（最常见的坑）

- 所有 YAML 文件（config.yml、messages.yml、menus/*.yml）**必须用 UTF-8 编码保存**，否则中文会变乱码。
- **不要用 Windows 记事本**「另存为 ANSI/GBK」。推荐 VS Code、Notepad++（编码菜单选 UTF-8 无 BOM）、IntelliJ IDEA。
- 已经乱码的文件救不回来，只能删掉重新生成或重写。
- 判断方法：`/jwb reload` 后菜单标题/lore 里的中文如果是「锟斤拷」这类，就是编码错了。

### 8.2 音效名会随版本变化

- 音效动作 `sound: XXX` 和菜单的 `open-sound` / `close-sound` 用的是 **Bukkit Sound 枚举名**（全大写下划线，如 `BLOCK_NOTE_BLOCK_PLING`）。
- **音效名写错不会报错**，只是静默不播放（debug 模式下控制台会有 `未知音效名` 提示）。
- 不同 MC 版本音效名可能不同（如 1.20 和 1.21 有差异）。写之前建议查你的目标版本的 Sound 枚举，或用最常见的通用音效：`UI_BUTTON_CLICK`、`BLOCK_NOTE_BLOCK_PLING`、`ENTITY_EXPERIENCE_ORB_PICKUP`、`ENTITY_ENDERMAN_TELEPORT`。

### 8.3 Material 名会随版本变化

- 按钮的 `material:` 用 **Bukkit Material 枚举名**（如 `DIAMOND`、`GRAY_STAINED_GLASS_PANE`）。
- 本插件解析材质的顺序：先 `Material.matchMaterial()`（兼容命名空间写法如 `minecraft:diamond`），再退回 `Material.valueOf()`，**都失败则回退成 `STONE`** 并在 validate 里报错，不会崩溃。
- 不同版本材质名可能不同（如 1.13 之前用数字 ID，之后用名称；某些方块在新版本改了名）。目标 1.20+ 用名称即可。
- 写完用 `/jwb validate` 检查有没有 `Material 不存在` 的报错。

### 8.4 slot 槽位怎么算

- slot 从 **0 开始**，一行 9 格。
- 菜单总格数 = `rows × 9`。所以 3 行菜单的合法 slot 是 **0~26**，6 行是 **0~53**。
- slot 超出范围会在 validate 报错，该按钮被跳过。
- 布局速查（3 行为例）：第一行 0-8，第二行 9-17，第三行 18-26。正中间是 13。

### 8.5 rows 只能 1~6

- Minecraft 箱子界面最多 6 行，`rows` 写 7 及以上会报错并**自动回退成 3 行**。
- 写 0 或负数同样回退。

### 8.6 改了配置一定要 reload

- 编辑任何 YAML 后，必须 `/jwb reload` 才生效。
- reload 会**关闭所有玩家正在看的菜单**（防止旧菜单执行错误动作），这是正常行为，提醒玩家重新打开即可。
- reload 后建议顺手 `/jwb validate` 确认没写错。

### 8.7 权限默认值要注意

- `jibaiworkbench.use`、`.open`、`.shop.*`、`.reward.claim` 默认**所有人可用**（default: true）。
- 管理类权限（`.admin`、`.reload`、`.create` 等）默认**仅 OP**（default: op）。
- `.open.*`（打开任意菜单）和 `.reward.vip` 默认仅 OP。
- 如果你想让普通玩家打开某个受限菜单，给他 `jibaiworkbench.open.<菜单ID>`；想全放开就给 `jibaiworkbench.open.*`。

### 8.8 控制台指令动作有安全开关

- `console-command:` 动作以**控制台身份（最高权限）**执行，能跑 `op`、`stop` 这类危险指令。
- config.yml 的 `actions.allow-console-command` 默认 `true`，但**请确保你的菜单配置可信**，别让玩家能触发危险的控制台指令。
- 想彻底禁用，把它改成 `false`，此时所有 `console-command:` 动作被跳过并提示。
- 默认自带的模板里**没有任何危险的控制台指令**。

### 8.9 商店防连点与退款机制

- 商店购买有**防连点锁**：同一玩家同一商品的购买在处理期间会拒绝第二次点击，不会重复扣款。
- **先扣款、后发货**，如果发货失败（如背包问题）会**自动退款**并提示，玩家不会损失金币。
- 出售会先检查背包里是否真的有足够物品，不足则拒绝。
- 背包满时购买会提示背包已满（物品不会凭空消失）。

### 8.10 奖励领取记录的存储

- 玩家的奖励领取、限购、冷却记录存在 `plugins/JibaiWorkbench/data/players.yml`，**不在 config.yml**。
- **不要手动编辑 players.yml**（除非你知道自己在做什么），否则可能导致领取状态错乱。
- 想重置某玩家的领取记录，停服后删掉 players.yml 里对应 UUID 的段落，或直接删整个文件（会重置所有人）。
- 每日奖励按自然日算（跨天可再领），每周按自然周算，一次性奖励永久记录。

### 8.11 菜单跳转不会死循环

- 就算你配置了 A→B、B→A 的互相跳转，本插件的返回历史栈有**最大深度 16** 的限制，不会栈溢出。
- `back:` 动作在没有上一级时**什么都不做**（不报错）。

### 8.12 全核心兼容与版本

- 本插件编译时只用 Spigot 通用 API，理论兼容 **Bukkit / Spigot / Paper / Purpur 1.20+**。
- 实测只在 **Paper 1.21.8** 完整验证过。上生产服前，建议在你的目标核心+版本自测一遍菜单点击、商店购买、奖励领取。
- **不支持 Folia**（多线程分区核心）。如果你用 Folia，本插件可能有线程安全问题，不建议使用。

### 8.13 十六进制颜色的兼容性

- messages.yml 和菜单文本支持 `&#RRGGBB` 或 `<#RRGGBB>` 十六进制颜色。
- hex 颜色需要 **MC 1.16+** 且服务端支持（Spigot/Paper 1.16+ 都支持）。
- 低版本或不支持的核心上，hex 颜色会被忽略（不报错），建议这种情况用传统 `&a` `&c` 颜色码。

### 8.14 giveitem 快捷物品的原理

- `/jwb giveitem <玩家> <菜单>` 给的物品通过 **PDC（持久化数据容器）** 绑定菜单 ID，**右键**该物品即可打开菜单。
- 靠 PDC 识别，不靠物品名，所以玩家改名、堆叠都不影响。
- 这个物品可以正常丢弃、交易、放箱子，捡起来右键仍然能用。

---

## 九、如何排查配置错误

执行 `/jwb validate`，插件会逐条列出问题，并给修复建议。例如：

```
[错误] broken.yml > broken : 行数 rows=9 超出范围 (建议: rows 必须在 1-6 之间，已回退为 3)
[错误] broken.yml > broken > bad_material : slot=100 超出菜单范围(0-26) (建议: 调整 slot 或增大菜单 rows)
[错误] broken.yml > broken > bad_shop : buy-price 为负数：-50.0 (建议: 价格必须 >=0)
[警告] broken.yml > broken > bad_target : 打开的目标菜单不存在：xxx (建议: 检查该菜单是否存在或 open 目标)
```

validate 会检查：菜单 ID 重复、行数 1-6、slot 范围、Material 是否存在、动作/条件是否合法、open 目标菜单是否存在、商品价格非负、依赖状态等。

> 关键点：**单个菜单写错不会导致整个插件崩溃。** 出错的菜单会被跳过（或问题按钮被忽略），其余菜单照常工作。

---

## 十、菜单配置字段速查

### 菜单级字段

```yaml
id: main                    # 菜单唯一 ID
title: "&b主菜单"           # 标题，支持 & 颜色码、<green> 标签、PAPI 变量
rows: 3                     # 行数 1-6
permission: ""              # 打开权限，留空=无限制
open-sound: "BLOCK_NOTE_BLOCK_PLING"   # 打开音效（写错会跳过不报错）
close-sound: "UI_BUTTON_CLICK"         # 关闭音效
allow-drag: false           # 是否允许拖拽物品（默认 false）
allow-take: false           # 是否允许拿走菜单物品（默认 false）
fill-item:                  # 背景填充物品，铺满空槽
  material: GRAY_STAINED_GLASS_PANE
  name: " "
buttons:                    # 按钮列表
  ...
```

### 按钮级字段

```yaml
  button_id:
    slot: 10                # 单槽位（0 开始）
    slots: [10, 11, 12]     # 或多槽位
    material: EMERALD       # 物品类型
    name: "&a商店"          # 名称
    lore:                   # 多行描述
      - "&7点击打开"
    amount: 1               # 数量
    glow: true              # 发光
    custom-model-data: 0    # 自定义模型数据
    permission: ""          # 显示+点击都需要的权限
    view-condition: []      # 显示条件
    click-condition: []     # 点击条件
    actions: []             # 通用点击动作
    left-actions: []        # 左键动作
    right-actions: []       # 右键动作
    shift-left-actions: []  # Shift 左键
    shift-right-actions: [] # Shift 右键
    shop: {}                # 商品段落（见第二节）
    reward: {}              # 奖励段落（见第四节）
```

---

有问题欢迎联系作者：即白 · jibai0517@gamil.com
