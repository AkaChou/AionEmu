# 客户端补丁文档 / Client Patch Documentation

## 中文

### 任务本地化 data.pak

`L10N/CHS/Data/data.pak` 修复中文客户端任务对话中的字典变量显示问题，并清空 `npcs/npc_mesh_replace.txt`，禁用 NPC 模型替换。部署时保持目录结构，替换前备份客户端原文件。

### VIP Game.dll

`patch_game_dll_vip.py` 可使用原始 STS 认证版 `Game.dll` 生成 VIP 版本。生成的 `Game.dll` 放入客户端 `bin64` 目录。

> **注意：** 使用此补丁可能导致客户端崩溃，请谨慎使用。

```bash
python3 .agents/summary/patch-game-dll-vip/patch_game_dll_vip.py \
  --source /path/to/Game.dll.sts-auth-original \
  --out /path/to/Game.vip-world.dll \
  --sts-ip 127.0.0.1
```

### 泰奥勃莫斯天空修复 Level.pak

`Levels/lf2a/Level.pak` 修复泰奥勃莫斯（210060000）进入地图后天空变红云/沙尘、地面偏黄昏的问题（改写 `mission_mission0.xml` 中的 WeatherOption 与入侵/世界突袭 cutscene TimeEnv）。保持目录结构覆盖客户端根目录，并**完全重启客户端进程**后生效。

- 校验：9,809,347 字节，MD5 `f72b44b54f0989b9235519a4f3d21571`
- 回滚：用原始 `Level.pak`（MD5 `fb49f0c3f1fce43d798b453e1def6dcf`）覆盖后重启
- 排查记录：`.agents/summary/weather-theobomos/`（`diagnosis-sandrain.md`、`server-control-boundary.md`）
- 维护（unpak 工具，位于 `/Users/mc/PycharmProjects/unpak`）：

```bash
python3 aion_pak.py unpack "<客户端>/Levels/lf2a/Level.pak" -o unpacked --overwrite --no-progress
python3 aion_pak.py pack unpacked -o Level.pak --template "<原 Level.pak>" --overwrite
```

## English

### Quest localization data.pak

`L10N/CHS/Data/data.pak` fixes dictionary-variable rendering in Chinese quest dialogs and clears `npcs/npc_mesh_replace.txt` to disable NPC mesh replacements. Keep the patch directory structure and back up the original client file before replacing it.

### VIP Game.dll

`patch_game_dll_vip.py` generates a VIP `Game.dll` from the original STS-authenticated client DLL. Place the generated file in the client's `bin64` directory.

> **Warning:** This patch may cause the client to crash. Use it with caution.

```bash
python3 .agents/summary/patch-game-dll-vip/patch_game_dll_vip.py \
  --source /path/to/Game.dll.sts-auth-original \
  --out /path/to/Game.vip-world.dll \
  --sts-ip 127.0.0.1
```

### Theobomos sky fix Level.pak

`Levels/lf2a/Level.pak` fixes the red/sand sky (and dusk-tinted ground) seen after zoning into Theobomos (210060000); it rewrites the `WeatherOption` entries and the invasion/world-raid cutscene `TimeEnv` in `mission_mission0.xml`. Keep the directory structure, overwrite the file under the client root, and **fully restart the client process**.

- Verify: 9,809,347 bytes, MD5 `f72b44b54f0989b9235519a4f3d21571`
- Rollback: restore the original `Level.pak` (MD5 `fb49f0c3f1fce43d798b453e1def6dcf`) and restart
- Investigation notes: `.agents/summary/weather-theobomos/` (`diagnosis-sandrain.md`, `server-control-boundary.md`)
