# 客户端补丁文档 / Client Patch Documentation

`patch/` 只保留三个交付物（2026-09-23 定稿）。改动 `.pak` 前先读模式卡 `CPK-001`
（单条目替换 + 以客户端现用文件为基准，出货前核对差异条目数 == 1）。

## 交付物

| 文件 | 大小 | MD5 | 部署 / 回滚 |
|---|---|---|---|
| `Game.dll` | 29,795,000 | `71a146481980f284e1144967037e3ab1` | 覆盖客户端 `bin64/Game.dll` 后**完全重启**；回滚 = 同目录 `Game.dll.bak` 覆盖回去 |
| `Levels/lf2a/Level.pak` | 9,809,347 | `f72b44b54f0989b9235519a4f3d21571` | 按目录结构覆盖客户端根目录后**完全重启**；回滚 = 原版 `Level.pak`（`fb49f0c3f1fce43d798b453e1def6dcf`） |
| `L10N/CHS/Data/data.pak` | 95,915,168 | `9e6247830a11876b2668cfe867f4d667` | ⛔ **不可部署**，见该节 |

## Game.dll：VIP + 血条数字（已实机验收）

**效果**：窗口内血条（目标窗口 / 组队 / 基础状态栏）显示数字，客户端稳定。
**怪物头顶世界血条仍无数字**——它由 NPC 显示系统手动绘制（`0x108c3ed0` → `0x108c2250`），
从不调用数字文本函数 `0x1097d290`，需代码注入；完整机理见证据文档 §14–§16。

**原理**：gauge 控件画不画数字由字段 `+0x8d4`（UI 属性 `num_type`：default/small/micro）决定，
控件更新函数 `0x108e125d` 读它后调 `0x1097d290`。补丁把该读取强制成 `1`（small）并去掉两处前置判定：

| 文件偏移 | 原 → 改 | 含义 |
|---|---|---|
| `0x8e141c` | `8b 83 d4 08 00 00` → `b8 01 00 00 00 90` | `mov eax,[rbx+0x8d4]` → `mov eax,1` |
| `0x8e13eb` | `74 7d` → `90 90` | 去掉「该字段为 0 就不画数字」 |
| `0x8e13f9` | `76 6f` → `90 90` | 去掉「未超过当前比例才画数字」 |

- 与原版（`f77e0b4729842929d6ae8bb8ec128b6d` = 客户端 `bin64/Game.dll.bak`）逐字节差 502 处
  （VIP 497 + 数字 3 + 指令等长扩展 2）
- 重建（按顺序，第 1 步需原始 STS 认证版 DLL）：
  1. `python3 .agents/summary/patch-game-dll-vip/patch_game_dll_vip.py --source <原版> --out Game.vip.dll --sts-ip 127.0.0.1`
  2. `python3 .agents/summary/client-hp-display/patch_game_dll_hpnum.py --source Game.vip.dll --out Game.dll`
     （`--verify` 只校验；`--no-gauge_num` / `--no-gauge_cond1` / `--no-gauge_cond2` 逐点关闭）

## Levels/lf2a/Level.pak：泰奥勃莫斯天空修复

修复进入泰奥勃莫斯（210060000）后天空变红云/沙尘、地面偏黄昏（改写 `mission_mission0.xml` 中的
WeatherOption 与入侵/世界突袭 cutscene TimeEnv）。排查记录：`.agents/summary/weather-theobomos/`。

```bash
python3 aion_pak.py unpack "<客户端>/Levels/lf2a/Level.pak" -o unpacked --overwrite --no-progress
python3 aion_pak.py pack unpacked -o Level.pak --template "<原 Level.pak>" --overwrite
```

## L10N/CHS/Data/data.pak：⛔ 不可部署

内容 = 任务对话字典变量修复 + 清空 `npcs/npc_mesh_replace.txt`（禁用 NPC 模型替换）。
它与客户端原版差 **58 个条目**（55 个 `Dialogs/*`、2 个 `Strings/*`、1 个 `npcs/npc_mesh_replace.txt`），
这些改动**从未在本客户端验证过**，整包替换会让客户端崩溃 → 只能按 `CPK-001` 逐条目二分后再启用。

## English

`patch/` holds three deliverables (final, 2026-09-23); read pattern card `CPK-001` before editing any `.pak`
(single-entry replacement, baseline the client's *current* file, diff must be exactly 1 entry).

| File | Size | MD5 | Deploy / Rollback |
|---|---|---|---|
| `Game.dll` | 29,795,000 | `71a146481980f284e1144967037e3ab1` | Overwrite the client's `bin64/Game.dll`, then **fully restart**; roll back with the sibling `Game.dll.bak` |
| `Levels/lf2a/Level.pak` | 9,809,347 | `f72b44b54f0989b9235519a4f3d21571` | Overwrite the same path under the client root, then **fully restart**; roll back with the original `Level.pak` (`fb49f0c3f1fce43d798b453e1def6dcf`) |
| `L10N/CHS/Data/data.pak` | 95,915,168 | `9e6247830a11876b2668cfe867f4d667` | ⛔ **do not deploy** |

**`Game.dll` — VIP + gauge numbers, verified in client.** Window gauges (target window / party / basic
status) show numbers; the over-head world bar still shows none — it is drawn manually by the NPC display
system (`0x108c3ed0` → `0x108c2250`) and never calls the number-text routine `0x1097d290`, so it needs
code injection. The patch forces the `num_type` read (`+0x8d4`, read at `0x8e141c`) to `1` (small) and NOPs
two guards at `0x8e13eb` / `0x8e13f9` inside the widget update function `0x108e125d`; 502 bytes differ from
the original. Rebuild: `patch_game_dll_vip.py` then `patch_game_dll_hpnum.py` (both under `.agents/summary/`).

**`Levels/lf2a/Level.pak`** fixes the red/sand sky and dusk-tinted ground in Theobomos (210060000) via the
`WeatherOption` entries and the invasion/world-raid cutscene `TimeEnv` in `mission_mission0.xml`;
notes in `.agents/summary/weather-theobomos/`.

**`L10N/CHS/Data/data.pak` — ⛔ do not deploy.** Quest-dialog variable fixes plus clearing
`npcs/npc_mesh_replace.txt`; it differs from the client's pristine original in **58 entries**
(55 `Dialogs/*`, 2 `Strings/*`, 1 `npcs/*`) that were never verified in this client, so a whole-file swap
crashes it — enable it only through a `CPK-001` entry-by-entry bisect.
