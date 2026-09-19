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
