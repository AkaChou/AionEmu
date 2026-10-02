# PENDING_CLIENT 实机走查清单（P8 收口批，2026-10-02）

对象：迁移计划 §10.3-#6（每族代表任务客户端验收）+ §10.3-#17①（放弃段全链路）。
执行者：**用户**（真实客户端 + 服务端 `aion/` 部署）。以下每项记录：任务 id、执行结果、
异常现象（客户端表现 + 服务端日志行）。回填到本文件「验收结论」节或口头转述均可。

前置：~~`scripts/package.sh` 部署最新 JAR~~ **已由代理完成（2026-10-02 10:36，`aion/AionEmu.jar`
含 P8 全部十一批提交 + MoveTo 同世界传送修复）**，仅需 `./aion/start-silent.sh` 启动服务端
（关闭 `./aion/shutdown.sh`），客户端 5.8 登录。

## ① Say 气泡（NativeSayPort = SM_SYSTEM_MESSAGE，登记偏差面）

- 代表任务：**10033**（DD 行，步内 MESSAGE 动作 col7/8，字符串走 `retail-quest-string-ids.tsv` 7 键）。
- 步骤：接取并推进到带消息的步（按任务指引击杀/对话），观察屏幕中央/气泡是否出现系统消息。
- 预期：出现**系统消息**（非 NPC 头顶气泡——这是已登记的通道偏差：真端 Say 气泡通道在 5.8
  客户端无对应包，按步 f F4 裁定走 SM_SYSTEM_MESSAGE）。
- 异常记录：消息未出现 / 出现但乱码 / 出现时机不对。

## ② Spawn 回收（NativeSpawnPort 单次刷 + 定时回收）

- 代表任务：**10010**（DD 行，步内 SPAWN 动作 col5，Absolute/Relative 定点刷）。
- 步骤：推进到刷怪步，观察 NPC 是否出现；离开区域或超时后回到原地，确认 NPC **已回收**；
  再次触发确认可重刷。
- 预期：单次刷出 → 客户端可见 → 定时回收（不残留）。
- 异常记录：不刷 / 刷出残留 / 重复叠加。

## ③ DD 接取页词汇（native 口径：信页 4762 → 1011 → 页 4 兜底 + 1007 开窗）

- 代表任务：**1870**（DD 行，Talk 接取；前置 1868 未移植走 fail-open，可直接接取）。
- 步骤：找到接取 NPC 对话：先直接点任务（应走 1007 接取窗或页 4），再走普通对话流
  （打开 → 1002 → 1003 → 提交）。
- 预期：页流顺序 = 真端口径（信页优先），无空窗/无页 10 残留污染动作；接取后任务出现在
  任务栏。
- 异常记录：页序错乱 / 点按钮无响应 / 页 4 无法到达。

## ④ 进区接取 `*_QuestArea_*`（§10.3-#26 新接线面）

- 代表任务（活 6 全查）：**12505 / 12524 / 22524**（Hunt）与 **12504 / 22504**（Talk，LDF5a
  单人副本 600051000）；**39005**（Hunt，df2a 220050000 入侵门区 InvadePortalDest_41）。
- 步骤：进入对应区域（LDF5a 需进副本到区多边形内），任务应**自动出现在任务栏**（无 NPC
  对话）。
- 预期：进区即接取（真端 `User_AddAreaQuest` 同构，走等级/职业/前置全条件面）；离开重进
  不重复接取。
- 异常记录：不触发 / 重复触发 / 条件不符的玩家（如等级不足）被错误发放。

## ⑤ 放弃段全链路（§10.3-#17①②，CombineTask 族代表）

- 代表任务：**5000**（同形行代表，CombineTask 车道）。
- 步骤：完整走一遍 接取 → 交付 → 领奖 → **放弃**（任务栏放弃按钮）→ 再接取。
- 预期：放弃后配方/工作物品回收正常（`RecipeListRecipePort` DAO 面），再接取不残留旧状态。
- 异常记录：放弃报错（记下服务端日志栈）/ 再接取异常 / 配方残留。

## 管理员命令速查（降低走查成本）

- `//quest start <id>`：对**选中玩家**强制建档（绕过接取面；①②③⑤ 的推进/交付/领奖/放弃
  可用 `//quest delete <id>` 复位重测；`//quest show` 查看状态）。
- `//quest log on`：打开任务追踪日志（复现异常时先开，服务端日志会带任务 traces）。
- `//moveto worldId X Y Z`：坐标传送。**④ 进区接取**的六个区多边形中心（z 取 bottom/top 中值，
  进入后如悬空轻微移动即可落地；各区几何 = ai-areas.xml 与真端 world 文件逐值一致的多边形）：

```
//moveto 600051000 614.9 2795.5 194.4    # LDF5a_QuestArea_Q12504（Talk）
//moveto 600051000 296.8  759.1 232.8    # LDF5a_QuestArea_Q12505（Hunt）
//moveto 600051000 1274.1 1978.2 93.2    # LDF5a_QuestArea_Q12524（Hunt）
//moveto 600051000 651.9  284.4 242.9    # LDF5a_QuestArea_Q22504（Talk）
//moveto 600051000 1454.1 1009.1 127.6   # LDF5a_QuestArea_Q22524（Hunt）
//moveto 220050000 1334.4 2011.6 145.7   # InvadePortalDest_41_questArea_02（39005，Hunt，df2a）
```

注意：④ 验的是**自动接取面**，不要先用 `//quest start` 建档（会掩盖触发面结论）；每个区
测完用 `//quest delete <id>` 复位再测下一个。LDF5a（600051000）为单人副本，需按正常入口
进入，进入后其五条 `//moveto` 可直接使用（`MoveTo` 已放行枚举外世界的**同世界内**传送，
`WorldMapType` 白名单外跨图直达仍拒绝）；df2a（220050000）在白名单内，可跨图直达。

## 回退与安全

- 任一项失败不影响其余项独立验收；失败现象优先记服务端日志（`aion/log/`，logback 默认
  `log/` 相对启动目录）+ 客户端截图。
- 全部四/五项通过后回填结论，P8 客户端验收轴（§10.3-#6 / #17①②）即可闭环。

## 验收结论（待用户回填）

> 回填格式：每项一行 PASS/FAIL；FAIL 附客户端现象与服务端日志行（`aion/log/` 最新
> `game.log` / `error.log` 中带 quest id 的行即可），代理据此走 fail-closed 异常批。
> Fill-back format: one PASS/FAIL line per item; for FAIL, attach the client symptom and
> the quest-id-bearing server log lines, and the agent will triage as a new fail-closed batch.

```
日期/时间：
客户端版本/登录账号等级（影响 ④ 副本入口与等级闸门）：

① Say 气泡        10033  PASS / FAIL   现象：
② Spawn 回收      10010  PASS / FAIL   现象：
③ DD 接取页       1870   PASS / FAIL   现象：
④ 进区接取  12504 12505 12524 22504 22524 39005   PASS / FAIL   各区结论：
⑤ 放弃段全链路     5000   PASS / FAIL   现象：
```
