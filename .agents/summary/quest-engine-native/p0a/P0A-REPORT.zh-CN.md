# P0a 审计报告：真端任务引擎迁移（只读审计，零行为变更）

> 文档状态：**P0a 完成**。本批未改任何产品代码/数据，未跑构建，未启停服务器，未触碰 QE-112 在飞文件。
> 执行日：2026-10-01。授权依据：用户「完成计划」指令 + 计划 §4（P0a 为零行为变更审计，产出放本目录）。
> 证据根：`<真端根>`=58Server（数据 `Map/XML/`、反编译 `server58-source/`、原始 DLL `MainServer/ScriptDLL64.dll`）；客户端解包根=`Quest_unpacked/`。
> 基线纪律：QE-112 在飞未提交（HEAD `71d0b031d`，15 文件脏）；涉及本仓台账的数字为当日快照，**冻结须等 QE-112 落地后重跑工具**（全部幂等只读）。

---

## 1. 三大语义矩阵（§4.1/§4.2/§4.3）——计划三大未决项全部有结论

### 1.1 DD「宽计数」前提被推翻（§4.1，详见 `semantic-matrix-dd-wide-count.md`）

- **计划 §3-12 的「8 行 >63」是误读**：90/120/300 全部是 `value5`（刷怪描述符 `Relative/Absolute <npc>, <数量>, <存活秒>, <坐标>`）与 `value10`（计时器）的数字，不是 Hunt 计数。全 DD 表 Hunt 行仅 **80817（`world_event_camel 100`）** 真实超 63。
- **DD vars 真实布局 = 6 位步号（bits0-5）+ 4×6 位组计数槽（bits6-29）**，单组上限 63；步进 `(var&0x3f)+1` 自动清组槽；完成判据 = 当步全组满；最多 4 组（第 5 组起被截断——真端自身缺陷，≥5 组可提前误推进）。
- **80817 在真端就不可能完成**（target=100 > 63 ⇒ 第 64 杀槽回绕并污染下一组）。native 处置：登记 `KNOWN_BROKEN_IN_RETAIL`，禁止静默修复。
- DD 处理器**无 status==3 守卫、无 0x40000000 守卫**（守卫=步号匹配+共享距离）；与家族相机不同布局、同一写入通道（+0xF0/+0x100 槽位号与 §4.2 的 IUserImp 一致，跨证据推定）。
- 本服「自造宽计数组合成器」建立在误读之上；native 不需要任何宽计数组合器。**原 P7 前置「>63 未还原」解除**，替代前置=其余 7 个 progress 类别（Talk/CollectItem/PvP/EnterArea/EnterWorld/TalkFOBJ）的 vars 布局与动作表考古 + 击杀事件分发器补全（见 §6 缺证据清单）。

### 1.2 相机双通道副作用闭环（§4.2，详见 `semantic-matrix-camera-channels.md`）

- `+0xf0` = `IUserImp::SetQuestProgress`（status∈{3,4} 才写、只写 vars）；`+0x100` = `IUserImp::SetQuestSuccess`（仅 status==3，写 vars + status 3→4，失败无副作用）。实现体在 NPCSvr64（快照内完整）。
- 两通道**同一传输与持久化路径**：NPCSvr→Main 包 0x44 → MainServer `User_OnUpdateQuestFromNpcServer`（权威态+区域校验）→ 客户端包 0x184 + **即时 fire-and-forget DB 包 0x56**；无事务/无回滚（失败仅 `internal state mismatch` 日志）。
- status 枚举：0 无 / 3 进行中 / 4 可交付 / 5 已领奖 / 6 待接取；`vars<0x40000000` = 30 位打包守卫（bit30/31 为哨兵区）。
- 一次事件每槽只 +1；同事件可链式推进多任务（handler 链遍历）。
- **native 结论**：`0xf0/0x100` 可合并为一个 state port 的两个操作（progress/success），守卫与状态迁移不同、落库与同步同路——P1 的「不合并通道」禁令可收敛为「一个 port、两个显式操作」。

### 1.3 `can_report` 闭环（§4.3，详见 `semantic-matrix-can-report.md`）

- quest.xml 子元素（非属性），**217 行全部=1**（18 SimpleHunt + 197 DD + 2 仅 quest.xml），高等级/日常/紧急命令向。
- 加载：MainServer `fun_249.cpp:4228` 存偏移 0x7CEC（NPCServer 同偏移只存不读；ScriptDLL 不加载 quest.xml）。
- **唯一消费点** `MainServer fun_239.cpp:13111`（C_HACTION 对话动作处理器）：任务 status==4（可交付）且 can_report!=0 时，对话动作 0x6c..0x7c 走**快速通道**（直接发奖+关对话）；=0 走 NPC 脚本化对话分发。**不是**「未满进度提前交付」开关。
- native 规格：`NativeReportRewardFlow` 对 can_report 行在可交付态提供直发路径；负例=状态≠4 时不得直发（QE-092 负例测试保留对接）。

## 2. 相机参数矩阵（§4.4.2，详见 `camera-params.tsv` + `camera-params-summary.md`）

- **2463 个调用点全解析**（2429 六位 + 34 十位；仅 2 个函数定义行非调用）：1812 任务，**全部属 SimpleHunt**；SimpleSerialHunt 零相机（P2 前须考古其推进机制）。
- 校验全绿：零混宽、零 fullValue 矛盾、零 required 超 mask、DD∩相机=0；**2 例真端自身表/脚本分歧**（13912/23912：表 count3 无 monster3 ⇒ 脚本无 slot3 相机，fullValue 含槽3，推进不可达）——登记 `NATIVE_CAMERA_TABLE_SCRIPT_DIVERGENCE`，native 以脚本调用集为准。
- 对计划数字的修订：旧口径 2221/13 无法复现，以本矩阵为准；**10 位 required 实际 14 种取值**（{1,65,67,77,80,100,231,235,242,251,260,329,500,1000}），计划「{1,100,500,1000}」过窄；flag 2463/2463 全=1。

## 3. 表来源与对拍（§4.4.3，详见 `table-source-inventory.tsv` / `table-semantic-diff.tsv`）

- 17 张表全量重算行数+sha256；入仓 8 张真端表 **token 语义全部等价**（byte 全 DIFF=编码/空白；SimpleHunt 17 行=dev_name/逗号空白规范化）。登记幂等转换规则：UTF-16→UTF-8、DOCTYPE 剥离、列表空白规范化。
- 入仓副本实质问题 2 处：`legacy/quest_random_rewards.xml` **陈旧**（329/817，交集 329 行全漂移）；`legacy/challenge_tasks.xml` id 全对齐仅行元素改名（task）。
- 行数修订：DD 2492（旧 2526）、SimpleHunt 1863（旧 1865）、item_quest 5674（旧 5682）。

## 4. owner 重冻与三源身份矩阵（§4.4.4，详见 `owner-identity.tsv` + `owner-identity-report.md`）

> **⚠️ 同日修正**：初版"名字缺失 4623 行"是审计工具索引缺陷（漏 `name_desc` 属性）。修正后：READY **7542/8562（88%）**、缺失 **518** 行、歧义 **165** 行、DD 客户端缺失 337（不变）。归因与证据链见 `owner-identity-correction.md`，P1 决策输入见 `../p1-prereqs/name-resolution-decision.md`。

- 真端表 **8562 行** vs 台账 retail 5482：**3080 个表行从未被本服采纳**——「表存在≠native-ready」被数据证实；台账无幽灵行（LEDGER_ONLY=0）、无表/XML 双主冲突（CONFLICT=0）。
- verdict（修正后）：READY（含哨兵）**7542** / NAME_MISSING **518** / AMBIGUOUS **165** / CLIENT_MISSING 337（全 DD）/ NO_TABLE_XML_ONLY 670。
- **名字解析轴结论（修正后）**：真端任务表引用的是 npcTemplates 的 `name_desc` 全名；`name ∪ name_desc` 精确解析即覆盖 88%，零发明规则。残差 234 个唯一缺失名中 146 个（623 引用）在本服静态数据构建范围外（LF4/DF4/DF6/ldf5 高地区域）；规范化桥接覆盖率 ≤3% 已否决。
- 337 个 DD id 不在客户端 quest.xml（`_challengetask_`/活动/内部行）→ 默认冻结 `IDENTITY_EVIDENCE_REQUIRED`，阻塞 DD 家族切换判定，不阻塞表解析。

## 5. raw vars 存档 / 缺失表决策 / 启动量级 / 硬编码

- **raw vars**（`raw-vars-archive.md`，本机开发库 956 行样本）：canonical 916 / packed_unexpected 40 / 高位 0。40 行 unexpected 正是自造宽计数组合器的产物（如 10034 vars∈{3,8}）；native 写入前可达性校验 → `NATIVE_RAW_VARS_INVALID` fail-closed，禁止静默规范化。生产库审计待用户提供访问。
- **缺失表决策**（`missing-table-decisions.md`）：P0b 必入仓=HtmlPages(5904)/challenge_task(123,源 hash)/quest_random_rewards(重入仓)；P7 前=npcfactions(436)；不参与=item_quest（无服务端加载点）与 SimpleGather（空）；暂缓=jumping_*（跃升角色 feature 级决策）。
- **启动量级**（`startup-scale-estimate.md`）：注册表合计 ≈15–25 MB、构建 2–6s，一次构建不可变，无启动风险。
- **硬编码**（`hardcode-audit.tsv` + `hardcode-audit-report.md`）：57 处；**召回 6/6 PASS**（具备门禁资格）；retail 包 48 处随包删除；跨包 6 处逐行裁定（1 GLOBAL_OK、1 NOT_QUEST_SCOPE、1 P0_WHITELIST、3 XML 车道冻结范围登记）；`RetailNpcNameIndex` 映射知识与 COMBINE_SKILLS 词汇标记为「删除≠丢弃」。

## 6. 缺证据清单（如实登记，不阻塞 P1 SimpleHunt）

| # | 缺口 | 补全路径 |
|---|---|---|
| 1 | DD 击杀事件分发器（谁调用注册的 handler 指针）+ param_4/param_5 来源 | 对 `DAT_184720a10/0a50` 交叉引用重反编译或 Server64 侧调用路径 |
| 2 | DD 侧 +0xE8/+0xF0/+0x100 的服务器端实现与封包 opcode（§4.2 已给同槽位 IUserImp 强对照） | MainServer_Server64 quest 对象类考古 |
| 3 | DD 其余 7 类 progress handler 的 vars 布局/动作表（Hunt/Talk 已还原） | 同 §4.1 方法（fun_718.cpp 5896-6300 一带） |
| 4 | SimpleSerialHunt 推进机制（零相机） | P2 前专项 |
| 5 | IUserImp::vftable 数据本体、ctx+0x18 getter、DB 侧 0x56 包接收端 SQL | 数据段/CacheD-DBSvr 反编译 |
| 6 | 337 个 DD id 的客户端正文证据 | 更完整客户端解包（任务书/页） |
| 7 | 生产库存档 raw vars 全量审计 | 用户提供生产库访问后重跑 `tools/raw_vars_probe.py` |

## 7. 对批次计划的直接影响（建议随 P0b 授权一并确认）

1. **P1（SimpleHunt）前置「名字解析器策略决策」已大幅收窄**（同日修正后）：`name ∪ name_desc` 精确解析覆盖 SimpleHunt 1691/1863；残差 172 行（150 缺失 + 22 歧义）与 SimpleTalk 213 行、DataDriven 909 行（572 名字/歧义 + 337 客户端缺失）的处置政策待用户裁决——见 `../p1-prereqs/name-resolution-decision.md`。
2. **P7（DD）前置改写**：旧前置「>63 未还原」解除；新前置 = 缺证据清单 #1/#2/#3 + 337 客户端缺失裁决；本服宽计数组合成器及其指纹按误读产物处理（退役方式随家族切换批）。
3. **P2（SimpleSerialHunt）前置**：缺证据 #4。
4. **80817**：登记 `KNOWN_BROKEN_IN_RETAIL`（真端不可完成），native 复刻或显式禁用，禁止静默修复——需用户裁决取向。
5. **raw vars 迁移**：40/956 样本 unexpected ⇒ P1 state port 必须带可达性校验负例测试。

## 8. 交付物清单（本目录）

| 文件 | 内容 |
|---|---|
| `P0A-REPORT.zh-CN.md` | 本报告 |
| `semantic-matrix-dd-wide-count.md` / `semantic-matrix-camera-channels.md` / `semantic-matrix-can-report.md` | §4.1/§4.2/§4.3 |
| `camera-params.tsv` / `camera-params-summary.md` | §4.4.2 相机逐行矩阵（2463 行带证据） |
| `table-source-inventory.tsv` / `table-semantic-diff.tsv` | §4.4.3 表来源 hash/行数/语义对拍 |
| `owner-identity.tsv` / `owner-identity-report.md` | §4.4.4 三源身份矩阵（9232 行） |
| `raw-vars-archive.md` / `raw-vars-probe-output.txt` | §4.4.5 存档分布（本机库样本） |
| `missing-table-decisions.md` | §4.4.6 缺失表决策 |
| `startup-scale-estimate.md` | §4.4.7 量级评估 |
| `qe112-inflight-status.md` | §4.4.8 QE-112 在飞记录 |
| `hardcode-audit.tsv` / `hardcode-audit-report.md` | §4.5 硬编码清单+裁定（召回 6/6） |
| `tools/*.py` | 幂等只读审计工具（可重跑） |

## 9. 下一步（须逐批授权）

1. **P0b 数据基础**（需单独授权）：HtmlPages/challenge_task/quest_random_rewards 入仓 + 来源 hash 门 + loader 骨架；不接路由、不翻 owner。
2. P0a 冻结：QE-112 落地（或裁决）后重跑 `tools/owner_identity.py` + `tools/raw_vars_probe.py` 刷新冻结数字。
3. P1 前置设计决策：名字解析器策略（§7.1）。
