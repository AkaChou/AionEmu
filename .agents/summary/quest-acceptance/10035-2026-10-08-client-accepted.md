# 任务 10035 客户端验收记录

quest: 10035「실렌테라 회랑 진격 준비 / 前往安格利浦关卡」（ELYOS Lv54+，英吉斯温链；DD 车道）第 5 步
进区（EnterArea `LF4_SensoryArea_Q10035A`，安格利浦关卡区域）

user acceptance confirmation: 用户 2026-10-08 原话「实机验证完成」（针对第 5 步进区修复的复测确认；未限定
分支/步骤，按规则 4 视为整条修复路径 `CLIENT_ACCEPTED`）。此前实机报障原话：「任务10035『前往安格利浦关卡，
查看情况』，具体要到哪里？我到了关卡，任务没有更新」——报障时轨迹见 `log/quests.log` 12:52:56（步数=4 后
无进区推进）。

server launch mode: IDEA（常驻 Spring Boot，classpath=target/classes）。修复资源就位证据：
`target/classes/aion/data/static_data/zones/zones_retail_enterarea.xml` 13:20 副本含
`mapid="210050000" name="LF4_SensoryArea_Q10035A"`（与 src 同尺寸 56327B）；服务端 13:27:59 由用户重启
（ZoneData 重载），13:28:36 用户登录复测。服务端生命周期由用户管理。

repository commit: `81887d32e`（修复提交：生成器 R5 活图归一 + 区注册表重生成 + 台账 normalized_from +
门禁 ⑥ + 主题 README + memory-bank QE-130）；本记录与 Playbook 收口提交紧随其后。
主题 README = `.agents/summary/quest-10035-enterarea-live-world/README.zh-CN.md`。

working tree: dirty；并行会话在飞主题（walker/geo 等）保留未暂存，未纳入本提交。

Aion 5.8 client/data provenance: 真端世界文件实读（UTF-16）——`Map/Worlds/LF4_M/world.xml` 感官区 15 处
（含 `LF4_SensoryArea_Q10035A`）、`lf4/world.xml` 0 处、`df4` 0 / `DF4_M` 13；台账
`qe-enterarea-retail-zone-resolution.tsv` 该行 = `R1_SAME_NAME / world_dir=LF4_M / mapid=210050000 /
normalized_from=210130000`；客户端包 SHA-256 本轮未新采集。

npc template/object: 关卡锚点 206363（`LF4_SensoryArea_Q10035A`，"Angrief Gate"）；运行时 object ID
not captured。

map/instance: Inggison 活图 world **210050000**（无实例）；目标区多边形 x∈[1276,1391] / y∈[1584,1744] /
z∈[322,422]（中心约 (1333,1664)）；镜像世界 210130000 按迁移口径不再承载该区。

steps（复测口径 = 修复后走到目标区；用户按整体确认，逐步未逐字回报）:
1. 前置：角色已有 10035 且步数=4（报障窗口 12:52:56 同状态）。
2. 走到安格利浦关卡区域（约 (1330,1660)）——非回廊入口一带（702663「Corridor Entry Controller」/
   730256「silentera westgate」，y≈2299，为第 7 步 FOBJ 与西口传送）。
3. 进区 ⇒ 期望推进到步数=5（击杀 Drakan ×10 指引）。
4. 未覆盖：重登/重复进入；组队；步 5-8 后续流程。

source state/status/vars: 步数 4 → 5（DD 纯进度写，START/var0）；运行时逐字记录 not captured。

action/page/button: 无按钮——`QuestEngine.onEnterZone` 原生分流按同名区 `LF4_SensoryArea_Q10035A` 命中
⇒ DD `dispatch` 步 4→5（无对话页参与）。

expected response: `SM_QUEST_ACTION 10035 状态=3 步数=5` + 步 5 指引（会话说 `STR_QUEST_SAY_LF4_21` 面）。
actual response: 用户整体确认「实机验证完成」（2026-10-08）。重启后 quest 包轨迹 not captured——`log/quests.log`
停在 12:54、`log/console.log` 在 13:28 重启后无 `[QUEST-TRACE]` 新行（日志打点未产出），如实标注；
不影响规则 4 下用户游玩验收的权威性。

startup health: 13:27:59 重启；`log/error.log` 最后一条错误为 11:16:04（早于本次重启），无
`Can't initialize typed quest engine` / `QuestCompilationException` / `AMBIGUOUS_TRANSITION`；
`log/warn.log` 13:25 后无新 WARN（13:25 为重启期连接断开）。

runtime logs: 时间窗 13:27:59–13:31+；证据 `log/adminaudit.log`（13:28:02 聊天处理器加载 = 重启完成、
13:28:36 登录、13:31:40 movetonpc 730256）；`log/console.log`（13:28:36 GAMECONNECTION_LOG 玩家进入世界）。
附件 SHA-256 not captured。

protocol trace: not captured（重启后 trace 打点未产出）。
screenshots/recordings and SHA-256: not captured。

acceptance status: **ACCEPTED_NEW_PATTERN**
matched Pattern: `ENTER_AREA_ZONE_MUST_HOST_ON_LIVE_WORLD`（本批建立）；代表测试
`DataDrivenEnterAreaPortGateTest#mirrorAuthoredEnterAreasRegisterOnTheLiveWorld`；修复提交 `81887d32e`。
remaining risks: LF4_M 其余 14 个感官区（Q10024A/Q11040/Q11076A-C/Q11147A/Q11149A-C/Q14062/Q36500/
Q36506/Q36512/IDTemple_Q30003）未实机（当前无 DD 引用，R5 规则自动覆盖）；镜像世界不再承载该区（本就
不可达）；组队/重登路径未覆盖；步 5-8 由既有 DD 面承担、不在本次修复范围。
