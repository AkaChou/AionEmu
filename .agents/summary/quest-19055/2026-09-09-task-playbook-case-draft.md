# 任务书案例草案：不可达接取前置导致接受页 load fail

> 状态：`PENDING_PLAYBOOK_ACCEPTANCE`
>
> 原因：自动化 compiler/catalog/白名单验证已完成，但本会话没有服务端运行日志或修复后的 Aion 5.8 客户端人工复测。根据任务修复规则，正式代表案例需待客户端验证完成后再写入 `CASES.zh-CN.md` 与 `PATTERNS.zh-CN.md`。

- Pattern ID：`UNREACHABLE_START_PREREQUISITE_ACCEPT_LOAD_FAIL`。
- 代表任务：19055「[Expert] Requirements For Construction Expert / [达人]成为家具制作达人的条件」。
- 搜索症状：NPC 可见任务但点击接受后 `load fail`、`Quest_Q19055.html (HtmlPageId 1002)`、接受按钮无响应。
- 玩家可见症状：在 NPC 798450 对话中打开接受窗口后点击“接受”，客户端尝试加载 `Quest_Q19055.html` 的 1002 动作/回退页并弹出 load fail，任务不能进入 START。
- 根因：生产 XML 在 metadata 中追加了 `finished quest-id="19054"`；19054 没有当前可执行定义，且用户确认其为 999 级不可达任务。接取动作虽然存在，但开始资格因该前置失败，typed quest owner 不处理该请求，最终暴露通用回退页面。
- 修复层：只删除 19055 的无效 `start-conditions`；保留 `min-level="29"`、ELYOS、任务物品、`start-eligible`、1002 接受动作、1003 响应页和 `VISIBILITY_REFRESH` 顺序。不修改共享 dispatcher，不删除其他任务前置。
- 修改文件：`../../../src/main/resources/aion/data/static_data/quest_definition/quests/19055.xml`（已由 `adc5cbc0b` 提交）；`../../../src/test/java/com/aionemu/gameserver/questEngine/definition/Quest19055ClientDialogAlignmentTest.java`（已由 `adc5cbc0b` 提交）；任务目录行同步修正见 `../../../docs/QUEST_CATALOG.zh-CN.md`。
- 第一检查点：先从 metadata/编译 IR 识别 `prerequisites`、`start-conditions` 和 `start-condition-groups` 的前置目标，再确认目标是否有当前 EXECUTABLE 定义、旧版任务定义和客户端/旧 handler 证据；不要仅因目标缺失就批量删除。
- 验证命令和结果：19055 XSD 通过；19055 专项合同测试通过；页面顺序审计通过；生产 catalog 6200 条编译成功、失败 0、白名单违规 0；全量 Maven 有独立基线失败，见同目录 `2026-09-09-unreachable-start-prerequisite-audit.md`。
- 复用边界：仅适用于“任务显示但接受条件引用不可达/缺失任务，导致接受路由不命中”的形状。若目标任务实际存在但等级、阵营、重复规则或客户端链要求前置，必须保留并验证；若 load fail 来自 action/page 错位、介绍链桥接或奖励页面，不复用本模式。
- commit：修复提交 `adc5cbc0b`；正式 Playbook commit：待客户端人工验收后再生成。
