---
alwaysApply: false
globs: "src/main/resources/aion/data/static_data/quest/definitions/**/*.xml, src/main/java/com/aionemu/gameserver/questEngine/**/*.java, src/main/java/com/aionemu/gameserver/ai/**/*.java, src/main/java/com/aionemu/gameserver/ai2/**/*.java, src/test/java/com/aionemu/gameserver/questEngine/**/*.java, src/test/java/com/aionemu/gameserver/ai/**/*.java, src/test/java/com/aionemu/gameserver/ai2/**/*.java, docs/quest/**/*.md"
---

# 任务诊断与修复规则 (Quest Diagnosis and Repair Rules)

## 必读资料 (Required Reading)

在处理任务问题之前，必须阅读：

- `../memory-bank/patterns/quest-engine.md`
- `../../docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md`
- `../../docs/quest/repair-playbook/PATTERNS.zh-CN.md`
- `../../docs/quest/repair-playbook/CASES.zh-CN.md`
- `../../docs/quest/WRITING_GUIDE.zh-CN.md`
- `../../.agents/summary/headless-client-extraction/MIGRATION.zh-CN.md`

当 `CASES.zh-CN.md` 将匹配到的案例链接至 `docs/quest/repair-playbook/cases/*.md` 时，在设计修复方案前必须先阅读对应的案例分片文件。

## 证据要求 (Evidence Requirements)

1. 在设计或编辑修复前，将玩家表现、编译后 IR、所有者形态（owner shape）以及副作用契约（side-effect contract）与 Playbook 的模式指纹（pattern fingerprints）进行比对。通读最具代表性 commit 的完整 diff 与测试代码，记录匹配和相异的契约字段；仅查阅单行的案例索引是不充分的。
2. 综合审查当前 XML 编译后的状态、事件、条件、事务性动作（transactional actions）以及 `after-commit` 的执行顺序。
3. 当存在 `origin/history` 引用时，将状态、对话页面与副作用顺序与遗留处理器（legacy handler）或真端零售模板进行比对。
4. 客户端证据统一称为“Aion 5.8 客户端”。切勿假定其存在于某台特定机器或固定路径上。
5. 若任务需要 Aion 5.8 客户端的页面、动作、字典、封包、解包资源、抓包数据或其他在当前对话或仓库中无法获取的外部证据，列出缺失项并请求用户提供。在获得证据前，切勿凭空猜测或标记任务已修复。
6. 根据具体情况，使用日志、对象 ID、NPC 模板 ID、地图或副本上下文以及登录/登出行为来验证实际的运行时路径。

## 实现边界 (Implementation Boundaries)

1. 生产环境的任务执行归属于任务 XML 与生产目录。遗留处理器、客户端数据与日志属于权威的行为证据，而非生产代码的所有者。
2. **以全部类似任务角度根本解决 (Systemic Over Isolated Repair)**：切勿孤立修复单个上报的任务。始终从同类所有任务的角度展开排查。明确缺陷是源于通用的引擎/编译器/规划器机制（例如事实需求 fact requirements、状态门禁 state gates），还是源于整个任务家族通用的 XML 契约失配（例如多杀计数步进变量、计数上限、前置任务继承等）。若属于引擎/编译器问题，应在底层根因处集中修复，让所有任务永久受益；若属于契约失配，应扫描全量生产目录与客户端数据集，将所有匹配的任务一并修复，并建立回归审计套件。
3. 对共享运行时或 AI 的改动必须证明其影响范围，并补充共享回归覆盖或生产目录审计。
4. 测试必须精确锁定源状态、目标状态、状态位（status）、变量（variables）、事件（event）、条件（conditions）、事务动作以及完整的 `after-commit` 顺序。仅断言最终状态是不充分的。
5. 当状态正确但对话页展示、关闭行为、NPC 生成（spawn）、跟随（follow）、传送（teleportation）或其他副作用依然有误时，视为修复未完成。
6. **严禁在任务引擎与编译器中引入硬编码特例 (No Hardcoded Exceptions in Quest Engine or Retail Compilers)**：
   零售任务编译器（`Retail*Compiler`）、生产调度器（`QuestProductionDispatcher`）与任务引擎必须严格遵循从真端表与客户端模板的 1:1 数据驱动纯粹转换。**严禁**在任务编译器或任务引擎中引入特定任务 ID、特定物品/NPC ID 映射的硬编码特例代码（如合成的 `questId -> npcId` 路由、合成的 `USE_OBJECT` 跳转，或硬编码的道具/NPC 对）来绕过缺失的 AI 或运行时行为。
   属于 NPC AI 职责的行为（如场景物体交互发放道具、物体消失进入刷新冷却、自定义对话或脚本化动作）必须在 NPC AI 层或专用的场景物体交互处理器中实现，以保持清晰的领域边界，使任务引擎保持解耦且无硬编码特例。

## 验收与 Playbook 更新 (Acceptance and Playbook Updates)

1. 仅当已被验收的代表性修复沉淀出了新的可复用问题模式时，才更新 `../../docs/quest/repair-playbook/PATTERNS.zh-CN.md` 和 `CASES.zh-CN.md`。对于已被既有模式覆盖的其他任务，切勿更新 Playbook；保持稳定的流程规则在 `QUEST_REPAIR_PLAYBOOK.zh-CN.md` 中。
2. 验收要求满足：XML/IR 契约正确、通过专项测试、且通过生产目录与白名单检查。在针对该任务的编译器测试和生产目录/白名单门禁通过之前，切勿将 XML 改动交付给用户进行客户端重测；若未获构建授权，保持修复状态为 `PENDING` 并列出未执行的命令。
3. 涉及客户端对话页、NPC 生成、跟随、登录/登出行为或性能的改动，还必须经过相应的客户端或运行时验证。向用户索取缺失的输入，切勿将验收降级为主观推测。
4. 明确指出当前任务并声明客户端验证或客户端验收已完成的用户消息，是该任务全程可玩（playable）的权威确认，除非用户明确将说明限制在某一分支或步骤。将其视同 `CLIENT_ACCEPTED`；切勿要求用户重复跑线或提供截图。该消息同时也是完成本地提交工作流的明确常规授权，无需等待后续的 "commit" 指令：保留无关的变更文件，提交任务所属的修复（若尚未提交）或复用现有的待验收 commit，记录验收结果，重新运行 Pattern 资格判定，并完成证据/Playbook 提交。已有 Pattern 对应一个修复 commit 加一个针对性验收记录 commit；新 Pattern 对应一个修复 commit 加一个 Playbook/验收记录 commit。该授权不包含 push、服务端生命周期操作或无关改动。在移交验收完成前，已知的编译器/目录检查失败仍必须先行解决。
5. 按照 `../summary/quest-acceptance/README.zh-CN.md` 中的字段记录已完成的客户端或运行时验证。记录必须指明代码仓库 commit 及相关工作树状态、Aion 5.8 客户端/数据出处、NPC/物体与地图/副本上下文、源状态/动作/页面、预期与实际响应、启动健康度、可用日志/协议/产物哈希、验收状态以及残留风险。触发规则 4 时，引用或忠实总结用户的确认内容，并将无法获取的技术产物标记为 `not captured`；其缺失不影响用户实机游戏验收的有效性。
6. 交付物必须明确区分“实现完成 (implementation complete)”与“验收完成 (acceptance complete)”。若测试、目录或白名单检查、Aion 5.8 客户端验证或实际运行时证据仍未完备，在最终回复中必须主动将工作标记为“待验收 (pending acceptance)”，列出未完成的命令或证据及责任方，切勿仅单方面汇报问题已修复。
7. 若启动日志中包含 `Can't initialize typed quest engine`、`QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或生产目录编译失败，立即停止客户端层面的诊断，返回 XML 展开与编译器证据排查。智能体绝不可自行启动、停止或重启用户管理的服务端来执行此检查。
8. Playbook 是修复参考手册，而非验收流水账。按可复用的问题模式去重案例，而非按任务 ID 去重。每个新增或实质性修订的代表性案例必须记录：一个代表性任务、稳定的 Pattern ID、可搜索的现象关键词、IR/所有者指纹、第一检查点、玩家可见现象、根因、修复层级、改动文件、代表性测试、验证命令与结果、复用边界以及 commit。
9. 对于重复所有者（duplicate-owner）修复，仅凭证明幸存 XML/运行时路由生效的测试是不够的。代表性证据还必须指出移除被替换所有者的 commit diff 或源码审计，且在实际路径可用时运行时验证必须记录实体或副作用计数。
10. 仅当后续任务的玩家可见现象、根因、修复层级与修复契约全部与既有案例相符时，才将其视作同一问题。不得追加该任务 ID、修改案例正文或创建重复案例。仅在问题或修复模式有实质性差异时创建新案例。
11. 严禁为进行中的工作、部分修复、测试失败、仅凭静态推断或仍处于 `EVIDENCE_REQUIRED` 状态的任务添加代表性案例。
12. 当一个共享改动影响多个任务时，分别对每个任务进行独立验收，但对于可复用模式仅保留一个代表性案例。
13. 每当验收证据发生变动以及在暂存任务修复前，重新评估 Playbook 准入判定。将玩家可见现象、根因、修复层级和修复契约与既有案例进行比对。一旦专项测试、目录/白名单检查、客户端验证或运行时证据改变了验收状态，先前的 `pending acceptance` 或去重判定即告失效。
14. 当已验收的修复建立了一个新的代表性案例时，使用连续的两个本地 commit，以便 Playbook 引用稳定的修复 commit 哈希：首先仅提交修复源码/测试，然后使用该修复 commit 哈希更新并提交 Playbook。切勿使用 amend 将 Playbook 合并进记录其哈希的 commit 中。
15. 若修复在待验收状态下已提前提交，保留其稳定的 commit 哈希，并在最终验收证据齐备后将 Playbook 跟进作为首个 commit。切勿重写待验收期间积累的无关历史。
16. 无论哪种顺序，案例一旦验收，在 Playbook commit 完成之前，严禁插入无关 commit、执行 push 或发出验收完成移交。即使验收是在后续才获得的，修复与 Playbook commit 也构成一个完整的交付批次。
17. 修改 Playbook 模式或代表性案例后，运行 `python3 .agents/summary/quest/check_quest_repair_playbook.py`，要求每个索引或详述的代表性 commit 均能解析，并至少包含一个结构化的 Pattern 指纹以及既有的 `TestClass#method` 引用。
18. 由于 `docs/*` 默认被忽略，在提交文档时必须使用显式路径添加每个修改的 Playbook 文档：

   ```bash
   git add -f docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md
   git add -f docs/quest/repair-playbook/PATTERNS.zh-CN.md
   git add -f docs/quest/repair-playbook/CASES.zh-CN.md
   ```

19. 在通过新的可复用结论或引擎级见解完成或验证任务修复后，按照自动沉淀协议（Automatic Wrap-up Protocol）更新 `.agents/memory-bank/patterns/quest-engine.md`；一次性个案沉淀至 `.agents/summary/<topic>/`，无需创建新 Pattern。
