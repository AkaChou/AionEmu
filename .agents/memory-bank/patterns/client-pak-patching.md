# Client Patch & pak Data Patterns (客户端补丁与 pak 数据模式)

本文档记录 Aion 5.8 客户端 `.pak` 数据文件的结构事实与改包纪律。客户端不在仓库内，但 `patch/` 下的
客户端补丁是本仓库的交付物，改错会让玩家客户端在装载期崩溃。

> Pattern IDs: `CPK-001`–`CPK-003`
> card_status: ACTIVE; verify source and runtime evidence before treating a claim as universal
> scope: 客户端 `data/**`、`L10N/**`、`Levels/**` 下的 `.pak` 条目改动与回封
> last_reviewed: 2026-09-23

---

## [CPK-001] 改 pak 条目必须保持该条目原有编码，且只替换单条目
<!-- pattern-metadata
status: CONFIRMED
scope: 客户端 .pak 内 XML/数据条目的替换与回封（`aion_pak.py` / 自建封包脚本）
first_seen: 2026-09-23
last_verified: 2026-09-23
symptom: 替换客户端 .pak 后客户端一进游戏即崩溃；或数据看似生效但客户端行为异常；`aion_pak.py unpack` 报 decoded=N，回封包报 decoded=0；单条目替换本身没问题，但按仓库上一版产物整包发出后仍然崩溃
root_cause: 两个独立成因——①客户端数据装载器按条目**原有编码**解析（`data/Npcs/npcs.pak` 内是 Aion 二进制 XML），而 `aion_pak.py unpack` 会把二进制 XML 解码成**文本 XML**、`pack` 没有任何反向编码器，整包重封等于把所有条目换成文本 XML；②交付物以**仓库上一版补丁产物**为基准，而它与客户端现用文件相差 N 个条目（这些改动从未在该客户端验证过），整包发出等于一次性引入 N 处变化
fix_or_guardrail: 改动前先确认条目原编码；一律「解出目标条目 → 就地改 → 只替换该条目」，其余条目连压缩流逐字节复制；二进制 XML 用 .agents/summary/client-hp-display/patch_binary_xml_entry.py（内含与解码器配对的编码器）。**并且以「客户端现用文件」为基准**（不是仓库上一版补丁产物），出货前用 zipfile 逐条目比对确认差异条目数 == 1，并把基准的大小/MD5 记为交付物的 parent
evidence: .agents/summary/client-hp-display/2026-09-23-client-hp-display-switch.zh-CN.md（§8–§10、§13–§15、§17：含已删除产物 npcs.pak / data.numbers.pak 的重建配方与 v1/v2 的证伪结论）; .agents/summary/client-hp-display/patch_binary_xml_entry.py; .agents/summary/client-hp-display/patch_pak_entry.py; patch/patch_documentation.md（只记录最终交付物）
validation: runtime；2026-09-23 用户实测「整包重封为文本 XML」的 npcs.pak → 客户端崩溃；改回二进制 XML（全文件仅 1 字节变化）后待复测；同日实测按仓库版 data.pak（与客户端现用文件差 60 条目）整包替换 → 客户端崩溃，改为以客户端现用文件为基准的最小差异包（差异条目 = 1）后待复测；static；16 个 XML 条目经编码器往返解码结构全等；两份 pak 的 zip 容器结构（extra/method/flag_bits）逐项比对同型
boundaries: 崩溃只在 data/Npcs 的 npcs.pak 上实测；L10N data.pak 本身是 UTF-16 文本 XML（可整包？否——仍用单条目替换）而 Levels/lf2a/Level.pak 曾以文本 XML 回封实测可用 → **编码要求因包而异，必须逐包确认原编码，不能外推**
superseded_by: none
first_check: `aion_pak.py unpack` 的 decoded= 计数与目标条目的原始编码（magic 0x80 = 二进制 XML）；回封脚本是否只是标准 zip / 是否保留原载荷字节
keywords: 客户端崩溃、pak 替换后进游戏崩溃、npcs.pak、data.pak、Level.pak、aion_pak.py、二进制XML、binary xml、血条不显示、hpgauge_level、最小差异包、基准文件、差异条目数
-->

**规则**：客户端 pak 的改动按「单条目替换」做，不做整包重封；改动前先判定该条目原编码并保持它；
**交付物一律以「客户端现用文件」为基准**，出货前核对差异条目数 == 1。

- **反例（2026-09-23 实机崩溃 · 基准错）**：以仓库里上一版 `patch/L10N/CHS/Data/data.pak`
  （叠加了 55 个任务对话 HTML 等共 60 处本地化改动）为基准生成整包 → 客户端崩溃。逐条目比对显示
  该基准与客户端现用文件差 **60 个条目**，容器结构（`extra`/`method`/`flag_bits`）却完全同型
  —— 崩溃来自那 60 处未验证改动，不是容器。改用「客户端现用文件 + 单条目」后差异条目 = 1。

- **反例（2026-09-23 实机崩溃）**：把 `client_npcs_*.xml` 的 `hpgauge_level` 改成 `45` 后用
  `aion_pak.py pack` 整包回封 → 客户端**一进游戏即崩溃**。旁证：`unpack` 对原始 pak 报 `decoded=15`、
  对回封包报 `decoded=0`（即回封包里已无二进制 XML 条目）。
- **正例**：只解出 `npc_ui.xml`，在二进制流内改一个字节，其余 16 个条目（含压缩流）原样复制 →
  容器仅 +12 字节（来自那一条目的重压缩），解压后 8,178 字节里**只有 1 个字节**不同。
- **别把「某包能用文本 XML」当通例**：`Levels/lf2a/Level.pak` 的文本 XML 回封此前实测可用，
  与 `npcs.pak` 的崩溃并不矛盾——两类包由不同装载路径解析。
- **「还原」也要按基准核对，不能只撤自己那一处**：2026-09-23 崩溃后只把新增的「数字」改动撤掉，
  仓库文件仍含那 58 处未验证改动，被用户指出「没有回滚」。真正的回滚是
  `git checkout -- patch/L10N/CHS/Data/data.pak`（95,915,168 / `9e624783…`）。**回滚后仍 ⛔ 不可部署**
  ——它与客户端原版差 58 个条目（55 `Dialogs/*` + 2 `Strings/*` + 1 `npcs/npc_mesh_replace.txt`）；
  客户端现用文件（94,240,820 / `b47db70c…`）才是唯一已验证的基线（= 原版 + 勾选框）。

## [CPK-002] Aion 5.8 pak 容器与二进制 XML 的格式事实
<!-- pattern-metadata
status: CONFIRMED
scope: 解析/生成客户端 .pak 容器与其中的 Aion 二进制 XML
first_seen: 2026-09-23
last_verified: 2026-09-23
symptom: 用 zipfile 读 pak 抛 BadZipFile；自建封包后客户端不认；二进制 XML 自写编码器解出错误节点名
root_cause: .pak 是 zip 但三处签名 XOR 0xFF，且每条目压缩流前 32 字节另做前缀 XOR；二进制 XML 的字符串表索引是字节偏移/2 而非序号，凭直觉按序号编码会让节点名整体错位且解码器不报错
fix_or_guardrail: 容器三处签名 XOR 0xFF（本地头/中央目录/EOCD），载荷前缀 XOR 用 AION_XOR_TABLES（v1 偏移 (csize&31)*32、v2 偏移 csize&1023，自反）；二进制 XML 索引一律按「字节偏移 ÷ 2」读写，改取值=换一个索引字节（等长）
evidence: .agents/summary/client-hp-display/pack_pak_aion_format.py; .agents/summary/client-hp-display/patch_binary_xml_entry.py; patch/patch_documentation.md
validation: static；编码器对 npcs.pak 全部 XML 条目往返解码结构全等；容器回解后 17 条目中仅目标条目变化
boundaries: 仅在 5.8 客户端数据上核对；XOR 版本需按包自动探测，不能假定 v2；二进制 XML 的 flags 只实测到 bit0=值/bit1=属性/bit2=子节点，更高位含义未知
superseded_by: none
first_check: 目标 pak 的 XOR 版本（用首个 deflate 条目试解 + CRC 校验探测）；条目是否为二进制 XML（首字节 0x80）
keywords: pak 格式、PK\x03\x04、XOR 0xFF、AION_XOR_TABLES、二进制 XML、0x80、字符串表、索引偏移、pack_pak_aion_format、patch_binary_xml_entry
-->

**容器**（`patch_pak_aion_format.py` 已实现）：

| 变换 | 内容 |
|---|---|
| 签名 | 本地头 `PK\x03\x04`→`af b4 fc fb`、中央目录 `PK\x01\x02`→`af b4 fe fd`、EOCD `PK\x05\x06`→`af b4 fa f9`（各 XOR 0xFF） |
| 载荷 | 压缩流**前 min(32, csize) 字节**再 XOR `AION_XOR_TABLES[version]`，表内偏移 v1 = `(csize & 31) * 32`、v2 = `csize & 1023`；变换自反 |
| 版本探测 | 按条目试 v1/v2 前缀 XOR → inflate → 比 `usize` 与 CRC32 |

**二进制 XML**（解码器 `aionpak.binary_xml`；编码器见 `patch_binary_xml_entry.py`）：

1. `0x80` → packed-s32 字符串表长度 → 字符串表（UTF-16LE，每串以 `\x00\x00` 结束）；
2. 节点：`name(索引)` + 1 字节 flags（bit0 有值 / bit1 有属性 / bit2 有子节点），值、属性键值、
   子节点数量与子节点全部为 **packed-s32**；
3. **字符串表索引 = 该串在表中的字节偏移 ÷ 2**（索引稀疏：首串偏移 2 后，下一串索引即为 9 而非 2）。
   按序号编码 → 解码器会静默读出**错位的名字**（`<npc_ui>` 被读成 `<pc_uis>`），不报错；
4. 节点的「值」本身是索引，因此把 `0` 改成 `1` 只是把一个索引字节换成另一个，**文件长度不变**。

## [CPK-003] gauge 血条数字＝控件属性 `num_type`；头顶世界血条没有数字通道
<!-- pattern-metadata
status: CONFIRMED
scope: 客户端 gauge 控件（`type="gauge"`）的数字显示；怪物头顶世界血条的显示判定
first_seen: 2026-09-23
last_verified: 2026-09-23
symptom: 世界血条能显示但没有数字；以为改客户端数据（npc_ui / hpgauge_level）或服务端模板能开关数字；按 4.6/32 位偏移打补丁无效；改了列表单元格渲染器的显示模式也不见数字
root_cause: 数字由控件字段决定——`+0x8d4`（`num_type`：default/small/micro）与 `+0x364`（`value_type`：number/float/percent/percent_float/curNumber），字段由 UI 数据属性写入；控件更新函数 `0x108e125d` 读 `num_type` 后调数字文本函数 `0x1097d290`。头顶世界血条却是**代码构造**：`over_hpgauge` 位判定在 `0x108c3a3a`（全 DLL 唯一），绘制经 `0x108c3ed0`→`0x108c2250`，该路径从不调用 `0x1097d290`（该函数全 DLL 仅 7 个调用点）；全部 2477 个 pak 解压内容里 `num_type`/`value_type`/`has_gauge`/`naturally` 无一处属于头顶血条，`npc_ui.xml` 的 45 档 × 25 字段全是显示开关
fix_or_guardrail: 想要数字只有两条：①给**数据里有的** gauge 控件加 `num_type="small"`（目标窗口已做成 data.pak 补丁，机制与组队血条完全相同）；②打 Game.dll 补丁——把 `0x8e141c` 的 `mov eax,[rbx+0x8d4]` 强制成 `mov eax,1`，并 NOP `0x8e13eb`/`0x8e13f9` 两处「不画数字」判定。**不要再**去改 npcs.pak、服务端模板或列表单元格渲染器（v1/v2 五处 `0x93ded4` 等属 `column_set` 单元格渲染器，实测无可见效果）
evidence: .agents/summary/client-hp-display/2026-09-23-client-hp-display-switch.zh-CN.md §14; patch/patch_documentation.md
validation: static；属性应用器 `0x1093c3xx`–`0x1093d1a1` 逐条解出「属性→字段」映射；`0x1097d290` 的 7 个调用点全部枚举（无一在世界血条路径）；2477 个 pak 全量扫 `num_type`/`value_type` 仅命中窗口类 UI；runtime；2026-09-23 用户实测——①v1/v2（列表单元格渲染器）无可见效果；②**v3（强制 `num_type=small` + 去两处前置判定）有效：窗口内血条出数字，但头顶世界血条仍无数字**（头顶血条由 `0x108c3ed0`→`0x108c2250` 手动绘制，不走 UI 控件的数字路径）
boundaries: 仅 5.8 本机发行版；32 位 game.dll 代码节加密，其偏移无法核对；控件字段偏移（`0x8d4`/`0x364`/`0x35c`/`0x368`）随版本可能变化；**头顶世界血条的数字需要代码注入**（在其手动绘制路径 `0x108c3ed0`→`0x108c2250` 里额外调用数字文本函数 `0x1097d290`）——v3 这类「翻开关」补丁对它无效，已实测；若某版本给头顶控件补了数据定义或改走 UI 控件绘制，则结论需重估
superseded_by: none
first_check: 先判断数字属于哪一类 gauge——数据里有没有 `num_type`（有→改数据即可）；再看头顶血条在数据里有没有控件定义（没有→只能改代码，且必须先确认它是否走控件数字路径，否则任何「模式翻转」都无效）
keywords: 血量数字、怪物血条数值、头顶血条、num_type、value_type、small、micro、has_gauge、naturally、0x1097d290、0x108e125d、over_hpgauge、4.6 分支、gamebase+0x3532CD
-->

**结论链**：

1. 「有没有血条」与「有没有数字」是两层：`over_hpgauge=1` 决定**头顶有血条**（客户端默认就有，
   用户实测「不打补丁也有血条」）；数字是控件层的 `num_type`，两者互不相关。
2. 数字由 **gauge 控件的字段**决定，字段由 **UI 数据属性**写入（属性应用器把
   `num_type` 写进 `+0x8d4`、`value_type` 写进 `+0x364`）；控件更新函数 `0x108e125d`
   在 `0x108e141c` 读 `num_type`，`0x108e1465` 调数字文本函数 `0x1097d290`。
3. 组队血条有数字＝数据里写了 `num_type="small"`；目标窗口没有 → 无数字；
   头顶世界血条**在任何数据文件里都没有控件定义**（全客户端 2477 个 pak 零命中）。
4. 头顶世界血条的代码路径（`0x108c37df`→`0x108c3ed0`→`0x108c2250`）**从不调用**
   `0x1097d290`，故 5.8 本客户端不存在「头顶数字」的可开关实现。
5. v1/v2 那五处（`0x93ded4`/`0x93e00a`/`0x93e021`/`0x93dd2b`/`0x93dd5c`）属
   `column_set` 列表单元格渲染器（`title,width,align,<显示模式>,font,key` 第 4 字段），
   与头顶铭牌无关，实测无可见效果——这是一次「按偏移猜补丁点」的反面教材。
6. 落地补丁（2026-09-23 精简后的最终形态）：**`patch/Game.dll`**（MD5
   `71a146481980f284e1144967037e3ab1`，29,795,000 字节，与原版逐字节差 502 处）= 原版 + VIP +
   v3 三处（强制 `num_type=small` ＋去掉两处前置判定）；同日删除的 `Game.hpnum.dll`／
   `Game.vip-hpnum.dll`／`data.numbers.pak`／`data/Npcs/npcs.pak`／`data/ui/game/game.pak`
   仅作过程证据，重建配方见 `patch/patch_documentation.md`。
