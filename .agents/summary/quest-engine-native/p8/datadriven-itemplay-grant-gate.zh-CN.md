# DD ItemPlay 道具可得门重建裁定（P8 第三批，2026-10-02）

对应：步 f 登记「`QuestItemPlayGrantGateTest` 退役（DD ItemPlay 道具可得门转 P8 重建）」。
产物：`src/test/java/com/aionemu/gameserver/questEngine/tablelane/DataDrivenItemPlayGrantGateTest.java`（1/1 绿；DD 门全组 `mvn -o test '-Dtest=DataDriven*Test'` 45/45）。

## 1. 旧门为什么退役、旧门其实有多窄

旧门（步 f 删除）口径：保留清单 RETAIL_TABLE 行的**旧编译视图**里，每条
`QuestEvent.ItemPlay` 边的 itemId 必须被同一定义的 `GiveItem` 动作或 `drops` 覆盖，外部来源
逐条登记（当时登记为空）。它退役的原因只是视图没了（DD 编译车道删除），不是判据失效。

重建取证发现旧门**口径有盲区**：旧视图把 DD 行编成对话链 IR，ItemPlay 步「占行不占对话段」
（随混合链交织推进），只有子集真正成为 `QuestEvent.ItemPlay` 边 ⇒ 旧门看到的边远少于真端
表里的 ItemPlay 步全集，绿得比字面口径窄。

## 2. 新门口径（全人口 + 三源对拍）

人口 = 生产路由集（`DataDrivenNativeRuntime.instance().routedQuestIds()`，1444 行）∩ DD 表
ItemPlay 步 = **42 步 / 37 任务**（全表 2526 行、70 步 / 58 任务，非切换集部分不路由不扫）。

每步载荷（`value0_progress_`，`符号, 计数` 形，与运行时 `TRAILING_INT` 同形剥计数）：

1. **源 1 物品模板**：符号经 `RetailItemNameIndex`（真端 item_template `name_desc` 索引）解析
   ——解析成功即模板存在；路由行解析失败本就被 `NAME_UNRESOLVED` 冻结，此处一致性复查
   （42/42 全解析）。
2. **源 2 授予（行内）/ 掉落（任务列）**：同行任意步（含接取步 0）`GIVE_ITEMS` 列 1 的
   `符号 数量` 对（与执行器同形解析）∪ 真端 quest.xml 掉落列（`RetailQuestDriver.ensureLoaded()
   .retailMetadataOf` 与生产元数据同链）。
3. **源 3 工作物品采集面**：itemId == 本任务自己的 `quest_work_itemN` 声明。**判据取证**：活
   运行时里工作物品不是接取发放（`NativeQuestStartPort`/DD 运行时零工作物品引用），而是由
   **掉落系统供给**——`QuestService`（:1157 附近）对工作物品做"已持有则不再掉落"的闸门，即
   任务数据声明凭证、掉落表供源。真端侧抽查（13830/15545/15334 三样本）确认工作物品不走
   quest.xml 掉落列、也不在 npcs_npcs.xml items_info ⇒ 其供源在掉落表域（任务数据之外），
   门只锁「凭证声明存在且归属本任务」这一数据面。

覆盖结果：42 步 = 6 步行内 GIVE ∪ 掉落覆盖 + 18 步工作物品采集面（13830-13834/23830-23834
Doc 系列、15334/25334 Relic、15545/25545、15042、13951/23951、19900/19910/19911 及镜像、
80978）+ **2 步外部登记**（18738/28738）+ 其余由源 2 直接覆盖。

## 3. 外部登记（附真端证据，登记必须仍被使用）

18738「데아마스의 비밀 병기」/ 28738（镜像）：单一 ItemPlay 步要 10×
`idraksha_solo_bomb_01a`（164000342，Improved Life Drain Bomb），quest.xml 无工作物品、无
掉落列，DD 行无 GIVE 列。真端证据：`npcs_npcs.xml` 三颗宝箱 NPC `items_info` 100% common
掉落——`IDRaksha_Solo_TreasureBox_A/B/C`（702694/702817/702818，×20/5/10），即单人副本
IDRaksha 内开箱供弹。门内 `REGISTERED_EXTERNAL` 锁这两行，并断言**登记仍被数据使用**
（数据漂移后登记作废即红，防登记腐化）。

## 4. 观察登记（不在本门范围）

接取轴同形问题：DD 表 21 行 `category_acquire_ = ItemPlay`（用道具接取），其中 5 行的起始
道具在任务数据三源下同样无源（9693 `QUEST_9693G`〔非切换集〕、15025 `QUEST_15025A`、
80827/80828 `quest_80827a`、80832 `quest_80831a`）。旧门也不覆盖接取道具。这些起始道具的
真端供源（掉落/商店/上一环）未取证，保持观察；如后续取证坐实供源形态，可并入本门登记轴。

## 5. 证据命令

```bash
mvn -o test -Dtest='DataDrivenItemPlayGrantGateTest' -DfailIfNoTests=false   # 1/1 绿
mvn -o test '-Dtest=DataDriven*Test' -DfailIfNoTests=false                    # 全组 45/45
# 真端证据：npcs_npcs.xml 内 idraksha_solo_bomb_01a 三处 items_info（702694/702817/702818）
python3 - <<'PY'   # 切换集 42 步 / 37 任务三源对拍（本裁定 §2 数字来源）
PY
```
