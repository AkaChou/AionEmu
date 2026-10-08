# 客户端对话页 HTML 结构损坏修复（2026-10-08）

现象入口：任务 30600 简报 / 与 NPC 800324（Linocus）对话 → 对话页报错
`Error on reading "L10N/CHS/data/dialogs/ldf4b\ldf4b_Ii\Linocus.html" :: Line:15, Col:52
Error reading element value, both cdata and child element exist`（用户实机截图，OCR 提取）。

## 根因

客户端 pak 内 4 个 HTML 文件**原生结构损坏**（非服务端/任务问题）：

- 损坏形态一（3 处）：`<npcfuncs>` 元素同时含子元素和裸文本。Aion 解析器按
  「读元素值：cdata 与子元素不能共存」拒绝（与标准 XML 不同——标准 XML 允许 mixed content，
  所以 ElementTree 对这类错误是盲的，需用「剥离成对元素后残余文本」规则检测）。
- 损坏形态二（1 处）：开标签缺 `>`（`<stigma_enchant烙印强化</stigma_enchant>`）。

**关键证据**：4 个 pak 副本（实机当前、`data.pak.orig-backup`、unpak 两份）中这 4 个文件
**逐字节完全一致受损** → 损坏来自客户端资源源头本身，不是本地任何一次修补引入的
（形态像汉化编辑事故：中文错位在标签外）。

## 全库普查（21989 个 HTML）

npcfuncs 专项扫描（`npcfuncs_scan.py`，模拟 Aion 解析规则）：21989 个 HTML 全量扫描，
**恰 4 处**违规，无其他遗漏；另有 103 个任务书页 `quest_q*.html` 存在**另一类**
XML 实体/标签语法问题（与本次报错模式无关，未处理，见 `scan_report.txt` 观察项）。

| # | 文件 | 损坏 | 修复（同族取证） |
|---|---|---|---|
| 1 | `Dialogs/ldf4b/ldf4b_li/linocus.html` | `</match_maker>` 后多裸文本「德雷得奇安渗透」 | 删除裸文本——同族 12 个 match_maker 文件均为「npcfuncs 内只含子元素」；值「萨德哈德雷得奇安渗透」由 aluna 互证 + `client_strings_msg.xml:26965`「萨德哈德雷得奇安渗透作战开始」佐证 |
| 2 | `Dialogs/ldf4b/ldf4b_da/aluna.html` | 文本在 `<match_maker>` 外、元素内为空 | 文本移入元素（与 #1 同批、天/魔对称 NPC） |
| 3 | `Dialogs/df4_m/df4_v06_d_master_stigma.html` | `<stigma_enchant烙印强化` 缺 `>` | 补 `>`（同行 `stigma_open` 完好 + 兄弟文件 `1011_d_master_stigma` 为 `<stigma_enchant>烙印之石强化</stigma_enchant>`） |
| 4 | `Dialogs/ideternity_war/ideternity_war_l_wpseller_sp_03.html` | 引导句误入 npcfuncs | 移回正文 `<p>`（对照完好魔族同档 `d_wpseller_sp_03` 与前一档 `l_wpseller_sp_02`） |

## 用户报错 NPC 的挂靠关系（为何不能靠"禁任务"规避）

Linocus（800324）挂 **3 个任务**：30600（简报）、30601 消耗品供给（接取+领奖，
max_repeat=255）、30602 补给品收集（接取+领奖，max_repeat=255）；Aluna 同理挂
30610/30611/30612。另 2 处损坏（df4 烙印功能 NPC、永恒战场物资商人）与 30600 完全无关。
客户端报错在「加载该 NPC 的 HTML 文件」本身——凡触发对应 NPC 对话窗口的路径都会撞，
禁单个任务只能堵一条路。30600 已确证**不是任何任务的前置**（退役前全 quest_definition
目录仅自引用；1162.xml 命中的是 EXP 数值）。

## 修复实现

保持条目原编码（UTF-16LE+BOM）与 `0x81` 加密层（XOR 对合，见 `repair_lib.py`：
`encrypt_blob = _crypt_aion_html_blob(0x81 0x81 + utf16le)`），只替换 4 个条目。
工具：`repair_lib.py`（提取/解密/加密/重建）、`fixes.py`（4 项替换定义 + 唯一性断言）。

**验证（`verify_repaired.py`）**：
- 4 个修复文件：ET 解析 + 混合内容检查器全净；
- 全条目逐字节等价：其余 23267 条**逐字节相同**、条目数与顺序一致、差异恰为 4 且每处
  仅含修复行（文本级 diff 复核）。

## 三方版本关系（对比基准）

| 版本 | 相对客户端原版 | 说明 |
|---|---|---|
| 客户端原版（`data.pak.orig-backup`，7/1） | 基准 23132 条目 | |
| 实机当前（9/23 15:49） | +**1 项**（`UI/game/global_option_dialog.xml`） | 此前从原版换过 1 个 UI 文件 |
| patch 候选（9/23 19:38） | +**58 项**（55 Dialogs + 2 Strings + npcs） | ⛔ 文档标注不可整包部署（未验证会崩） |

两批改动互不相含；4 个损坏文件在三方**字节一致**。严格按 CPK-001「以客户端现用文件为基准」
原则：部署基准用实机文件，修复后 diff 恰为 4（均为已验证的修复行）。

## 落地记录（2026-10-08）

### 实机部署（客户端）

- 备份：`~/IdeaProjects/5.8客户端/L10N/CHS/Data/data.pak.bak-2026-10-08`
  （94,240,820 B，SHA-256 `cf78b2b8…`；回滚即覆盖回去）
- 部署：`/tmp/data.pak.repaired`（94,240,837 B，SHA-256 `3df976d5…`）= 实机现用文件 + 4 项修复
- **生效需完全重启客户端**（pak 为启动时读入）

### patch 交付物（2026-10-08 收敛）

- 首次同步：并入 4 项修复后（95,915,185 B，MD5 `a8c48709…`，含 58 项未验证候选）。
- **客户端实测：该包整包替换后启动崩溃**——实证原「58 项未验证、不可整包部署」警告。
- 最终处置（按用户指令）：**删除**该候选包；`patch/L10N/CHS/Data/data.pak` 改为
  **实机客户端目录版本**（= 客户端现用基准 + 4 项修复）：94,240,837 B，
  MD5 `772b0555f80d38ecf865ea184494e6fa`，SHA-256 `3df976d5…`——与本机实机部署**同字节**，
  diff==4，✅ 可部署（基准核对见下）。
- 58 项候选资料归档：git 历史（`e05348f55` 及其前序）+ `/tmp/data.pak.patch-backup-2026-10-08`；
  将来启用仍按 `CPK-001` 逐条目二分。
- `patch/patch_documentation.md` 已同步（中英表格行 + data.pak 节）。

### 它机部署（客户端在另一台电脑时）

本包基准 = 2026-09-23 版客户端文件（SHA-256 `cf78b2b8…`）。对其它机器先核对其现用
`data.pak` 的 SHA-256：等于 `cf78b2b8…` 可直接用本包替换；不一致（如纯原版 `ac54a2ee…`）
须以该机现用文件为基准重做 4 项单条目替换。替换前备份该机原文件，替换后**完全重启客户端**。

## 复测口径

1. **完全重启客户端**（必须）→ 与 800324（Linocus）对话：页面应正常打开，不再报
   both-cdata 错误（30600 简报路径 / 直接交互均可）；
2. 顺带可查：Aluna（魔族对应 NPC）、df4 区域烙印 NPC 页、永恒战场物资商人（sp_03 页）；
3. 若异常：用 `data.pak.bak-2026-10-08` 覆盖回滚（同样需完全重启）。

## 复盘要点

- 修复**只要落在结构层**：值选「萨德哈德雷得奇安渗透」有双侧互证；只补 `>` 不改文本；
  引导句按族内位置移回正文。
- 判定此类损坏用「Aion 解析规则」而非标准 XML（ET 对 mixed content 盲）——
  两者互补才能全量发现（见 `mixed_content_scan.py`）。
- 不得整包部署 patch 候选（文档已警告）——部署包必须实机基准 + 最小 diff。
