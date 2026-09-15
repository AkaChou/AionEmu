# 寻路对象搜索（CM_OBJECT_SEARCH）同名冲突与真端对齐治理 / Quest NPC Search Collision & Retail Alignment

## 背景与问题溯源

1. **用户提报**：天族 45+ 使命任务 14043「Drawling Balaur / 学习龙族语」，玩家在与“班图斯”对话阶段点击寻路，目标被错误定位至魔族诺斯珀德（DF6）70 级巨鸟精英怪 `241418`（与 `241198` 昼夜共点），而非埃雷修兰塔（欧比斯）情报官 `278532`。
2. **深层追问**：
   - 为什么明明有 `npcId`，客户端还会寻路到错误的 NPC？
   - 为什么经常出现点击寻路完全无反应（地图不标记/找不到）？
   - 官方真端（Retail）是否存在同样的问题？真端是如何处理的？

---

## 根因深度剖析

### 1. 协议天然无状态与上下文丢失（Lossy Protocol）
- 5.8 客户端上行数据包 `CM_OBJECT_SEARCH (0x00D6)` 仅包含 4 字节的整数 `npcId`（`readD()`）。
- 客户端在发包时，未携带任何任务 ID（`questId`）、步骤（`step/var0`）或触发词条（`STR_DIC_xxx`）。

### 2. 客户端词典多模板同名反查机制（Client Display Name Collision）
- 客户端任务文本（如 `quest_q14043.html`）中仅记录富文本宏 `[%dic:STR_DIC_N_Ventus]`，词典标题为“班图斯”，未绑定任何数值模板 ID。
- 玩家点击“定位”时，客户端二进制（`Game.dll`）中的 `FIND_POS_BY_NAME` 拿着纯中文字符串“班图斯”去本地已加载的 NPC 模板表做红黑树检索。
- Aion 5.0 在诺斯珀德（DF6）新增了巨鸟怪 `241198`（内部名 `Birdmom`，中文本地化重名为“班图斯”）。客户端字典优先命中了新怪 `241198`，因此发出的包内数据本身就已经错了。
- 官方 5.8 真端客户端由于使用的是完全相同的二进制和数据表，同样会产生此查找错误。

### 3. 服务端历史缺陷
- **GM 限域错误**：`CM_OBJECT_SEARCH.java` 中此前对 14043（班图斯）、10031（艾尔米提亚）、14047（阿凯斯泰斯）的别名改写被错误包裹在 `if (gm)` 分支中，普通玩家点击寻路时不走别名改写，依然按错误 ID 寻路。
- **静态点兜底剥夺**：`allowStaticFallback` 被硬编码传为 `gm`。非 GM 普通玩家在活体怪物死亡等待刷新、巡逻离开出生点或区域未预加载时，直接返回 `null`，服务端静默丢包不回发 `SM_SHOW_NPC_ON_MAP`，导致点击寻路完全无反应。

---

## 真端对齐治理方案（本次改动）

1. **别名消解全员生效**：
   - 提取 `resolveSearchNpcId(Player player, int requestedNpcId)`，并在 `runImpl()` 入口处统一调用。
   - 所有普通玩家只要正处于对应任务阶段，均能将 241198/241418 动态改写为 278532。
2. **阵营与世界合法性过滤（Faction & World Legality Check）**：
   - 普通玩家寻路时，根据玩家阵营（`Race.ELYOS` / `Race.ASMODIANS`）自动过滤敌对大陆主权地图（天族过滤 `WorldType.ASMODAE` 诺斯珀德等；魔族过滤 `WorldType.ELYSEA` 阿斯泰拉等）。
   - 欧比斯与中立区域保持双向互通，GM 保持全图穿透。
3. **放开普通玩家静态刷怪点兜底（保证找得到）**：
   - `allowStaticFallback = gm || (player != null && !locations.isEmpty())`。
   - 存活实体优先（保障巡逻怪实时坐标）；实体暂缺或处于刷新冷却时回退至首个静态出生点，100% 保障地图标记下发。
   - 活体实体若缺少静态点配置，自动构建其实时坐标的 `SpawnSearchResult`。

---

## 验证与测试

- **单元测试**：`src/test/java/com/aionemu/gameserver/network/aion/clientpackets/CMObjectSearchTest.java` 扩充：
  - `resolvesSearchNpcIdForNormalPlayerWithActiveQuest`：验证普通非 GM 玩家在 14043 阶段将 241198/241418 正确消解为 278532。
  - `filterLocationsByRaceFiltersEnemyFactionHomeContinents`：验证天族过滤敌对大陆、魔族过滤敌对大陆，并保留共享欧比斯地图。
- **全量测试结果**：`CMObjectSearchTest` 全量 14 个测试用例全部通过（`exitCode = 0`）。
