# 中文 GM 命令（//移动 / //掉落 / //任务 / //gm）实施记录（2026-09-21）

## 1. 需求

- `//移动 npc <npcid>`：移动到 NPC。
- `//移动 <地图名>`：例如 `//移动 泰奥勃莫斯`；地图与局部区域的中文名取自国服客户端翻译，输入翻译即可直接移动。
- `//掉落`：等价于 `//dropinfo`。
- `//任务`：等价于 `//quest`。
- `//gm`：等价于 `//invul` + `//speed 300`。

## 2. 中文地点名的数据来源（客户端 → 服务端）

1. 国服客户端：`/Users/mc/IdeaProjects/5.8客户端/L10N/CHS/Data/data.pak`（ZIP）→ `Strings/client_strings_*.xml`（UTF-16）。
2. 服务端静态数据：`src/main/resources/aion/data/static_data/teleport_location.xml`，每个 `teleloc_template` 带
   `loc_id / mapid / name / name_id / posX / posY / posZ / heading`。
3. 用 `name_id` 把两侧连接：中文名来自客户端字符串表（地图级入口在 `client_strings_level.xml`，形如
   `STR_LF2a_Airport_ZONE = 泰奥勃莫斯`；局部区域为 `STR_*_Airport_SUB_*` 等）。
4. 生成结果：`src/main/resources/aion/config/administration/teleport_names_zh.txt`
   （UTF-8，`loc_id<TAB>中文名<TAB>英文名`，353 条，其中带服务端坐标 269 条）。
   生成脚本：`.agents/summary/chinese-gm-commands/generate_teleport_names_zh.py`。
5. 本轮证据同时留档：`teleloc_zh.tsv`（客户端字符串 ↔ loc_id 的连接结果，含 loc_id/mapid/中英名/坐标）、
   `coverage_report.py`（按 `world_maps.xml` 逐世界统计中文名与坐标覆盖度）。

覆盖情况：

- 14 张老地图有"地图级"中文入口并带坐标：极乐世界 / 伏魔殿 / 普埃塔 / 贝尔特伦 / 埃尔特内 / 因特尔蒂卡 /
  泰奥勃莫斯 / 伊斯夏尔肯 / 阿尔特盖德 / 莫尔海姆 / 贝鲁斯兰 / 布鲁斯特豪宁 / 英吉斯温（外港）/ 格尔克马洛斯（飞艇场）。
- 其余开放地图（希哥尼亚、厄夏勒、阿斯泰拉/伊鲁玛、诺斯珀德、卡多尔、莱文绍尔、埃雷修兰塔、潘斯特拉四区、
  卡塔拉姆、奥里艾/佩尔农等）以客户端给的局部区域名收录（例如 `希哥尼亚` loc 400、`诺斯珀德` loc 438）。
- 4 条无法解析中文名：loc 591/592（Lakrum 600200000）、599/600（Dumah 600300000）——这两个世界在客户端
  正式数据里标注 `Need Client Patch!`，本表跳过。
- 84 条是客户端飞行传送点（`teleport_location.xml` 没有 posX/Y/Z），中文名仍然收录，`//移动` 会明确提示
  "服务端没有坐标，暂不支持直接传送"，`//移动 list` 会标注"（无坐标）"，不会静默失败或传到 0,0,0。

## 3. 实现

### 3.1 多别名机制（为后续继续加中文命令铺路）

- `ChatCommand`：`super("主别名", "中文别名"...)` 支持多别名；访问等级按别名逐条绑定（`setAccessLevel(alias, level)`）；
  新增 `resolveAlias(text)`（最长匹配）与 `argumentsOf(text)`（按实际命中的别名切参数）。
- `ChatProcessor#registerCommand`：遍历全部别名逐个注册；**单个别名缺配置只跳过该别名**，其他别名照常注册
  （升级时旧运行配置没有中文别名也不会把英文命令一起弄丢）。
- `ChatProcessor#loadLevels`：`administration/commands.properties` 改为 **UTF-8** 读取（Java `Properties` 默认
  ISO-8859-1，中文别名键会被解码成乱码而永远匹配不上）。
- `AdminCommand` / `PlayerCommand`：鉴权、参数切分、GM 审计日志都改用实际命中的别名。

### 3.2 命令

| 命令 | 实现 | 说明 |
|---|---|---|
| `//移动 npc <ID|名称>` | `MoveToZh` | 与 `//movetonpc` 等价（`TeleportService2.teleportToNpc`，只搜索当前地图刷点，先自查刷点再回显） |
| `//移动 <中文地名>` | `MoveToZh` | 查 `teleport_names_zh.txt` → `loc_id` → `teleloc_template` → `TeleportService2.teleportTo`；同名跨多地图时列出候选地图 ID；同名天/魔分线按阵营取 |
| `//移动 <地图 ID>` | `MoveToZh` | 优先中文表里该地图最小 loc_id 且带坐标的入口；没有则回退 `TeleportService2.getReviveWorldStartPoints`（如 600040000 提亚玛兰塔之眼） |
| `//移动 list [关键字]` | `MoveToZh` | 列已收录中文地点名，无坐标的标注"（无坐标）" |
| `//掉落` | `DropInfo`（别名） | 与 `//dropinfo` 同实例、同实现 |
| `//任务` | `Quest`（别名） | 与 `//quest` 同实例、同实现 |
| `//gm` | `Gm` | 开启：无敌 + 速度 300%；再执行一次：解除无敌 + 速度恢复基础值 |

- 访问等级配置 `administration/commands.properties`：`任务 = 3`、`掉落 = 0`（与 dropinfo 一致）、`移动 = 3`、`gm = 3`。
- 帮助页 `data/static_data/HTML/commands.xhtml` 已补中文命令块。

### 3.3 副本地图处理

`teleport_location.xml` 里 3 个世界在 `world_maps.xml` 标记为 `instance="true"`（302200000 神圣要塞防御战、
302300000 伏魔殿防御战、302350000 永恒风暴峡谷）。只传 `instanceId=1` 时 `World.setPosition` 找不到实例会直接
返回，玩家会停在"已 despawn 但没有有效坐标"的状态。`MoveToZh#teleportTo` 因此与 GM 面板
`network/aion/gmhandler/CmdTeleportTo#goTo` 保持同一口径：目标为副本地图且玩家不在该图时，先
`InstanceService.getNextAvailableInstance(worldId)` + `registerPlayerWithInstance(...)`，再用真实 instanceId 传送。

### 3.4 `//gm` 的语义选择

需求写的是"等价于 //invul + //speed 300"。实现为 **GM 模式开关**：未无敌时开启（无敌 + 300% 速度），
已无敌时关闭（解除无敌 + 速度归 0% 即基础速度），避免"再敲一次 gm 反而掉无敌、速度还留着 300%"的半开状态。
速度覆盖使用 `Speed.SPEED_OWNER` 这一固定 owner（`//speed` 与 `//gm` 共用），重复执行只覆盖不叠加。
若希望改成"只开不关"或"关闭时不还原速度"，只需调整 `Gm#execute` 的两行。

## 4. 验证状态

- static：
  - IDE 检查：`MoveToZh` / `Gm` / `Speed` / `ChatCommand` / `AdminCommand` / `PlayerCommand` / `ChatProcessor` /
    `DropInfo` / `Quest` / `CommandAliasRegistryTest` / `ChatCommandAliasTest` / `ChineseTeleportNamesTest`
    报错为 0。
  - 别名门禁（Python 复算 `CommandAliasRegistryTest` 口径）：声明别名 192 个、配置别名 192 个，双向零悬空、
    零重名、每类恰好一次 `super(...)` 声明，中文别名 3 个（任务/掉落/移动）。
  - 数据自检：353 行全部能解析、loc_id 全部存在于 `teleport_location.xml`、锚点（泰奥勃莫斯→210060000、
    普埃塔→210010000、伏魔殿→120010000）一致。
- focused-test（2026-09-21 17:57，用户授权后执行）：
  `mvn -B -Dtest=CommandAliasRegistryTest,ChatCommandAliasTest,ChineseTeleportNamesTest test`
  → **BUILD SUCCESS**：9 例全绿（ChatCommandAliasTest 3、ChineseTeleportNamesTest 2、CommandAliasRegistryTest 4），
  0 failure / 0 error；同一次构建里 4720 个主源文件 + 1052 个测试源文件全量编译通过。
- runtime/client：**未执行**（需要同步运行配置、重建资源并重启服务器）。

## 5. 部署注意（对应 IR-003 同族事故）

运行期读取的是 *部署目录* 的 `config/administration/`，不是 `src/main/resources`：

1. `config/administration/commands.properties` 必须包含 `移动 / gm / 掉落 / 任务` 四个键，否则对应别名在注册时被
   跳过（日志是 `log.8812baf3b55e` 告警），英文命令不受影响。
2. `config/administration/teleport_names_zh.txt` 必须一起部署，否则 `//移动 <中文名>` 会提示缺少中文地点表。
3. 该文件为 UTF-8，不要用 Latin-1 工具改写。

## 6. 回归点

- 英文命令：`//dropinfo`、`//quest`、`//movetonpc`、`//speed` 行为不变（`//speed` 只是把效果 owner 换成固定实例）。
- `//掉落` 的目标/参数与 `//dropinfo` 完全一致；`//任务 start|set|show|delete|log` 与 `//quest` 一致。
- `//gm` 开关两次后：无敌与速度都应回到原点状态。
