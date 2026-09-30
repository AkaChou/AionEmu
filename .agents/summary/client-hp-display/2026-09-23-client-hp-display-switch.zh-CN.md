# 客户端「显示 NPC 血量」开关定位（2026-09-23）

## 1. 排查起因

用户得到线索：**32 位 `game.dll` 的 `gamebase + 0x3532C6` 可开启血量显示，默认不显示**。
需要排查这是否就是「怪物血量不显示／不实时」的原因。

## 2. 结论

### 2.1 客户端确实存在该开关（线索方向正确）

64 位 `bin64/Game.dll` 中定位到**选项窗口的控件注册表**：一串形如

```
mov rax, [rdi]              ; 48 8b 07
mov rcx, rdi                ; 48 8b cf
call [rax+0x340]            ; ff 90 40 03 00 00   —— 虚函数：创建控件
lea rdx, [rip+disp]         ; 48 8d 15 xx xx xx xx —— 控件名
mov r8d, 0x2001             ; 41 b8 01 20 00 00  —— 控件属性
mov [rdi+0x5XX], rax        ; 48 89 87 xx 05 00 00 —— 存入成员
```

的模板，从 `0x28fd99` 顺序铺到 `0x2903d5`，**全部无条件执行**。其中血量组：

| 控件名 | 属性 | 成员偏移 | 语义 |
|---|---|---|---|
| `cb_show_hp_me` | `0x2006` | `[rdi+0x5c0]` | 显示自己的 HP（combobox，与 `cb_title_show_option`/`cb_guild_show_option` 同组） |
| `cb_show_hp_party` | `0x2001` | `[rdi+0x5c8]` | 显示队伍 HP |
| `cb_show_hp_alliance` | `0x2001` | `[rdi+0x5d0]` | 显示军团 HP |
| **`cb_show_hp_npc`** | `0x2001` | **`[rdi+0x5d8]`** | **显示 NPC（怪物）HP ★** |
| `cb_show_hp_target` | `0x2001` | `[rdi+0x5e0]` | 显示目标 HP |
| `cb_show_gauge_num` | `0x2001` | `[rdi+0x5e8]` | 显示计量条数值 |

> **2026-09-23 更正**：上表早期版本整体错位一格（把 `cb_guild_show_option` 当成了 `cb_show_hp_me`）。
> 注册块里 `GetWidget` 的返回值不是紧跟自己的 `lea` 存回：`lea <下一个控件名>`（7 字节）+
> `mov r8d, attr`（6 字节）夹在调用与其存储之间，所以**存储属于前一次调用**。
> 用 `cb_show_name_*` 一串可自校验（`name_me`→0x550、`party`→0x558、`alliance`→0x560、`npc`→0x568、
> `pc`→0x570、`etc_npc`→0x578、`pc_other`→0x580、`gather`→0x588、`pvp_rank_title`→0x590、
> `title_show_option`(0x2006)→0x598、`guild_show_option`(0x2006)→0x5a0）。

同族的 `cb_show_name_*`（我／队伍／军团／NPC／玩家／采集…）也在同一段内注册。

**定位方法**（可复用）：字符串表里 `strings | grep cb_show_hp_npc` 拿文件偏移 → 换算 VA →
全节扫描 `REX.W + 8D /r` 且 `modrm & 0xC7 == 0x05`（RIP 相对 `lea`）→ 匹配 `rip+disp32 == VA`。
每个控件名**恰好一个** `lea` 引用，且相邻项间隔 0x20，说明是同一张注册表的顺序展开。

- `cb_show_hp_npc` 字符串：文件偏移 `0xd75098`，VA `0x10d75098`
- 唯一 `lea` 引用：文件偏移 `0x28ff4f`（VA `0x1028ff4f`）

### 2.2 但位数不匹配（关键）

用户的启动脚本 `单机启动.bat` 用的是 **`bin64\aion.bin`** —— 实际运行 **64 位**客户端：

```
start bin64\aion.bin -ip:... -port:2106 -cc:5 -lang:chs -vip -f2p ... -disable-xigncode ...
```

因此那条 **32 位**偏移 `0x3532C6` **不适用于用户当前客户端**（32/64 位代码布局无关，同一 RVA 指向完全不同的代码）。

### 2.3 32 位 DLL 无法静态验证

| 指标 | `bin32/game.dll` | `bin64/Game.dll` |
|---|---|---|
| 文件大小 | 24,930,168 | 29,795,000 |
| 架构 | PE32 i386（ImageBase `0x10000000`） | PE32+ x86-64（ImageBase `0x10000000`） |
| 节名 | 首节空名、`bdqkrpqp`、`cbyxtypf`、`.taggant` | 首节空名、`oxgvueao`、`grhvescf`、`.pdata`、`.taggant` |
| 首节特征 | `Ch=0xe0000040`（可写+可执行） | `Ch=0xe0000040` |
| **`c3`（ret）计数** | **0** | — |
| 函数序言计数 | `55 8b ec` = **16** | `48 89 5c 24` = **18527** |
| 字符串区 | 乱码（无 `Aion - `、无 `cb_*`） | 明文可读 |
| 结论 | **代码节加密／加壳，静态不可 patch** | **明文，可直接改文件**（VIP 补丁已验证） |

即：**32 位那条 patch 无法在当前 32 位文件上核对**（无论是文件偏移还是运行时偏移都无法静态确认），
而 64 位版本虽然可 patch，但需要重新定位对应逻辑，不能套用 32 位偏移。

### 2.4 这是「显示层」，与 `CV-001`（服务端同步层）是两回事

| 层 | 控制者 | 决定 |
|---|---|---|
| 显示层 | 客户端开关 `cb_show_hp_npc`（＋`cb_show_gauge_num`） | 血条**显不显示** |
| 数据层 | 服务端 `SM_ATTACK_STATUS` 的 0–100 百分比 | 显示的**值对不对** |

两层独立且互补：

- 开关关闭 → 客户端不画血量 → **服务端同步修复无从观察**（先开开关才有意义）；
- 开关打开但数值不刷新 → 才是 `CV-001` 覆盖的服务端同步问题（已修）。

因此**不能**用这条客户端开关解释「血条停在旧值」类症状（那说明血条当时是显示的），
两者应对应不同症状：**完全没有血量显示** vs **有血条但不更新**。

## 3. 后续验证与结论修正

### 3.1 实机确认：选项窗口里没有该项

用户实测反馈 UI 中无「显示 NPC 血量」。解包 `data/ui/game/game.pak` 的
`global_option_dialog.xml` 证实：`page_status`（状态页）只有

| 行 | 控件 | 文本 key | 中文 |
|---|---|---|---|
| 98 | `cb_show_hp_party` | `..._TABFRAME_OPTION_WIDGET14` | 显示队员状态栏 |
| 102 | `cb_show_hp_alliance` | `..._TABFRAME_OPTION_WIDGET15` | 显示部队成员状态栏 |
| 103 | `cb_show_hp_target` | `..._GAME_GAUGE_TARGET` | 显示所选对象的状态栏 |
| 104 | `cb_show_gauge_num` | `..._TABFRAME_OPTION_WIDGET32` | 显示状态栏数值 |
| 105 | `cb_show_hp_me` | combobox（AUTO/SHOW/HIDDEN） | 自动／总是显示／隐藏 |

**`cb_show_hp_npc` 不在其中** —— 控件被客户端从界面上摘掉了（代码里仍注册、成员槽
`[rdi+0x5d0]` 仍预留）。

### 3.2 文本表里两套译文都还在

| key | 韩文 | 中文（`L10N/CHS/Data/data.pak`） |
|---|---|---|
| `STR_GLOBAL_OPTION_DIALOG__GAME_GAUGE_NPC` | NPC 상태바 표시 | **显示NPC状态栏** |

即：**文本在、代码在、UI 定义缺** —— 补一个引用该 key 的 checkbox 即可恢复。

### 3.3 补丁（已生成）

`patch/data/ui/game/global_option_dialog.xml`（88,155 字节），在 `cb_show_gauge_num`
之后补入：

```xml
<Widget font="v3_option" frame="45,165,220,17" name="cb_show_hp_npc" preset="v5_check"
        style="checkbox" text="STR_GLOBAL_OPTION_DIALOG__GAME_GAUGE_NPC"
        text_offset="5,0" type="button" valign="middle" />
```

部署方式（散文件或重打包）与回滚见 `patch/patch_documentation.md`。

### 3.4 部署：散文件无效，回封 pak

- **散文件方式已实测无效**：客户端不从 `data/ui/game/` 读散文件（该目录优先 pak），用户实机确认。
- **重打包**：`patch/data/ui/game/game.pak`（381,718 字节，MD5 `0d2d812105aae3f855c571b953d93a9d`），
  以 `aion_pak.py pack unpacked -o game.pak --template <原 pak> --overwrite` 生成，再经
  `pak_to_aion_format.py` 做签名变换。回解后与原目录 `diff -rq` **零差异**、`cb_show_hp_npc` 在位。
- **已部署**到客户端 `data/ui/game/game.pak`；原始文件备份为同目录 `game.pak.orig-backup`（522,543 字节）。

### 3.5 `.pak` 容器格式（本次查清的坑）

**Aion 5.8 的 `.pak` 就是 zip，但三处 zip 签名被 XOR 0xFF**，其余字节（deflate 流、名称、时间戳）原样：

| zip 签名 | pak 内实际值 | 数量（game.pak） |
|---|---|---|
| 本地文件头 `50 4b 03 04` | `af b4 fc fb` | 369 |
| 中央目录 `50 4b 01 02` | `af b4 fd fe` | 369 |
| EOCD `50 4b 05 06` | `af b4 fa f9` | 1 |

因此 `python zipfile.open()` 读原始 pak 会抛 `BadZipFile`，而 `aion_pak.py list/unpack` 能读（它自己
处理该变换）——**但它的 `pack` 输出标准 zip，不做反向变换**，必须补
`.agents/summary/client-hp-display/pak_to_aion_format.py`（按结构遍历定位 739 处签名后 XOR，不靠字节搜索）。

**为什么回封后文件更小**（382KB vs 523KB）：pak 内原本是**二进制 XML**（如 `abyss_fort_info_dialog.xml`
5200 字节），`unpack` 默认解码成**文本 XML**（6945 字节），而 deflate 对文本的压缩率更高（1805 → 1315 字节）。
解压后总量反而更大（1,532,041 → 1,969,783）。两处差异都由「二进制 XML → 文本 XML」解释，与签名变换无关
（变换只改 739 × 4 字节，不改变文件大小）。

### 3.6 仍待实机确认

- **文本 XML 是否被客户端接受**：pak 内原为二进制 XML，回封时是文本 XML；工具 `pack` 无反向编码能力。
  `Levels/lf2a/Level.pak` 的回封（同为文本 XML）此前实测可用，故判断客户端两种都读，但仍需本次实机确认。
- **控件是否仍驱动显示逻辑**：代码注册表明期望仍在，但需勾选后实机确认 NPC 状态条真的出现。
- 确认后若血量不刷新 → 转 `CV-001` 服务端同步口径排查。

## 4. 涉及文件

- 客户端（不在仓库内）：`${AION_CLIENT_ROOT}/bin32/game.dll`、`bin64/Game.dll`、
  `单机启动.bat`、`SystemOptionGraphics.cfg`
- 仓库内既有客户端补丁：`patch/Game.dll`（64 位）、`patch/patch_documentation.md`、
  `.agents/summary/patch-game-dll-vip/patch_game_dll_vip.py`
- 既有客户端／服务端控制边界：`.agents/summary/weather-theobomos/server-control-boundary.md`

## 5. 勾选框是残留：DLL 里没有消费代码（2026-09-23 续查）

- 注册块只是**绑定**：`call [rax+0x340]` = `GetWidget(name, attr)`，整段无条件执行；XML 补上控件后
  绑定成功（勾选框出现、可勾），但不产生任何显示行为。
- 该类代码区（`0x1028f000`–`0x10293300`）全量反汇编：唯一读取 HP 组槽位的是 `0x10293e31` 起的
  **互斥逻辑**（`cb_show_hp_alliance` ⇒ 勾 `cb_show_hp_party`；`cb_show_name_alliance` ⇒ 勾 `cb_show_name_party`），
  **没有任何指令读 `[this+0x5d8]`**；同类控件 `cb_cooltime`(0x600)、`cb_abnormal_time`(0x5f8) 也**同样无读取**——
  说明「读勾选状态」走的是框架级机制，不在这个类里。
- 全文件扫描 `[reg+0x5d8]` 读点：命中的 `0x101a5e6c`/`0x101a5e98` 属另一个 GM 编辑对话框
  （同偏移不同类，同时期控件名是 `cb_fortress_list`/`cb_level_list`/`cb_indun_list`），与本选项无关。
- `bin32/game.dll` 全节加密（抽样 8KB 内 `c3`=0、无函数序言），`gamebase + 0x3532C6` 无法静态核对；
  客户端 `bin64/Game.dll` 与其 `Game.dll.bak` **逐字节相同**（未被任何补丁改动过）。

## 6. 实机反馈（用户实测）

| 测试 | 结果 | 结论 |
|---|---|---|
| A 勾选后重开选项窗口 | 勾还在 | 会话内控件状态保持（对话对象未销毁） |
| A' 选择角色后再看 | 勾消失 | 无持久化通道——该控件不在保存/载入名单里 |
| B 「显示队员状态栏」勾/取消 | 队友血条有反应 | 该页选项通道对「队伍」是活的 |
| C 选中怪 | 目标窗口有血条、无数值 | 客户端拿得到 NPC 血量（`SM_NPC_INFO` 带 0–100 百分比），目标窗口渲染正常 |

## 7. 真开关：客户端数据 `npc_ui` / `hpgauge_level`

- 客户端有 **NPC 显示档位表** `data/Npcs/npcs.pak → npc_ui.xml`（45 个档位），每个档位规定
  「名字 / 血条 / 雷达 / 悬停 / 点击」各显示什么；每条怪物数据的 `<hpgauge_level>` 就是档位 id。
- 战斗怪默认档位 `3 monster`，其 **`ui_hpgauge=0`**；档位 `45 show_hpgauge_monster`
  （注释原文：*HP gauge bar가 항상 보이는 전투 NPC용* = HP 条常显的战斗 NPC 用）与档位 3
  **逐字段 diff 只差 `ui_hpgauge`（0→1）**。
- 这份客户端数据里**没有任何一条怪使用档位 45** → 世界血条从不绘制，与 UI 勾选框无关。
  （`npcs.pak` 里的 `client_npcs_*.xml` 为 Aion 二进制 XML；`npc_ui.xml` 里的 `over_hpgauge=1`
  表示「悬停显示血条」，档位 3/45 都带，可作为「渲染路径是否存在」的判别测试。）

### 8. 第一次补丁失败：文本 XML 让客户端崩溃（2026-09-23）

第一版做法：`aion_pak.py unpack` 解包 → 把 19,170 条怪的 `hpgauge_level` 改成 `45` → 用
`pack_pak_aion_format.py` 按原生容器格式回封。**客户端替换后一进游戏即崩溃。**

根因（高置信）：`unpack` 把 pak 内的 **Aion 二进制 XML** 解码成**文本 XML** 输出，工具链**没有反向编码器**
（`aionpak.binary_xml` 只有 `read_binary_xml` / `binary_xml_to_utf8`），回封时写进去的就是文本 XML；
`npc_ui.xml` 等条目在客户端里走的是**二进制 XML 装载器**，编码不符 → 装载期崩溃。
（旁证：`unpack` 对原始 pak 报 `decoded=15`，对回封包报 `decoded=0`。）

**由此确立的硬约束：改 `npcs.pak` 必须保持二进制 XML 编码。**

### 9. 二进制 XML 格式细节（写编码器时踩到的两个坑）

1. **字符串表索引 = 字节偏移 ÷ 2，不是序号。** 解码器 `_StringTable.get` 用 `start = index*2` 直接寻址，
   说明原写入者存的是「该串在表中的字节偏移 / 2」（索引因此是稀疏的：串 "npc_uis" 占 2..17，下一个串
   索引就是 9 而非 2）。按「第 i 个串」编码会让**所有节点名整体错位**，且解码器不报错——只会静默读出
   错误的名字（本例把 `<npc_ui>` 读成 `<pc_uis>`）。
2. **节点的值本身就是字符串表索引**，所以「把 `0` 改成 `1`」= 换一个索引字节，**文件长度不变**。

### 10. 补丁（第二版，二进制保持）与封装

| 项 | 内容 |
|---|---|
| 改动 | `npc_ui.xml` 档位 `3 monster` 的 `ui_hpgauge`：`0` → `1`（44 个档位与全部 `client_npcs_*.xml` 未动） |
| 影响面 | 档位 3 被 **38,558 条** NPC 使用（monster 19,170 + `client_npcs_npc.xml` 19,388）→ **所有 NPC** 常显血条 |
| 产物 | `patch/data/Npcs/npcs.pak`（3,984,799 字节，MD5 `a6d4a2cd50e41a5ae2425e59f1fe79e6`） |
| 回滚 | 原始 `npcs.pak`（3,984,787 字节，MD5 `2d296a9ba9a9aca5ba1d66f250cc17bd`） |
| 工具 | `.agents/summary/client-hp-display/patch_binary_xml_entry.py`（单条目：解出目标条目 → 二进制内改一个索引 → 其余条目连压缩流逐字节复制；内含与解码器配对的编码器） |
| 校验 | 17 条目中仅 `npc_ui.xml` 变化；该条目解压后 8,178 字节**只有 1 字节不同**（`0x1093`：`0x3d → 0x13`）；容器本地头链与中央目录条目数均为 17；编码器对 16 个 XML 条目全部结构一致 |
| 状态 | **待客户端实机确认** |

副作用评估：档位 3 与档位 45 除 `ui_hpgauge` 外 20 个字段全同（名字、雷达、悬停、点击、目标窗口均不变），
故本改动只增加「世界里常显血条」。未纳入常显的战斗档位：`8`(命名怪)/`18`/`23`(raid)/`24`(从属怪)/
`28`/`36`/`41`——它们各有自己的 `ui_hpgauge` 取值，需要时按同一手法放进来。

### 11. 关于朋友给的 4.6 线索

「4.6 版 32 位 `game.dll` 跳到 `gamebase + 0x3532CD` 数字就会显示」：

- 用户实际跑的是 **64 位** `bin64/aion.bin`（见 §2.2），32 位偏移不能套用；
- 32 位的 `bin32/game.dll` **代码节加密**（§5：抽样区 `c3` 计数为 0、无函数序言），偏移**无法静态核对**；
- 该线索说的是「**数字**」（血量数值），对应的是选项页里的 `cb_show_gauge_num`（「显示状态栏数值」），
  与「血条是否显示」是两条不同的路径；64 位 DLL 里同样**没有任何指令读该槽位**（§5），
  说明 5.8 这两项都已数据/框架化，静态找「读槽位」的补丁点不会有收获。
- 结论：这条线索**不适用于当前客户端**；可用的杠杆是服务端数据 + 客户端数据（本文件 §7–§10）。

### 12. 用户实机反馈与「数字」路径定位（2026-09-23 续）

用户实测三点（关键，推翻了此前一部分假设）：

1. **世界血条本来就有**（不选中怪也看得到头顶血条）→ 档位 45/`ui_hpgauge` **不是**「有没有血条」的开关，
   本文件 §7–§10 的 npcs.pak 补丁对用户目标无效（可撤）。
2. 「显示状态栏数值」选项**有效，但只作用于组队数值**（我方观察到的 `cb_show_gauge_num` 活跃路径）。
3. 用户真正想要的：**怪物头顶血条上显示数值**（数字），与「4.6 那条分支让数字显示」的线索一致。

**「数字」的决定位置（bin64/Game.dll，明文可 patch）**：

- 头顶铭牌由**代码构造**，不是数据：字符串 `has_gauge` / `naturally` 在整个客户端（3421 个文件，除音频/模型）
  **只出现在 `bin64/Game.dll`**（含 `Game.dll.bak`）；所有 `.pak`、散文件、配置里都没有。
- 铭牌行的「显示模式」由代码解析为整数：把行上的属性名依次与
  `none` / **`number`** / `text` / `naturally` / `param` 匹配，得到 `-1 / 1 / 2 / 3 / 4`
  （4 份同构解析器：文件偏移 `0x93cf7a`、`0x94051f`、`0x9407ec`、`0x941f83` 附近；
  其中 `number` → `ebx/r14d/esi = 1`，`naturally` 全文件只被引用一次）。
- 解析结果与 HP 比例（`double`，由行的 `width` 属性换算）、`has_gauge` 布尔一起传给
  **绘制函数 `0x1092b220`（文件偏移 `0x92b220`）**；即 `mode=1` 才是「画数字」。
- 该函数不做数值格式化以外的判断，数字文本来自行自身（`title`/`width`/`font` 属性）。
- 属性表（文件偏移 `0xfe9f28` 起，23 条 0x20 字节记录）给出档位字段的位标志：
  `ui_hpgauge` = 字 `0x08` 的第 3 位、`over_hpgauge` = 字 `0x10` 的第 3 位、
  `ui_title` = 字 `0x08` 的第 0 位（`over_hpgauge=1` 才是「头顶有血条」，与用户实测一致）。

**结论**：数字是**客户端二进制开关**（`Game.dll` 内的模式判定），
数据（npcs.pak / data.pak / Level.pak）、服务端（`Npc.getHpGauge()` 无调用方、
`SM_NPC_INFO` 不发档位）都改不出来 —— 与朋友那条 4.6 线索同族，只是地址与版本不同。
下一步只能是 Game.dll 补丁（64 位明文，已有 `patch/Game.dll` VIP 补丁先例与脚本），
风险与回滚见 `patch/patch_documentation.md` 的 Game.dll 一节。

### 13. 落地补丁（2026-09-23，待实机确认）

用户授权后生成的 64 位 `Game.dll` 补丁：把「头顶铭牌」那份解析器里的三处模式判定各改 1 字节
（详见 `patch/patch_documentation.md` 的 «怪物头顶血条显示数字 Game.dll»）：

| 文件偏移 | 原 → 改 | 含义 |
|---|---|---|
| `0x93ded4` | `bb 02 00 00 00` → `bb 01 00 00 00` | 默认模式 2 → 1（带数字） |
| `0x93e00a` | `8d 58 03` → `8d 58 01` | `naturally` 分支 3 → 1 |
| `0x93e021` | `b8 04 00 00 00` → `b8 01 00 00 00` | `param` 分支 4 → 1 |

- 作用域：仅 VA `0x1093de30`–`0x1093e0c0`（`has_gauge` 全 DLL 只被这一份引用）；
  HUD/队伍那份（`0x1093cf7a` 附近）未动，故「显示状态栏数值」的组队行为不受影响。
- 产物 `patch/Game.hpnum.dll`（29,795,000 字节，MD5 `1ed1a8f7d3fb9de2376285327eb31c42`），
  与原版逐字节仅差 3 处；原版 MD5 `f77e0b4729842929d6ae8bb8ec128b6d`（== 客户端 `bin64/Game.dll.bak`）。
- 脚本 `.agents/summary/client-hp-display/patch_game_dll_hpnum.py`（可 `--verify`、可逐点关闭、
  可作用于已打其它补丁的 DLL）。
- 回滚：`bin64/Game.dll.bak` 覆盖回 `Game.dll`。
- 状态：**待客户端实机确认**（观察点：怪物头顶/世界血条旁是否出现数值；若出现异常文本或无效，
  用 `--no-default/--no-naturally/--no-param` 逐点排查）。

**合并版（2026-09-23）**：`patch/Game.vip-hpnum.dll`（29,795,000 字节，MD5 `3579a12abbde160cd7e8efb9a3bf9b23`）
= VIP 版 `patch/Game.dll` + 同样这 3 处改动，由 `patch_game_dll_hpnum.py --source patch/Game.dll` 产出。
逐字节关系（实测）：原版↔VIP 497 字节、原版↔hpnum 3 字节、原版↔vip-hpnum 500 字节、
VIP↔vip-hpnum 3 字节、hpnum↔vip-hpnum 497 字节 —— 两份补丁互不重叠，可共存。

**v2（2026-09-23 追加）**：v1 三处实机无可见效果 → 追加兄弟函数 A'（`0x1093dd20`，清空并重建行表、
每行都带 `has_gauge` 实参）里两处**常量模式 2** 的立即数（文件偏移 `0x93dd2b`、`0x93dd5c`）改为 1，共 5 处。
产物：`patch/Game.hpnum.dll`（MD5 `61cbeaa866e70c59dcb690e209748dd7`，与原版差 5 字节）、
`patch/Game.vip-hpnum.dll`（MD5 `3bcc6c08668cd0860dd3b26cdb0e946d`，与 VIP 版差 5 字节）。
同时查明：三个「行构造」函数中 B=`0x102b59ba`、C=`0x108fc4e4` 是列表控件（`v5_listview`/`v5_listview_sel`），
与铭牌无关；A/A' 的调用方是**间接调用**（函数指针表），静态无法上溯。

### 14. 血条数字的完整机理与「世界血条无数字通道」的判定（2026-09-23 续）

#### 14.1 UI 控件属性 → 控件字段的映射（属性应用器 `0x1093c3xx`–`0x1093d1a1`）

| 控件字段 | UI 属性 | 取值 → 整数 |
|---|---|---|
| `+0x8d4` | **`num_type`** | `default`→0、**`small`→1**、**`micro`→2** |
| `+0x364` | **`value_type`** | `number`→0、`float`→1、`percent`→2、`percent_float`→3、`curNumber`→4 |
| `+0x358` | `align` | `left/top`→1、`right`→2、`bottom`→3、`circle`→4 |
| `+0x3cc` | `text_halign` | `center`→1、`left`→2、`right`→3 |
| `+0x3a4` / `+0x3ac` | 布尔属性 | `true`→1 / `false`→0 |

#### 14.2 数字是怎么画出来的

控件更新函数 **`0x108e125d`**（gauge 控件类的虚函数，虚表在 `0x152d800` 附近）：

```
0x108e13e4  cmp  [rbx+0x35c], 0        ; 值为 0 → 跳过
0x108e13ed  comisd [rbx+0x368], xmm7   ; 不大于当前比例 → 跳过
0x108e141c  mov  eax, [rbx+0x8d4]      ; ← num_type
0x108e143d  add  eax, 2
0x108e1465  call 0x1097d290            ; 数字文本函数（type = num_type+2）
```

数字文本函数 `0x1097d290` 全 DLL **只有 7 个调用点**：`0x101d0400`、`0x102ebbbd`、
`0x108e125d`、`0x1091b6a0`、`0x1091b800`、`0x1091d000`、`0x10969a90`。

#### 14.3 结论：世界血条（头顶）没有数字通道

1. **数据侧**：`num_type` / `value_type` / `has_gauge` / `naturally` 在整个客户端（2477 个 pak
   解压后 + 散文件）只出现在 `data/ui/game/game.pak` 的窗口类 UI 里（party / force / mercenary /
   spectator / target_delay 等），**没有任何一处属于头顶世界血条**；`npc_ui.xml` 的 45 个档位
   （25 个字段）全是显示开关，没有数字字段。
2. **代码侧**：世界血条的绘制路径为 `0x108c37df`（按 NPC id 遍历）→ `0x108c3ed0`
   （`over_hpgauge` 位判定在 `0x108c3a3a`，**全 DLL 唯一**）→ `0x108c2250`（三段特性位
   `r15d = [rcx+0x24] & [rcx]`：bar/次要/再次要）。该路径**从不调用** `0x1097d290`。
3. 因此「怪物头顶世界血条上的数字」在本客户端**不存在可开关的实现**：既无数据定义，也无
   数字渲染调用。4.6 的 `gamebase+0x3532CD` 那条线索在本版本对不上（32 位代码节加密，亦无法核对）。

#### 14.4 据此产生的两个补丁

- **v3（Game.dll）**：把 `0x108e141c` 的 `mov eax,[rbx+0x8d4]` 强制为 `mov eax,1`（small），
  并 NOP 掉 `0x108e13eb` / `0x108e13f9` 两处前置判定 → 所有走该控件更新路径的 gauge 都画数字。
  产物 `patch/Game.hpnum.dll`（MD5 `94daba7832635d8290eee81952f0f889`）、
  `patch/Game.vip-hpnum.dll`（MD5 `71a146481980f284e1144967037e3ab1`）。
- **data.pak**：给 `target_dialog`(5) 与 `target_assist_dialog`(1) 的 `hp_gauge` 加
  `num_type="small"`（机制与组队血条完全相同）→ 目标窗口血条出数字。
  产物 `patch/L10N/CHS/Data/data.pak`（MD5 `0aac276758d88646abea7aed3a122f9f`，
  仅 `UI/ui_game_override.xml` 变化）。
- v1/v2 的五处（`0x93ded4`/`0x93e00a`/`0x93e021`/`0x93dd2b`/`0x93dd5c`）经查明属于
  **`column_set` 列表单元格渲染器**（`title,width,align,<显示模式>,font,key` 的第 4 字段，
  取值 none/number/text/naturally/param，渲染函数集在 `0xf74cf0`/`0x115326e0` 两张表里），
  与头顶铭牌无关，实测无可见效果 → 脚本中降级为 `--with-cell_*`（默认关闭）。

### 15. data.pak 实机崩溃与「基准必须是客户端现用文件」纪律（2026-09-23）

**现象**：用户安装 `patch/L10N/CHS/Data/data.pak`（含目标窗口数字改动）后，客户端崩溃；
同轮安装 `patch/Game.vip-hpnum.dll` 正常。

**取证**（三份 pak 条目级对比）：

| 文件 | 大小 | MD5 | 与「客户端现用」差异条目 |
|---|---|---|---|
| 客户端原始备份 `data.pak.orig-backup` | 94,592,478 | `dd0aa4c92c54b59b3f5daa5c0ec2999d` | 1（仅 `UI/game/global_option_dialog.xml`） |
| **客户端现用** `data.pak` | 94,240,820 | `b47db70ce6c181f233d1e76f102b39d7` | — |
| 仓库版 `patch/.../data.pak`（上一轮发出） | 94,239,692 | `0aac276758d88646abea7aed3a122f9f` | **60** |

- 那 60 个差异条目：55 × `Dialogs/**/quest_q*.html`、2 × `Strings/*`、2 × `UI/*`、1 × `npcs/*`，
  即仓库里那套**任务对话本地化**改动 —— 它从未在该客户端验证过；整包替换 = 一次性引入 60 处
  变化，客户端崩溃。
- **容器不是原因**：客户端现用文件的 zip 结构（`extra` 字段 0 个、`method` 分布 180/23091、
  `flag_bits` 全 0）与工具输出完全同型 —— 该文件本身就是同一套 `patch_pak_entry.py` 产出的，
  故容器格式已知可用。
- 已按字节还原仓库文件（`data.pak.with-num` 把 `UI/ui_game_override.xml` 换回未改版本 →
  MD5 复现 `2d7de00e8c6d5ccce50f4a378723e4a9`）。

**纪律（新增，写入模式卡 `CPK-001`）**：任何 pak 交付物必须

1. 以**客户端现用文件**为基准，而不是仓库里上一版补丁产物；
2. 做**单条目**替换，并在出货前用 `zipfile` 逐条目比对，确认**差异条目数 == 1**；
3. 若基准无法取得，先记录「客户端现用文件」的大小与 MD5 作为交付物的 `parent`。

**本轮产物**（最小差异包）：`patch/L10N/CHS/Data/data.numbers.pak`
（94,240,838 字节，MD5 `b3b8512b398ba044897b7af45eb66cf5`；基准 = 客户端现用文件，
差异条目 = 1 × `UI/ui_game_override.xml`）。部署时重命名为 `data.pak`。

### 16. 实机结果（2026-09-23）：v3 让**窗口内**血条出数字；头顶世界血条确认无此通道

用户实测（VIP 版 + v3 三处）：

| 观察 | 结论 |
|---|---|
| `patch/Game.vip-hpnum.dll` 安装后客户端正常，**窗口内的血条出现数字**（目标窗口/组队/基础状态栏等 UI 控件） | v3 的三处补丁**有效**——`0x108e125d` 就是 UI 控件画数字的路径，`num_type` 强制成 `small` + 去掉两处前置判定即可让窗口血条出数字 |
| **怪物头顶世界血条仍然没有数字** | 头顶血条不经过 UI 控件的 Draw/Update：它的绘制是 `0x108c37df`（按 NPC id 遍历）→ `0x108c3ed0` → `0x108c2250` 手动完成的（用 `[obj+0x24] = 7` 特性位 × `[obj]` 掩码选三个子控件，逐个 `SetRect/SetPos` 后绘制），**该路径从不调用数字文本函数 `0x1097d290`** |
| 附带确认 | 头顶血条的子控件确实是**同一个 gauge 控件类**（虚表在 `0x152d800` 附近：`[+0x110]`=`0x108e2d2b`、`[+0x1b0]`=`0x108e3ae2`、`[+0x260]`=`0x108e4120`、`[+0x2a8]`=`0x108e4660` 全在该类方法区），但它被**手动绘制**而不是被 UI 框架绘制，所以类里的数字逻辑（`0x108e125d`）对它不生效 |

**结论**：在本 5.8 客户端，「头顶血条上的数字」需要**注入代码**（在世界血条绘制路径里额外调用数字文本函数）才能实现，不是改开关。
可行的两条路：
1. 向社区/朋友索取 5.8 **64 位**现成补丁 —— 拿到后做字节对比即可立刻定位（最快）；
2. 用 x64dbg 实机跟到 `Game.dll+8C2250`，取三个子控件的虚表指针与构造点，判断能否在构造处补上 `num_type`/数字控件；再决定是否写代码注入补丁。

### 17. 交付物定稿与 `patch/` 精简（2026-09-23，用户要求「只留最终的」）

本文件 §13–§16 记录的是当时的中间产物（各版本 DLL、各版 pak），**均已被精简删除**；
最终形态与命名以 `patch/patch_documentation.md` 为准：

| 项 | 精简后 | 说明 |
|---|---|---|
| 唯一二进制补丁 | `patch/Game.dll`（29,795,000 字节，MD5 `71a146481980f284e1144967037e3ab1`） | ＝ 原 §16 的 `Game.vip-hpnum.dll` 内容改名为规范名；原版 + VIP + v3 三处，与原版差 502 处 |
| 已删除 | `Game.hpnum.dll`、`Game.vip-hpnum.dll`、`L10N/CHS/Data/data.numbers.pak`、`data/Npcs/npcs.pak`、`data/ui/game/game.pak` | 重建配方逐条保留在 `patch/patch_documentation.md`「已删除的补丁」 |
| 保留不动 | `Levels/lf2a/Level.pak`（天空修复）、`L10N/CHS/Data/data.pak`（⛔ 暂不可部署） | 与本轮数字工作无关 / 仍标 ⛔ |

本文件的其它编号（§8–§10 的 npcs.pak、§13 的 v1/v2、§15 的 data.pak 崩溃取证）保留原样，
作为**过程证据**与「按偏移猜补丁点」「以错误文件为基准」两次教训的原始记录。

**17.1 追加：data.pak 真正回滚（用户指出「没有回滚」后）**

§15 记录的那次「还原」（→ 94,239,674 / `2d7de00e…`）只撤掉了自己加的「数字」那一处，
**仓库文件仍含 58 处未在本客户端验证的任务对话改动**——用户比对后指出未回滚，属实。已按提交版本回滚：

```bash
git checkout -- patch/L10N/CHS/Data/data.pak   # 95,915,168 字节 / MD5 9e6247830a11876b2668cfe867f4d667
```

回滚后条目级事实（条目数均为 23,271）：

| 对比 | 差异条目 |
|---|---|
| 回滚后 vs 客户端原版 `data.pak.orig-backup`（94,592,478 / `dd0aa4c9…`） | **58** = 55 × `Dialogs/*`（46 个 `quest_q*.html` + 9 个 `QUEST_Q*.html`）、2 × `Strings/*`、1 × `npcs/npc_mesh_replace.txt` |
| 回滚后 vs 客户端现用（94,240,820 / `b47db70c…`，= 原版 + 勾选框） | 59 = 上述 58 + `UI/game/global_option_dialog.xml`（勾选框只存在于客户端现用文件里） |
| 客户端现用 vs 客户端原版 | 1（仅勾选框） |

`UI/ui_game_override.xml`（数字那处）在 提交版 / 磁盘版 / 客户端现用 三份中**逐字节相同**，
全库 `num_type="small"` 命中 0 → 数字改动确实已撤净。

**结论不变**：回滚后该文件**仍不可部署**（58 处未验证改动在包内），
`patch/patch_documentation.md` 也已同步为「已回滚为 git 提交版本 + 仍 ⛔」。
