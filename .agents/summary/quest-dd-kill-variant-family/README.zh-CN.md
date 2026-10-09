# DD 击杀路由变体族修复(15546/15500/15503/15640/17510)

日期:2026-10-08 · 分支:quest · 状态:**实现完成、聚焦测试全绿,待实机验收**

## 报障与定性

用户实机报障「任务 15546、15500、15503、15640 等,击杀目标进度不会变,任务状态不变」,追加「17510 击杀也不计数」;并澄清 17510「怪物是刷新了的,但是击杀没有计数」。

定性:**QE-048(KILL_TARGET_COVERS_CLIENT_VARIANT_FAMILY)在 DD 原生车道的回归**。typed XML 退役、切真端表驱动后,装载面按精确解析重新引入同型缺陷:

- DD 表 hunt 名单写「代表名」(base/最低级模板),如 15546 写 `LF6_Daru_A_66_n`、17510 只写 `IDTransform_Sado_*_66_An`;
- 世界实刷的是变体族:Iluma 只刷 `LF6_T_Daru_A_66_n/67_n`(base 0 刷新,241664/241665),变身副本(302100000)按玩家等级刷 `IDTransform_Sado_*_66..75_An` 全族;
- `NativeNpcNameResolver.resolveMonsterIds` 按 name_desc 精确解析只命中代表名 ⇒ `killsByNpcId` 不含实刷模板 ⇒ 击杀零路由、进度恒 0;
- 客户端合同 `quest_monster.csv`(QE-048/QE-125 裁定的权威)恰好就是全族名单(15546 SECTION_2 = `lf6_daru_a` 族 4 名;17510 = sado/boss 全 80 名)。

## 修复内容

| 文件 | 变更 |
| --- | --- |
| `src/main/java/com/aionemu/gameserver/questEngine/tablelane/NativeNpcNameResolver.java` | 新增怪物变体族索引(`idsByMonsterFamily` / `monsterFamilyKeyByNpcId`)、族键形态学 `monsterFamilyKey`(剥「等级 1-2 位 + 紧邻 tag 1-2 字母」尾段、归一 `_t_` 中段;无等级尾段自族不并)与 `resolveMonsterFamilyIds`(desc/name/别名命中扩族;对话名组不扩) |
| `src/main/java/com/aionemu/gameserver/questEngine/tablelane/DataDrivenNativeRuntime.java` | 仅 HUNT 分支改用族解析(`resolveMonsterFamilies`);对话/接取/物件/刷怪动作面保持精确语义 |
| `src/test/java/com/aionemu/gameserver/questEngine/tablelane/DataDrivenHuntVariantFamilyGateTest.java` | 新门禁:族键形态学、精确通道不变、族覆盖实刷变体(含负例)、客户端合同对拍(17510 族拆解与合同 80 模板恰好相等)、生产击杀兴趣面、实机故障行为复演(杀 241664 推进 15546 计数) |
| `.agents/memory-bank/patterns/quest-engine.md` | QE-048 补 DD 车道回归证据(scope/root_cause/fix#6/validation/boundaries/代表案例) |

无任务 ID 特例、无硬编码映射(quest-repair 规则 6):族展开是数据驱动的通用形态学规则,模板实存才并入。

## 验证

- **聚焦测试(IDEA MCP,2026-10-08)**:`DataDrivenHuntVariantFamilyGateTest` 6/6、`NativeNpcNameResolverTest` 8/8(既有解析语义不变)、`DataDrivenNativeRuntimeGateTest` 全类全绿(运行时零回归)。首跑门禁自己抓出 `contractIds` 只取第 7 列的缺陷——合同名单实为「第 7 列起每列一名到行尾」,已修正。
- **全库审计**(`audit_dd_hunt_variant_coverage.py`,只读;结果落 `report.tsv`):1190 个 DD hunt 任务,修复前断供 252,族展开净恢复 81;报障任务实刷交集 15546: 0→8、15500: 0→10、15503: 0→13、15640: 0→12、17510: 0(副本动态刷怪,静态审计不可达,路由面已由门禁锁定)。
- **副本移植(IDEA MCP,2026-10-08)**:`FissureOfOblivionInstanceTest` 5 项中 3 项通过;`hiddenRoomControllersCannotInterceptBossAttacks` 与 `entranceKeepsOnlyTheQuestAndPortalOwnedExit` 失败为**存量**(归因:前者 needle 以 `);` 结尾而源码为 `spawn(..., (byte) 0), 2000);` lambda 调度形式,indexOf 恒 -1;后者断言的 quest_definition/spawns XML 未被本次触碰——两文件及 AI 类在 git 中零改动,HEAD 同样失败);`FissureOfOblivionInstance.java` IDE 检查 0 error,onDie 分支结构核验 4/8/12 原样。
- **未执行**:全量回归套件、实机验收。

## 边界与残余

- 同 body 的精英 tag 变体(如 `66_ds`)会并入族,杀之也计数——与客户端「同名主体」口径一致,宽松非硬坏。
- 族展开后仍无静态实刷交集的任务约 171 个:动态/副本刷怪族或数据缺口,非路由层可修;名单名完全不可解析的 125 个为既有冻结面(NAME_UNRESOLVED),由运行时门禁管理。

## 17510 副本战斗链移植(2026-10-08 第二段,按真端数据)

实机复测反馈:杀 244859 后「忘却之影」计数未完成。定性两层:
1. **244859 = `IDTransform_Boss_Base_75_Ae`,不在任务合同内**——17510 SECTION_2 的客户端合同只认 `Boss_TypeA..D_{66..75}_Ae`(quest_monster.csv),Base 本体按真端语义就不计数(真端 on_killed_by_user 只设 boss=3/广播,不进任务);
2. **副本只刷 Base、不刷 Sado,且缺「Base 召唤 TypeA..D」战斗链**——Base 由第四房 AI `IDTransformTransRoom04AI2` 按玩家等级刷出(玩家 75 级 → 244859,即实机所杀),Sado 全程零刷新;真端 Base 进战后按 HP 召 TypeA..D,本服没有这条链 ⇒ Type 永不出现 ⇒ SECTION_2 永远不满。

真端取证(`58Server/Map`,UTF-16):
- `Map/Worlds/IDTransform/world.xml`:Sado 为**条件刷怪** territory(`(level_check == 66) && (a_room == 7)` 形),每级 60 点位(3 线 × 4 职业房,a/b/c 线每房 4/5/6 只);**level_check 只切换模板等级(66 级 base 每级 +41),点位三线共用**;`boss == 2` 时在 (296.103027, 513.080505, 354.342468) 刷 `Boss_Base_<lv>`。
- `Map/XML/NpcAIPatterns_IDTransform_JSM.xml`:`Boss_Base` on_enter_attack_state 召 WorldRaid_On_NPC + 5s battle timer;on_battle_timer 对 TypeA/B/C/D **各挂独立判定**(HP < 80% + 30% 概率)在自身位置召唤;TypeA..D 才是任务计数怪。
- 提取产物:`retail-idtransform-spawns.tsv`(611 行条件刷怪定义)。

本服移植(`FissureOfOblivionInstance`,2026-10-08 实机反馈修正后定稿):
- **Sado 刷怪零改动**——第一/二/三房 AI(`IDTransformTransRoom01/02/03AI2`)已按房间分批刷 4 只(75 级 = 244823–244826),`killCounters` 4/8/12 删空气墙控制器开门;曾试加「16 只一波」破坏了分批结构(实机:杀 4 只空气墙不开),**已回退**;
- Boss_Base 刷怪维持既有第四房 AI(`IDTransformTransRoom04AI2` 按等级 spawn,有源码门禁锁定);
- 唯一新增 = **Boss_Base 召唤 TypeA..D 扫描循环**(5s):实例内存在 Base 且进战、HP<80% 时,对未召唤的 TypeA..D 各按 30% 判定召唤(位置 = Base 自身);Base 死亡/副本销毁即停;
- **已知偏差(标注)**:真端 Type 带 `despawn_at_attack_state`(Base 脱战即消失),本服 Type 存活至死亡/副本清理。

## 待办(按 quest-repair 规则)

1. 用户重启实机服务端(IDEA 常驻进程,改动需重启生效),复测 15546/15500/15503/15640 的击杀计数与 17510 的全等级击杀计数。
2. 实机验收通过后:按规则 13 重审 Playbook 资格(QE-048 已覆盖本模式,预计不新增 case)、补验收记录、授权后提交。
