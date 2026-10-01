# P0a §4.3 语义矩阵：`can_report`（真端 vs 本服）

> P0a 只读审计产物 · 2026-10-01 · 审计工作区：QE-112 在飞（未提交），本文件数字为当日快照，P0a 收口前不作为冻结验收数字。
> 方法：server58-source 反编译快照全树 grep + quest.xml（UTF-16）解析 + 客户端解包 quest.xml 交叉验证。所有结论带 文件:行号 证据；推断处已标注。

an_report` 考古报告

## 0. 先决更正（证据）

- **`can_report` 不是 XML 属性，是子元素** `<can_report>1</can_report>`。quest.xml（UTF-16LE，BOM `ff fe`，22,691,384 字节，根 `<quests>`，共 10,035 个 `<quest>`）中该字符串出现 434 次 = 217 个元素 × 开/闭标签，与“约 217 行”吻合。
- **加载点行号更正**：`MainServer_ScriptDLL64/fun/fun_249.cpp` 全文仅 1657 行，**没有任何 quest.xml 加载，也没有 can_report**（grep 验证）。ScriptDLL64 只加载 `quest\event_quest.xml`（`/Users/mc/IdeaProjects/58Server/server58-source/MainServer_ScriptDLL64/fun/fun_912.cpp:930`）。用户记忆中的 3431/3484 行实际属于 `MainServer_Server64/fun/fun_249.cpp`。

---

## 1. 数据分布（可复现命令 + 输出）

```bash
python3 - <<'EOF'
import xml.etree.ElementTree as ET
from collections import Counter
root = ET.parse('/Users/mc/IdeaProjects/58Server/Map/XML/quest.xml').getroot()
rows = [(q.findtext('id'), (q.find('can_report').text or '').strip(),
         q.findtext('minlevel_permitted'))
        for q in root.iter('quest') if q.find('can_report') is not None]
print(len(rows), Counter(v for _, v, _ in rows))
EOF
```

**输出：`217 Counter({'1': 217})`**

| 维度 | 结果 |
|---|---|
| 总值数 | **217**（quest.xml 共 10,035 个任务） |
| 取值分布 | **全部为 `1`**。不存在 `0`/`true`/`false` 变体 |
| 地区变体 | Japan/quest.xml：213 个全为 1；China/quest.xml：211 个全为 1 |
| 按等级段（minlevel_permitted） | 10-19: 2，20-29: 2，30-39: 4，**40-49: 34，50-98: 140，999(特殊): 35** → 高等级/日常向 |
| 家族交叉 | Quest_SimpleHunt **18**：11311-11318, 16931, 21311-21318, 26931；SimpleTalk/SimpleCollectItem/SimpleUseItem/SimpleItemPlay/SimpleSerialHunt/CombineTask 均 **0**；data_driven_quest.xml（2492 条）**197**；SimpleHunt 与 dd 不重叠，并集 215，两个都不在（未查明的 2 个） |

**⚠️ 用户要求“=1 与 =0 各半”无法满足：217 个带标记的值全为 1，不存在 =0 的行。** 语义上的"0" = 9818 个**不含该元素**的任务（解析器缺省为 0）。

代表 10 个 can_report=1（真端 dev_name 为韩文）：

| id | 家族 | dev_name（韩文原样） | minlvl |
|---|---|---|---|
| 1877 | 仅 quest.xml（dd 之外） | [긴급 지령] 마군 9급병 처치（紧急命令/魔军讨伐） | 45 |
| 2878 | 仅 quest.xml | [긴급 지령] 천군 8급병 처치 | 45 |
| 13841 | data_driven | [일일] 왼쪽 날개 그늘 소탕 작전（每日清扫作战） | 45 |
| 11311 | SimpleHunt | [목/일] 게르하 진격로 지원 | 999 |
| 21313 | SimpleHunt | [목/일] 게르하 진격로 공격 명령 | 999 |
| 16829 | data_driven | [인던/파티] A지역 - 방어막 데바 | 66 |
| 25712 | data_driven | [긴급 지령] 뢰베의 요청 | 66 |
| 27511 | data_driven | 기억 (마족) | 66 |
| 80982 | data_driven | [이벤트] 창조력 지급 퀘스트 24 | 66 |
| 1877–1887 系列 | quest.xml | 긴급 지령（紧急命令）系列共 11 个 | 45 |

代表 10 个 de-facto 0（无该元素）：85, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008（新手剧情任务）。

---

## 2. 代码考古（server58-source 全树）

### 2a. 加载点：存到哪个字段

全树大小写不敏感 grep `can_report|CanReport|IsCanReport` 仅命中 **2 个文件**（NPCServer 与 MainServer 各一），字符串只以日志形式 `L"Quest::Set, can_report"` 存在——即这些是**通用 XML 元素分发器里的 case 分支**，用日志字符串标注属性名。

**MainServer 加载点** — `/Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/fun/fun_249.cpp:4228`，位于元素分发函数 `FUN_140d1b3d0(int *param_1, uint64_t *param_2)`（定义于 :3705，即 QuestDB 每元素 setter）：

```c
case 0x15e:                                              // = 350, "can_report" 的分发 ID
  iVar4 = FUN_140d98030(param_2, param_1 + 0x1f3b);      // 从 XML 读 int 写入
  if (iVar4 == 0) { FUN_140da2250(L"Quest::Set, can_report"); return; }
```

`param_1` 是 `int*`（4 字节步长）→ **字节偏移 = 0x1f3b × 4 = 0x7CEC**。

**NPCServer 加载点** — `/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/fun/fun_052.cpp:3822`，位于 `FUN_1402abf20(int64_t param_1, uint64_t *param_2)`（:3448，NPCServer 自己的 QuestDB 加载器）：

```c
case 0x15e:
  FUN_1403150d0(param_2, param_1 + 0x7cec, L"Quest::Set, can_report");
```

`param_1` 是 `int64_t`（字节指针）→ 同样是**字节偏移 0x7CEC**。

**交叉验证（推断→高置信）**：相邻属性两边完全对齐——`cannot_giveup` MainServer `+0x1c`(int*)=0x70 / NPCServer `+0x70`(byte)，`cannot_share` `+0x1d`=0x74 / `+0x74`。即 **quest 模板结构体字节偏移 0x7CEC 处存一个 int（0/1），MainServer 与 NPCServer 各持一份副本**。0x15e（十进制 350）即 `can_report` 标签的注册分发 ID（证据：该 case 是唯一以 "Quest::Set, can_report" 为失败日志的分支；标签名→ID 的注册表未在本快照中反编译出来，属推断）。

quest.xml 装载入口本身：`fun_249.cpp:3431`（`quest.xml` / `quest_%s.xml` 地区变体）与 `fun_249.cpp:3484`（`FUN_140d1a880` 加载 `%squest\quest.xml`），经通用装载器 `FUN_140d1aa50(&DAT_14f4395a0, ...)` 分发到上述 switch。模板查询函数 `FUN_140d1df50`（fun_249.cpp:5404-5432）按任务 id 在红黑树 `DAT_14f4395c8` 中查找，返回模板指针。

### 2b. 消费点：全树只有一个

**唯一消费点** — `/Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/fun/fun_239.cpp:13111`，位于 `FUN_140c56090`（:12808 起，即 Socket.cpp 的 **C_HACTION** 处理器；上下文断言字符串 "C_HACTION: ..."、"Socket.cpp"、packet 0x805b 可证）：

```c
// 仅当目标 NPC 类型 *(int*)(plVar6 + 0x144) == 1（任务 NPC）且
// 对话动作码 uVar7 ∈ [0x6c..0x7c] 且 != 0x6d 时到达此处
UserQuestData_GetQuestState(plVar5 + 0x8b0, local_238, uVar18);
if (local_238[0] != '\x04') goto LAB_140c5678e;          // 任务状态必须是 4
lVar15 = FUN_140d1df50();                                 // 按 id 查 quest 模板
if ((lVar15 != 0) && (*(int *)(lVar15 + 0x7cec) != 0)) {  // ★ can_report != 0
  iVar3 = 0;
  if (uVar7 != 0x6c) { iVar3 = uVar7 - 0x66; }
  *(bool *)(plVar5 + 0xa1a) = (uVar9 == 0x7f);
  User_OnGiveQuestRewardFromNpcServer(plVar5);            // 直接走 NPCServer 发奖
  *(uint32_t *)((int64_t)plVar5 + 0x435c) = 0;
  (**(code **)(*plVar5 + 0x638))(plVar5, ...);            // 发包
  User_OnEndDialog(plVar5, (int)plVar5[0xada]);           // 结束对话
  ...清空对话 id...
}
LAB_140c5678e:  // can_report==0 时直接落到这里：走 NPC 对象 vtable+0x560 的标准脚本化对话分发
```

**解读**（代码路径为证据，UX 表述为推断）：

- 任务状态枚举证据：`/Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/classes/Account/UserQuestData.cpp:6237-6251` —— `0 = none, 3 = acquired, 4 = success`。
- 因此 can_report **不是**“未满进度提前报告”开关：状态必须已是 4（success/可交付）才会进入该分支。
- 真实语义：**can_report=1 的任务在“可交付”状态下，客户端发来的对话动作（0x6c..0x7c 段，即任务报告类按钮）由 MainServer 快速通道处理——直接调 `User_OnGiveQuestRewardFromNpcServer` 发奖并关闭对话框；can_report=0（缺省）则只走标准 NPC 脚本化对话分发（NPC vtable+0x560，交给 NPCServer 侧脚本流程）**。即：允许“一键报告交付”、跳过冗长的脚本对话步骤。
- 对照组（同一函数的正常触发路径）：`/Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/fun/fun_049.cpp:12613` —— NpcSocket.cpp 的 **"GiveQuestRewardPacket"**（NPCServer→MainServer, packet 0x8055）驱动同函数，这是不依赖 can_report 的标准发奖路径。另有 `IUserImp.cpp:1874/1942` 的 `IUserImp::AutoQuestReward`（PCScriptInterface.cpp）脚本 API 也调用它。

### 2c. 结论：有消费点，但仅 1 处

- **消费点确实存在**（MainServer 1 处），并非“只加载不读取”。
- **NPCServer**：自己加载并存到 0x7CEC，但快照中**未发现任何读取**该偏移的代码（grep `0x7cec`/`0x1f3b` 全模块仅命中 setter 本身）。若 NPCServer 用到，只能是通过整个模板结构体隐式传递——未找到证据。
- **ScriptDLL64**：不加载 quest.xml，无 can_report 字符串、无 0x7CEC 偏移引用（全树 grep 覆盖）。**没有通过脚本 DLL 隐式使用的证据**。
- 偏移穷尽性：全树 grep `0x7cec` 仅 2 命中（NPCServer setter、MainServer 消费点）；`0x1f3b` 仅 setter + 一个误命中（`fun_048.cpp:2990`，是 Guild.cpp 源码路径字符串里的行号巧合）。

---

## 4. 客户端侧证据

客户端解包根 `/Users/mc/PycharmProjects/unpak/Quest_unpacked/` 只有 6 个文件：`quest.xml`、`challenge_task.xml`、`combine_task.xml`、`data_driven_quest.xml`、`quest_monster.csv`、`quest_script_monster.csv`。

- 客户端 `quest.xml` 同样内嵌 `<can_report>`：**10,035 个任务中 217 个，全为 1，与服务端逐一对应**。
- 抽查 3 个 can_report=1 的 id：**1877**（minlvl 45）、**11311**（minlvl 999，SimpleHunt）、**80982**（minlvl 66，dd）——三者客户端条目均含 `<can_report>1</can_report>`。
- 客户端 `<name>` 均为占位符（Q1877/Q11311/Q80982），dev_name 为空——文案在客户端其他分包中，本解包根内**没有**任务书/对话页文件。
- **说明**：“报告按钮”的实际 UI 行为在客户端二进制里，本解包根无法定位到按钮级证据；能给出的最强证据是：客户端数据侧存在与服务端完全一致的 can_report 标记（客户端任务对话框构建读取 quest.xml，故该标记是客户端决定可否直接报告/交付的数据依据）。结合服务端消费点位于 C_HACTION 对话动作处理器，可以自洽地推断客户端在 can_report=1 任务的交付对话框上提供直接报告选项（此为推断，标注区分）。

---

## 关键文件清单

| 用途 | 绝对路径 |
|---|---|
| 真端任务主表 | /Users/mc/IdeaProjects/58Server/Map/XML/quest.xml |
| 家族表 | 同目录 Quest_SimpleHunt.xml（命中 18）、data_driven_quest.xml（命中 197） |
| MainServer 加载 | /Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/fun/fun_249.cpp:4228（存偏移 0x7CEC）、:3431/:3484（quest.xml 装载）、:5404（模板查找） |
| NPCServer 加载 | /Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/fun/fun_052.cpp:3822（存偏移 0x7CEC，无读取） |
| **唯一消费点** | /Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/fun/fun_239.cpp:13111（C_HACTION 快速交付） |
| 状态枚举证据 | /Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/classes/Account/UserQuestData.cpp:6237-6251 |
| 对照发奖路径 | /Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/fun/fun_049.cpp:12613、classes/Account/IUserImp.cpp:1874/1942 |
| 客户端数据 | /Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml（同样 217×1） |

**对本服（Java 仓库）建模建议的事实基础**：can_report 是每任务可选 bool 元素，缺省 0；=1 仅影响一件事——可交付状态下允许通过对话动作快速报告交付（跳过脚本化对话流程）；真端 217 个均为 1，集中于高等级/日常/紧急命令类任务，且 215/217 同时收录于 data_driven_quest 或 Quest_SimpleHunt 家族表
