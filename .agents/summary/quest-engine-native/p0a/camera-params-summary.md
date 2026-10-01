# P0a §4.4.2：相机参数矩阵（6 位 / 10 位逐行）

> P0a 只读审计产物 · 2026-10-01。逐行 TSV：`camera-params.tsv`（2463 行，每行带 文件:行号 证据）。
> 提取工具：`tools/camera_param_matrix.py`；相机函数 = `MainServer_ScriptDLL64/fun/fun_731.cpp:5306`（6 位 `FUN_180cb13b0`）与 `:5356`（10 位 `FUN_180cb14e0`）。

## 1. 总量与口径

| 口径 | 6 位 | 10 位 |
|---|---:|---:|
| 全树原始出现次数 | 2430 | 35 |
| 字面量调用行（解析成功） | 2429 | 34 |
| （差值 = 函数定义行本体） | 1 | 1 |
| 去重 (questId, slot) | 2429 | 34 |
| 去重 questId | 1786 | 26（与 6 位零交集） |

- 计划旧口径 2221/13 的过滤规则无法复现，**以本矩阵为准**（本矩阵每个原始出现都解析成功，且逐行带证据）。
- 相机函数签名：`(questId, ctx, slot, required, fullValue, flag)`；slot 1 基；shift = `slot*宽度 - 宽度`。

## 2. 校验结果（fail-closed 设计输入）

| 校验 | 结果 |
|---|---|
| 同任务 6/10 位混用 | **0 例**（1812 任务全部单一宽度）|
| 同任务 fullValue 不一致 | **0 例** |
| required 超 mask（63/1023）| **0 例** |
| fullValue == 各槽 required 按位组合 | 1810/1812 OK；**2 例 BAD：13912、23912**（见 §4）|
| 相机 id ∩ DD 表 id | **0**（证实 DD 零相机调用）|
| 相机 id 不在任何家族表 | 0（1812 个 id 全部有家族声明；全部属 SimpleHunt）|

## 3. 分布要点

- **全部 2463 个相机调用都属于 SimpleHunt 家族**（SimpleSerialHunt 16 行零相机——串行狩猎的推进机制待 §4.1 agent / P2 专查）。
- flag 参数：**2463/2463 全部 = 1** ⇒ 真端实际语义退化为「`newVars == fullValue` 即走推进通道」；flag 位同时上送共享记录（消费点未见，存疑项）。
- 10 位 required 实测集合 = {1, 65, 67, 77, 80, 100, 231, 235, 242, 251, 260, 329, 500, 1000}（14 种）。**计划 §2.2 "10 位 required ∈ {1,100,500,1000}" 口径过窄，以本矩阵为准。**
- 6 位 required ≤ 63 全部合法；fullValue 样例：1842 族 `0x450` ⇒ 10 位槽1=80 + 槽2=1（与计划例证吻合）。

## 4. 真端自身的表/脚本分歧（2 例，登记为 `NATIVE_CAMERA_TABLE_SCRIPT_DIVERGENCE`）

- quest **13912**（fun_762.cpp:670）与 **23912**（fun_762.cpp:679）：表行声明 `count1=1` + `count3=1`（**无 monster3**），代码生成器只为 slot1 生成了相机，但 fullValue=`0x1001` 含 slot3 满位。
- 含义：真端脚本里这两行永远到不了 fullValue ⇒ 推进通道不可达（交付推进不依赖此相机或任务不可完成，属真端数据残缺）。
- native 处置建议：相机注册以**脚本调用集**为准（slot1, fullValue 按脚本），登记表行 `count3` 无 monster3 为真端数据残缺；禁止 native loader 从表行重算 fullValue 时"自修复"该 2 行（防止发明语义）。

## 5. 对 P1/P2 的直接输入

1. `RawQuestVarsCodec`：6 位 ×5 槽（bit0..29）/ 10 位 ×3 槽（bit0..29）；bit30/31 为守卫哨兵区（`vars < 0x40000000`）。
2. `CameraStepSpec` 逐行字段与 TSV 一致；注册表 1812 任务 / 2463 步骤；缺行、混宽、fullValue 矛盾、required 超 mask 全部 fail-fast（真端基线为 0 违例 + 2 例登记分歧）。
3. 推进条件实现：`flag && newVars==fullValue`；flag 常量 1 来自表行（DD 驱动 codegen 的表行 flag 位），native 保持该输入位。
