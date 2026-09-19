# 本地化日志参数契约：异常必须回到日志调用（2026-09-19）

## 1. 结论

批次把 583 处日志调用里被 `I18n.get(...)` 静默吞掉的异常参数搬回日志调用的最后一个参数位，
使 SLF4J 重新打印堆栈；同时修正 18 个无法消费参数的模板、给空模板补上文本，并用
`LocalizedLogArgumentsTest` 把「参数必须有占位符消费、异常不得进 `I18n.get`」固化为闸门。

## 2. 现象与根因

- 现象：`console.log` 里出现**只有一行空 ERROR**（`ChatCommand` 等）、以及
  `对所有 NPC 运行访问器时异常` 这类**无堆栈**的错误行，故障无法定位。
- 根因：I18n 迁移把遗留 SLF4J 写法 `log.error("msg", e)` 改写成 `log.error(I18n.get("log.xxx", e))`。
  迁移时 `e` 被塞进 `I18n.get` 的参数位，而模板里没有对应占位符：
  1. `MessageFormat` 丢弃多余参数 → 异常文本与堆栈全部丢失；
  2. 日志调用只剩一个 `String` 重载 → SLF4J 不再打印堆栈。
- 遗留语义证据（`3e40f5116^`）：
  - `commons/database/DB.java`：`log.warn("Error executing select query: {}", query, var17);`
    → SLF4J 规则「占位符少于参数且末参为 Throwable」= 打印消息 + 堆栈；迁移后堆栈消失。
  - `dataholders/WalkerData.java`：`log.error("Error while saving data: " + e1.getMessage(), e1.getCause());`
  - `loginserver/utils/DeadLockDetector.java`：`log.warn("DeadLockDetector: " + e, e);`
  - 既有正确范式（迁移前就写对）仍保留在树里，例如 `ShutdownHook`：
    `log.error(I18n.get("shutdown.disconnect_login_error"), e);`

## 3. 规模（脚本复算）

`python3 .agents/summary/i18n-log-args/audit_log_args.py` / `classify.py`（以修复前 HEAD 内容为准）：

| 分类 | 调用点 | 不同键 | 含义 |
|---|---|---|---|
| A-stack-trace-lost | 548 | 517 | 异常进了 `I18n.get`，模板不消费，日志无堆栈 |
| E-throwable-only-inline | 19 | 15 | 模板用 `{n}` 渲染异常 `toString()`，仍无堆栈 |
| B-throwable-also-logged | 23 | 18 | 异常同时传给 `I18n.get` 与日志，消息里冗余 |
| multi-throwable | 5 | 2 | 同一个异常重复传入 |
| C-plain-arg-dropped | 7 | 4 | 普通参数没有占位符 |
| not-a-log-call | 27 | 27 | `I18n.get` 的非日志用法（`String.format`/返回值），本次不动 |
| ok | 781 | 724 | 参数与占位符匹配 |

合计 `I18n.get` 带参调用点 1391 处，其中 583 处需要改写，覆盖 170 个 Java 文件、526 个不同键。

## 4. 修复规则（已固化为闸门）

1. **异常不进 `I18n.get`**：`log.error(I18n.get("k", a, e))` → `log.error(I18n.get("k", a), e);`
   （异常必须是日志调用的最后一个参数，SLF4J 才会打印堆栈）。
2. **模板必须消费参数**：占位符最大序号 + 1 == 传参个数，两套语言包都要满足。
3. **消息不得为空**：`log.da39a3ee5e6b` 原来是空串（遗留 `log.error("", e)`），现为
   `Unexpected error` / `发生未预期的错误`。
4. `E`/`B` 类站点在搬走异常后，同步删除模板里对应的占位符（例如
   `Failed to persist abyss rank for player {0}: {1}` → `... player {0}`），避免出现字面量 `{1}`。

## 5. 改动清单

- 代码：`codemod_throwable.py` 改写 583 处（170 个文件）；`HousingBidService`、`KnownList` 另见下节。
- 语言包：`messages.properties` / `messages_zh_CN.properties` 同步修改 18 个键（17 个模板修正 + 1 个空模板补文本），
  另外新增 `log.113eb26bcfad`（键名沿用 `log.<sha1(英文模板)[:12]>` 规则）。
- 闸门：`src/test/java/com/aionemu/boot/i18n/LocalizedLogArgumentsTest.java`
  （占位符计数、异常参数检测、双语键集合/非空/占位符集合一致性、扫描量下限 1000）。
- 工具（`AI 中间产物`，不入提交）：`audit_log_args.py`、`classify.py`、`codemod_throwable.py`、
  `apply_bundle_edits.py`、`verify_invariant.py`、`list_touched.py`、`touched-files.txt`。

## 6. P2：KnownList 异常定位能力

- 修复前：`KnownList.doOnAllNpcs` 与 `doOnAllNpcsWithOwner` 共用 `log.70e363d7c042`，
  模板不含占位符且异常被吞 → 只能看到「对所有 NPC 运行访问器时异常」一行，既不知道哪条路径、也不知道哪个对象。
- 运行证据：`log/console.log:66251`（09-19 09:32:58，`pool-4-thread-36`）、`:66360`（09:43:27，`pool-4-thread-9`），
  对应 `log/error.log:11262-11263`；两条都发生在 `ThreadPoolManager.scheduleAtFixedRate` 的
  scheduledPool（`pool-4-*`）上，而该池上唯一周期进入 `KnownList` NPC 遍历的调用是
  `MovementNotifyTask`（500ms 周期，`doOnAllNpcsWithOwner`，见 `taskmanager/tasks/MovementNotifyTask.java:113`）。
- 修复：
  1. 异常参数回到日志位（P1），堆栈恢复；
  2. 两条路径拆成两条消息：`log.70e363d7c042`（`doOnAllNpcs`）与新增
     `log.113eb26bcfad`（`doOnAllNpcsWithOwner`），文案为
     「对所有 NPC 运行访问器时异常：所有者={0}，NPC={1}，已遍历={2}」；
  3. 新增 `describe(VisibleObject)`（类型 + 对象 ID + 名称 + 地图，名称不可用时降级为 ID，
     保证兜底日志自身不抛异常），循环内记录当前 NPC 与计数。
- 边界：本次**未重启服务端**，因此没有拿到真实堆栈；下一次复现时日志会直接给出
  所有者、失败 NPC 与遍历进度，堆栈由 P1 恢复。可能的失败点（静态阅读）：
  `MoveNotifier.visit` 的 `object.getAi2()`/`lifeStats`、以及
  `CreatureEventHandler.onCreatureMoved → checkAggro/onAtDistance` 内部。

## 7. P3：HousingBidService 住宅记录缺失时的空指针

- `HousingBidService.executeTask()` 里 5 处 `getHouseByAddress(...)` 结果在未判空的情况下被解引用
  （`house.getOwnerId()`、`wonHouse` 过户、`bidHouse.isInGracePeriod()`、`house.getObjectId()` 等）。
  住宅数据未加载成功时必然 NPE（2026-09-18 运行时栈：`HousingBidService.java:324`）。
- 修复：统一加判空 + 复用既有键 `log.15485b0c4e93`（`House not found for address: {0}`）告警并跳过该条竞拍，
  不再让一条坏数据中断整场结算；`getBidByEntryIndex` 为空时同样跳过。
- 边界：仅做空值防护，不改变拍卖业务规则；`sellerPcd`（玩家数据）判空不在本次范围。
- 遗留（未改，供后续决定）：该方法体内有 5 处错位的重复 Javadoc（调用点复制了方法声明文档，
  行 194/337/386/441/589 附近），以及 `WorldMapInstance.doOnAllNpcs` 复用了「…players」文案的键。

## 8. 验收

```bash
# 1) 参数契约全量静态复算：1708 个调用点，0 违规（验证时刻；并行任务继续新增调用点后为 1713）
python3 .agents/summary/i18n-log-args/verify_invariant.py

# 2) 聚焦测试（10 例 + 34 例，全绿）
mvn -B -Dstyle.color=never -Dtest='LocalizedLogCallsTest,LocalizedLogArgumentsTest,I18nTest' test
mvn -B -Dstyle.color=never -Dtest='LocalizedLogArgumentsTest,LocalizedLogCallsTest,I18nTest,KnownListTest,KnownListIterationSafetyTest,GameHousingLifecycleTest,GameHousingRuntimeBridgeTest,GameHousingServicesTest' test
```

- `verify_invariant.py`：验证时 `call sites checked: 1708` / `violations: 0`（并行任务继续提交后复算 1713 仍为 0）。
- 聚焦套件：10 例与 34 例全部通过（含新闸门 2 例）。
- 未做：服务端重启与客户端复现（按项目规则由用户执行）；因此 `KnownList` 根因仍是**待复现确认**，
  本次交付的是「可定位」能力与静态风险点。
