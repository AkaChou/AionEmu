# 事件 NPC 应援按钮（SETPRO1=10000）收尾零发包 客户端验收记录（ACCEPTED_NEW_PATTERN）

```text
quest: 80787-80790（事件族「休眠玩家应援」，Talk 类零步任务；本轮验收面 = 派生的 NPC 服务动作「获得帕塔的应援」，非任务接取）
user acceptance confirmation: 用户 2026-10-06 依次回复「buff 正确获得了，但是没有像 npc 831031 一样有施法的动作」→（第③轮修复后）「验证成功」
server launch mode: IDEA（常驻 Spring Boot 进程，classpath=target/classes）；改动须用户重启服务端后生效（本轮由用户重启复测）
repository commit: 随本文件同批入库（fix(quest): 事件应援按钮收尾零发包 + 施法动作对齐参照 NPC）
working tree: dirty；并行任务（quest-owner-derived、jfr-startup-jrebel-conflict 等）改动/目录保留未暂存；本任务文档单独提交
Aion 5.8 client/data provenance: 客户端 npc 表 client_npcs_npc.xml（833672 无 race_type/idle_animation/talk_animation，831031 有）、skills.pak/client_skills.xml（11049 motion_name=buff / 20950 motion_name=alterfire+cast_fx）、custompreset.pak（preset_world_guide_2.xml = pc_lf 天族女性 PC 模型）；真端 NPC 表镜像 Map/XML/npcs.xml（833672 另有 no_check_animation=1）；本轮未采集客户端包 SHA-256
npc template/object: 833672 event_Mongsil_Return（运行时 object 141234）；833671 event_Mongsil_Newbie（object 141233）；参照 NPC 831031 event_Nebrith（npc_support）
map/instance: not captured（事件 NPC 所在地图/实例 ID 未采集）

steps:
1. 未接取玩家点 833672 对话页 select_quest 的「获得帕塔的应援。」（HACTION_SETPRO1=10000，questId=0 上一页=10）。
2. 第②轮修复后复测：应援 buff（833672=11049 / 833671=11047）正确获得。
3. 第③轮（撤销关窗包）后复测：buff + NPC 施法动作均正常。

source state/status/vars: 与任务状态无关（动作归 NPC 自身层；退役 XML 无 SETPRO1 转移、真端 SetQuestProgress 对未接取零写）
action/page/button: NPC 事件对话 html（event_mongsil_return.html）select_quest 页按钮「获得帕塔的应援。」= HACTION_SETPRO1(10000)
expected response: 零发页、零关窗；NPC 广播施法包（SM_CASTSPELL + 结果包，技能 11047/11049）并施加应援 buff；对话窗保持打开（与 831031 / Npc_SupportAI2 同形）
actual response: 与预期一致（用户「验证成功」）；修复前对照试验 = 增益正确但多发一条 SM_DIALOG_WINDOW(objectId,0) 关窗，客户端看不到 NPC 施法动作

startup health: 服务端启动日志 not captured（生命周期由用户管理）；本轮授权门禁：DataDrivenNativeRuntimeGateTest 29/29 + AyasSupportAI2Test 3/3（32/32，IDEA MCP）
runtime logs: log/quests.log 2026-10-06 09:39:15-09:40:19（833671/833672 动作=10000 轨迹；仓库相对路径，SHA-256 not captured）
protocol trace: 上述 SM_DIALOG_WINDOW 行为片段来自仓库日志文本；施法包未落日志（客户端表现为实机观察）
screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: 记忆库 QE-148（ACQUIRE_FACE_ACTION_PAGE_CONTRACT_GATE：接取面不认领未声明动作 + NPC 对话按钮归 NPC 自身层/AI + 收尾语义不许跨面搬运）；关联 QE-142（cabb10 0x5d8=关窗，属任务侧 SETPRO 分派面 questId≠0，本轮即按「跨面搬运」纠正）；representative test = DataDrivenNativeRuntimeGateTest#acquireFaceEchoesOnlyClientDeclaredActionPages、AyasSupportAI2Test#cheerButtonOutranksTheQuestEngineAndSendsNoDialogPacket
remaining risks: ① 客户端数据面（833671-833674 缺 race_type/idle_animation/talk_animation）本轮证明**不阻塞**施法动作，但其它「PC 外观 + 无 race_type」NPC 若出现「buff 有、动作无」仍应先比收尾包而非先改客户端；② 同型事件 AI（DivineBonfire / Code_Red_Nurser / Dapplie / Motlie / LegendaryToyBear / Mighty_Hero / Npc_Support 等）的 buff 可达性依赖同一「接取面不认领」前提，未逐一实机；③ 魔族侧 833673/833674 共用同族分支（skill 11047/11049 映射已锁），未单独实机；④ 复测前须确认服务端已重启（旧字节码会致假阴性）
```

## 证据引用

- 修复证据链、客户端数据对照与工具：`.agents/summary/quest-80788-event-npc-setpro-loadfail/README.zh-CN.md`（§7 施法动作调查；`tools/dump_vftable.py` 为真端脚本 DLL 虚表读取脚本）。
- 门禁：`DataDrivenNativeRuntimeGateTest`（29/29）、`AyasSupportAI2Test`（3/3，IDEA MCP，2026-10-06）。
- 记忆库卡：QE-148（`patterns/quest-engine.md`），派生素引随 `sync_memory_bank.py` 更新。
