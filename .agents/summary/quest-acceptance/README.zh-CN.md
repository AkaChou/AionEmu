# 任务客户端与运行时验收记录

每次人工客户端或实际服务端验收单独创建一份 Markdown 记录。建议文件名为
`<quest-id>-<yyyy-mm-dd>-<short-result>.md`。记录仓库相对路径、Git commit、附件 SHA-256 和可复现步骤；
不要写个人机器绝对路径、账号、角色名、数据库凭据、IP、令牌或其他敏感信息。

客户端资源来源应写为“Aion 5.8 客户端”，并引用仓库内已有的来源清单或哈希记录。截图、录屏、日志和抓包
可以放在仓库外；存在附件时必须给出稳定文件名、SHA-256 和获取方式，无法长期访问的临时缓存路径不算证据。
用户明确回复某任务客户端验证或验收完成时，该确认是整个任务游玩验收的权威证据，除非用户主动限定了分支或步骤。
记录原意和日期；没有附件或协议抓包时填写 `not captured`，不要求用户重复游玩来补材料。

## 记录模板

```text
quest:
user acceptance confirmation: wording or faithful summary; date; full quest or explicitly limited scope
server launch mode: IDEA / packaged JAR / other
repository commit:
working tree: clean / dirty; relevant diff or paths
Aion 5.8 client/data provenance: package names; repository evidence reference; SHA-256 when newly collected
npc template/object: template ID; runtime object ID; interaction object provenance
map/instance: world ID; instance ID; entry/reentry context

steps:
1. prerequisite and starting location
2. NPC/object interaction sequence
3. logout/login, reconnect, restart, death, retry, or repeat steps when relevant

source state/status/vars:
action/page/button:
expected response: target state/status/vars; transaction actions; complete after-commit order
actual response: visible result; target state/status/vars; side effects

startup health: typed quest engine initialized; catalog result; relevant WARN/ERROR absent or quoted
runtime logs: timestamp window; quest/NPC/object/world identifiers; repository-relative attachment and SHA-256
protocol trace: packet order and objectId/questId/page/action fields; attachment and SHA-256
screenshots/recordings and SHA-256: stable file name; what each artifact proves

acceptance status: PENDING / ACCEPTED_EXISTING_PATTERN / ACCEPTED_NEW_PATTERN / REJECTED
matched Pattern: Pattern ID; matched fields; differing fields; representative commit and TestClass#method
remaining risks: missing branch, class/race, reconnect, repeat, performance, or external evidence
```

`startup health` 发现 `Can't initialize typed quest engine`、`QuestCompilationException`、
`AMBIGUOUS_TRANSITION` 或 production catalog compile failure 时，本次客户端点击不构成有效任务验收；先返回 XML
展开和编译器门禁。人工观察只证明所记录角色、状态和路径，不自动覆盖其他职业、种族、奖励分支或重登路径。
