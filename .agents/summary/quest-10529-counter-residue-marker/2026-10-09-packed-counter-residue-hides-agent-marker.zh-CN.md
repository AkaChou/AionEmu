# 10529/20529 报告代理人头顶无任务标记——s6 计数残留污染 packed 步数

日期:2026-10-09
状态:已修复(PENDING 实机复测 + 聚焦测试未授权未跑)
受影响:10529(天族)、20529(魔族镜像);NPC 806075(代理人维达)/ 806079(魔族代理人)

## 现象

玩家(Kk)执行 10529「保护之实体2」:任务状态、任务书行高亮、与维达的对话/汇报/领奖全部正常,唯独维达头顶不显示任务标记(进行中箭头/领奖感叹号),玩家无法从世界场景识别交付对象。

QUEST-TRACE 实录(2026-10-09 11:13-11:14):

```text
SM_QUEST_ACTION 任务=10529 状态=3 步数=4230   → var0=6, var1=2, var2=1(s6 杀怪计数,设计内)
SM_QUEST_ACTION 任务=10529 状态=3 步数=12296 → var0=8, var1=0, var2=3(s8:var2 残留!)
SM_QUEST_ACTION 任务=10529 状态=3 步数=12297 → var0=9, var1=0, var2=3(s9:var2 残留!)
```

状态=3 = START;packed 解码布局 `var0 | var1<<6 | var2<<12`(与 XML progress bit-field offset 0/6/12 一致)。

## 根因

QE-044(COLLECT_PROGRESS_PREMATURE_ADVANCE_DIALOG_DROPPED)的同根因变体:Aion 5.8 客户端的 NPC 对白/头顶任务标记按**整型 packed 步数**与本地步骤表精确匹配;任务书行高亮则按 var0 位域投影。两套判定双轨,所以「对话能走通、行高亮正确、标记缺失」完全自洽。

本任务的缺陷形态:

- s6 是计数阶段(var1=潜入兵×7、var2=军官×3),packed 带高位是设计内行为;
- 但离开计数阶段的步进边只清 var1 不清 var2:
  - s6→s7(两条狩猎尾杀边):只 `set var0=7`,var1/var2 全残留;
  - s7→s8:清 var1,漏 var2;
  - s8→s9:清 var1,漏 var2;
  - s9→s10(SETPRO10):不清零 → packed=12298 而非 10;
  - s10→reward(SET_SUCCEED):不清零 → packed=12299 而非 11;
- 同文件的副本失败回退边(s4~s9→s3)反而 var1/var2 都清了——成功链与失败链清零不对称,属明显遗漏。

2026-09-24 的验收记录(`quest-acceptance/10529-2026-09-24-client-accepted.md` step 2「报告行已高亮,但在 806075 处没有任务」)与本次报障是同一现象的两个观察面:当时补了 USE_OBJECT(-1) 直连 SELECT11 的对话入口(修的是服务端路由面),头顶标记的整型匹配面并未修。

## 修复

两个 XML 同修(镜像同形):

1. **步进边全清零**:s6→s7 两条边、s7→s8、s8→s9 补 `var2=0`(及 s6→s7 补 var1);s9→s10、SET_SUCCEED 补 `var1=0, var2=0`。此后 s7..reward 各阶段 packed 纯净为 7/8/9/10/11。
2. **无 source enter-world 自愈边** ×5(target s7/s8/s9/s10/reward):限定 `status-is + var0-is`,只清 var1/var2 不动阶段,修复修复前落盘的旧档(如玩家 Kk 的 var0=9,var2=3 档)。其中 s7/s8/s9 三边加 `world-is 301690000`(副本内限定)——与副本失败回退边(同 worldId、expected=false)**条件精确互斥**,通过编译器 AMBIGUOUS_TRANSITION 静态检查:世界外 s4..s9 场景由回退边独占接管(其自身清零计数,既有语义不变),副本内重登由自愈边接管;s10/reward 与回退边 source 集不相交,无需 world 限定。sync 模式跟随既有 reward 旧档自愈边先例(LEVEL_AND_VISIBILITY_REFRESH)。

设计过程中被编译器拦下一次的教训:最初 s7/s8/s9 自愈边不带 world 限定,编译器 `AMBIGUOUS_TRANSITION` 拒绝——世界外 var0=7 档上失败回退边与自愈边**真实重叠**,报得对;放行途径有条件互斥/节点集不相交/双方显式 priority 三种,选了最贴合语义的条件互斥(world-is 交叉),避免给 6 条失败边和相邻 s4→s5 边连锁加 priority。

回归锁扩展:`Quest10529BossKillCounterContractTest`
- 原 var1 位旧档循环保留;
- 新增 s6→s7 双边、s9→s10、SET_SUCCEED 的清零断言;
- 新增 5 条自愈边结构断言(存在性、条件限定、只清计数不改 var0);
- 新增实机报障档复现:副本内(worldId=301690000)var0=9+var2=3 重登自愈为纯 9;boss 击杀带 var2=3 残留产出纯 9。

## 第二段:SET_SUCCEED 后任务书步骤整块空白(同日追加,实机 GM 跳步测试发现)

用户按方案 A 跳步(delete → set START 10 + 清 var1/var2)与维达走完报告链,trace:`SET_SUCCEED → SM_QUEST_ACTION 状态=4 步数=11`——**REWARD/11 纯净**,头顶标记链路健康;但「任务书对话步骤没了」。

裁决性证据链:

1. 客户端 `quest_q10529.html` quest_summary 共 12 行,**行 11「和代理人维达对话」与行 0 逐字重复**(quest_summary 原文取证,20529 镜像行 0/行 11 同构重复)——属 QE-054「末行 = 第 0 行复述行」例外;
2. legacy(旧 handler `_10529Protection_Artifact_2`)REWARD 落盘 = **10**:SETPRO10 分支 `changeQuestStep(env, 9, 10, false)`,`to` 在 reward=true 时代从不写盘(QE-051);
3. `.agents/summary/quest-10527-reward-row/…md` 的 10528 勘误(2026-09-22)已把同型任务纠正为「reward 回 legacy 落盘值 + 恢复边反转」,并明确列出「10525/10526/**10529**/20529 等 36 个『reward = 末行 = 最后 START + 1』歧义带任务**仍未实机复核,禁止按任一方向批量改**」——批次 4(2026-09-21)已把 10529 推向「末行 11」方向,今天的实机正是那份复核的裁决:**REWARD/11 → SECTION_0 越出客户端 0..10 进度域 → 任务书步骤整块空白**;
4. QE-051 边界(2026-10-07 增补):「REWARD/(权威值) -> (末行索引)」的自愈边是**反向带毒**,必须反转为回滚方向——批次 4 给 10529 加的 `REWARD/10 → 11` 边正是此形态,今晨我给它续的 REWARD/11 清计数边也在同一位置,一并反转。

修复(10529/20529 同构,镜像 10528 勘误形态):

1. `reward` 节点投影 var0 11 → **10**(legacy 落盘值;var0 max 保持 11,旧档要 pack 得动);
2. SET_SUCCEED 交接**不再回写 var0**(由 reward 目标投影补 10),保留 var1/var2 清零;
3. 顶部无 source enter-world 恢复边反转为 **REWARD/var0=11 → 10**(合并清计数动作);删除批次 4 的 `REWARD/10 → 11` 带毒边与今晨重复的清计数边;
4. s10(START/10 报告行)保留——9-24 实机截图证实其高亮;
5. 在线 REWARD/11 档的领奖路由会被 reward 投影(10)挡住,重登触发反转边即恢复(与 10527/10528 同构);
6. 测试:`JournalReportRowSplitContractTest` 按新口径重写(reward 投影 10、SET_SUCCEED 不写 var0、REWARD/11 回滚边、交接清 var1/var2),`Quest10529BossKillCounterContractTest` 的 reward 自愈边断言改为回滚断言。

**行为说明(复测预期)**:修复后 SET_SUCCEED → `REWARD/10`,任务书高亮行 10「带上陷入沉睡的德扎波波,向代理人维达报告」(实义末行);行 11「和代理人维达对话」是复述行,**任何状态下都不亮,这是真端行为**(10528 同型勘误已验收)。头顶标记看 SECTION_0=10(域内)正常显示。

## 验证状态

- [x] XML well-formed(xmllint --noout 通过,两侧)
- [x] 编译器/planner 对无 source 多边支持有活证(既有 reward 旧档自愈边)
- [x] 聚焦测试 `Quest10529BossKillCounterContractTest` 2/2 PASS(2026-10-09 IDEA MCP,用户授权;首跑吃旧字节码失败,重跑恢复)
- [x] 聚焦测试 `JournalReportRowSplitContractTest` 9/9 PASS(2026-10-09 IDEA MCP,新口径重写后)
- [ ] 实机复测:①REWARD/11 档重登 → 反转边回滚 REWARD/10,任务书行 10 高亮 + 维达头顶领奖标记;②全流程 GM 重测方案 B;③新角色纯净档全链

按全局规则 1,未执行 `mvn` 命令行;测试经用户授权后由 IDEA MCP 运行配置执行。
