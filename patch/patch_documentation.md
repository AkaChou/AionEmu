# 客户端补丁文档 / Client Patch Documentation

## 中文

### 任务本地化 data.pak

`L10N/CHS/Data/data.pak` 修复 Aion 5.8 中文客户端任务对话中的字典变量显示问题。部署时应保持补丁目录结构，使文件位于客户端的 `L10N/CHS/Data/data.pak`；替换前请备份客户端原文件。

修复内容：

- 为 3 个缺少显示名称分隔符的任务字典词条补全名称。其中任务 1153 使用的 `STR_DIC_N_Hianu` 从缺少名称的说明文本修复为 `西亚诺;说明文本`。
- 修复 54 类不存在、大小写不匹配或拼写错误的任务 `%dic` 引用。
- 共修改包内 57 个文件，包括 2 个字典 XML 和 55 个任务 HTML；其余 23,075 个文件内容与原包一致。
- 字典 XML 保持原始 UTF-16 格式，任务 HTML 保持客户端使用的 `0x81` 加密格式。

兼容性与校验：

- 原始 `data.pak` SHA-256：`ac54a2ee01aef730500513c05755e32d8c2d4ee2eb9443ecb323a47717f99553`。
- 补丁 `data.pak` SHA-256：`534c565a40e1d75ffec358d4c55061277e5a8114d8f0c44a3b9ec8ad593bd7e3`。
- 已验证 9,116 个任务对话和 50,253 个字典引用，并通过完整压缩包测试及二次解包一致性检查。

### VIP Game.dll

`patch_game_dll_vip.py` 用于生成适配 Aion 5.8 的 VIP `Game.dll`。脚本不会修改源文件，输入和输出路径均由命令行指定。

> **注意：** 使用此补丁可能导致客户端崩溃，请谨慎使用。
>
> **安装提醒：** 补丁文件必须命名为 `Game.dll`，并放在 Aion 5.8 客户端的 `bin64` 目录下。

```bash
python3 scripts/patch_game_dll_vip.py \
  --source /path/to/Game.dll.sts-auth-original \
  --out /path/to/Game.vip-world.dll \
  --sts-ip 127.0.0.1
```

- `--source`：原始 `Game.dll` 文件路径，文件的 SHA-256 必须为 `f928a5dbc3d4d54e71f1968f38a75745882ef29f459d81b7b0fe50ac7cf490ad`。
- `--out`：生成文件路径；父目录不存在时会自动创建。
- `--sts-ip`：STS 服务的 IPv4 地址，默认值为 `127.0.0.1`。

路径可以是绝对路径，也可以是相对于当前工作目录的相对路径。成功后脚本会打印输入路径、输出路径和生成文件的 SHA-256。

## English

### Quest localization data.pak

`L10N/CHS/Data/data.pak` fixes dictionary-variable rendering problems in Aion 5.8 Chinese quest dialogs. Keep the patch directory structure when deploying so the file is installed as `L10N/CHS/Data/data.pak` in the client. Back up the original client file before replacing it.

Fix scope:

- Restores display names for three quest dictionary entries that were missing the name separator. For example, quest 1153's `STR_DIC_N_Hianu` entry now begins with `西亚诺;`.
- Corrects 54 classes of missing, case-mismatched, or misspelled quest `%dic` references.
- Changes 57 archive members in total: two dictionary XML files and 55 quest HTML files. The other 23,075 files are content-identical to the original archive.
- Preserves the original UTF-16 dictionary XML format and the client's `0x81` encrypted quest HTML format.

Compatibility and verification:

- Original `data.pak` SHA-256: `ac54a2ee01aef730500513c05755e32d8c2d4ee2eb9443ecb323a47717f99553`.
- Patched `data.pak` SHA-256: `534c565a40e1d75ffec358d4c55061277e5a8114d8f0c44a3b9ec8ad593bd7e3`.
- Validated 9,116 quest dialogs and 50,253 dictionary references, followed by a full archive integrity test and unpacked-tree consistency check.

### VIP Game.dll

`patch_game_dll_vip.py` generates a VIP `Game.dll` for Aion 5.8. It does not modify the source file, and both paths are provided on the command line.

> **Warning:** This patch may cause the client to crash. Use it with caution.
>
> **Installation:** The patched file must be named `Game.dll` and placed in the Aion 5.8 client's `bin64` directory.

```bash
python3 scripts/patch_game_dll_vip.py \
  --source /path/to/Game.dll.sts-auth-original \
  --out /path/to/Game.vip-world.dll \
  --sts-ip 127.0.0.1
```

- `--source`: Path to the original `Game.dll`. Its SHA-256 must be `f928a5dbc3d4d54e71f1968f38a75745882ef29f459d81b7b0fe50ac7cf490ad`.
- `--out`: Output DLL path. Missing parent directories are created automatically.
- `--sts-ip`: IPv4 address of the STS service. Defaults to `127.0.0.1`.

Paths may be absolute or relative to the current working directory. On success, the script prints the source path, output path, and generated file SHA-256.
