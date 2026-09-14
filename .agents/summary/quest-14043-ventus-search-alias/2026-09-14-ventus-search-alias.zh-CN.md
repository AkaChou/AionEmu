# 任务 14043「学习龙族语」班图斯同名寻路别名修复（2026-09-14）

## 触发与症状

- 用户报告：任务 14043 的“和班图斯对话”阶段点击寻路后，落到了NPC 241418。
- 现场日志：`log/console.log:2160-2164` 显示 21:42:27 任务 14043 进入 `REWARD var0=8`（状态=4 步数=8），
  35 秒后客户端请求 `@public_DF6` 频道（切图到诺斯珀德）；`log/adminaudit.log:49` 显示 21:44:37 GM 的目标为
  `Terenac`（服务端模板 241418）。GM 点击寻找会直接传送，说明客户端提交的模板 ID 落在诺斯珀德，
  而不是埃雷修兰塔的情报官 278532。

## 权威证据链

1. 服务端任务合同 `src/main/resources/aion/data/static_data/quest_definition/quests/14043.xml`：
   - `:87/:98` 起始阶段（`started`，var0=0）与 NPC 278532 对话；
   - `:355`/`:366` `reward6`/`reward8` 的 `npc-complete` 也是 278532。
   即任务只在这三个阶段寻找 278532，与 241198/241418 无关。
2. Aion 5.8 客户端数据（客户端 `data/Quest/Quest.pak` md5
   `27bd5122412bfeb6ae14ab6b0385a2c7`，`data/Npcs/npcs.pak` md5
   `2d296a9ba9a9aca5ba1d66f250cc17bd`，与其解包副本一致）：
   - 步骤文本只保存显示名：`Dialogs/10000_19999/quest_q14043.html` 第 1/8 步为
     `和[%dic:STR_DIC_N_Ventus]进行对话`，不携带任何 NPC ID。
   - 客户端 NPC 表：`npcs_unpacked/client_npcs_abyss_monster.xml:756014` = 278532 `Ventus`
     （`ment`/`quest_ai_name`=Ventus，埃雷修兰塔的深渊守卫）；`npcs_unpacked/client_npcs_monster.xml:1231419`
     = 241198 `DF6_B2_24_Named_Birdmom_70_Al`；同表 `:1243853` = 241418
     `DF6_B2_24_Replace_Named_Griffon_70_Ah`。
   - 客户端字符串：`client_strings_monster.xml` `STR_NPC_Ventus`(313536)=班图斯、
     `STR_DF6_B2_24_Named_Birdmom_70_Al`(2303509)=班图斯、
     `STR_DF6_B2_24_Replace_Named_Griffon_70_Ah`(2303713)=玛罗德。
   → 客户端按显示名解析“班图斯”时会命中 DF6 的同名精英怪 241198，与 10031 艾尔米提亚、
   14047 阿凯斯泰斯属于同一类“同名模板解析冲突”。
3. 昼夜替换关系 `src/main/resources/aion/data/static_data/spawns/Npcs/220110000_Norsvold.xml`：
   - `:2293` 241198 `temporary_spawn 8-20`；
   - `:2354` 241418 `temporary_spawn 20-8`；
   - 两者共用同一坐标 `(2561.0264, 422.39435, 280.875)`。现场测试在晚间，因此该刷新点实际刷出的是
     241418，这解释了“寻路找到了 241418”的直接观察。
4. 正确目标 278532 的唯一静态刷点：`spawns/Npcs/400010000_Reshanta.xml:643`（埃雷修兰塔）。

## 根因

任务步骤在客户端只记录显示名；5.8 客户端把“班图斯”解析成诺斯珀德 DF6 B2_24 的同名精英怪模板。
服务端 `CM_OBJECT_SEARCH` 只收到模板 ID，GM 寻找因此在任务阶段传到诺斯珀德该刷新位，
（当时刷出昼夜替换怪 241418）。

## 修复

- 修复层：`CM_OBJECT_SEARCH`，沿用 Playbook Pattern `QUEST_SCOPED_NPC_SEARCH_ALIAS`
  的 GM 限域别名；不修改任务 XML，不影响普通玩家地图标记，不做全局同名替换。
- 别名合同：仅 GM；`quest=14043` 且 `START var0=0` / `REWARD var0=6` / `REWARD var0=8`；
  请求 241198 或 241418（同一 DF6 B2_24 刷新位的昼夜两个模板）时解析为 278532；
  其他任务、其他状态/阶段以及直接请求 278532 均保持原 ID。
- 修改文件：
  - `src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_OBJECT_SEARCH.java`
  - `src/test/java/com/aionemu/gameserver/network/aion/clientpackets/CMObjectSearchTest.java`
    （新增 `resolvesTheClientDf6BantusCollisionDuringQuest14043VentusStages`、
    `keepsTheDf6BantusSlotIdsOutsideQuest14043VentusStages`）

## 验证状态：CLIENT_ACCEPTED（沿用既有 Pattern，无 Playbook 变更）

- 已完成：IDE 错误级检查（两个文件均无 error）；`git diff --check` 通过。
- 已完成：2026-09-14 22:12 `mvn -q -Dtest=CMObjectSearchTest test` 通过，
  `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS（新增 2 个用例含在内）。
- 已提交：`e46058230 fix(quest): resolve quest 14043 Bantus search alias`（仅
  `CM_OBJECT_SEARCH.java` 与 `CMObjectSearchTest.java` 两个文件，未推送）。
- Playbook 判定：与既有 Pattern `QUEST_SCOPED_NPC_SEARCH_ALIAS`（代表案例 14047 阿凯斯泰斯）
  的症状、根因、修复层和合同一致，因此不新增案例、不修改 Pattern/CASES 正文。
- 已完成：2026-09-14 用户回复“验证通过，提交”，确认 14043 的班图斯寻路阶段按修复后行为通过客户端验证；
  验收记录见 `.agents/summary/quest-acceptance/14043-2026-09-14-client-accepted.md`。
- 残余风险：本次没有抓到 `CM_OBJECT_SEARCH` 的原始包，客户端提交的模板 ID 是依据其显示名解析链推断的
  241198；因此别名同时覆盖同一刷新位的昼夜两个模板 241198/241418，以覆盖现场观察到的 241418。
  客户端复测若仍指向诺斯珀德，需补充该包的 npcId 后收紧别名。
