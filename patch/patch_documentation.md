# VIP Game.dll 补丁文档 / VIP Game.dll Patch Documentation

## 中文

`patch_game_dll_vip.py` 用于生成适配 Aion 5.8 的 VIP `Game.dll`。脚本不会修改源文件，输入和输出路径均由命令行指定。

> **注意：** 使用此补丁可能导致客户端崩溃，请谨慎使用。
>
> **安装提醒：** 补丁文件必须命名为 `Game.dll`，并放在 Aion 5.8 客户端的 `bin64` 目录下。

```bash
python3 scripts/patch_game_dll_vip.py \
  --source /path/to/Game.dll.sts-auth-original \
  --out /path/to/Game.vip-world.dll
```

- `--source`：原始 `Game.dll` 文件路径，文件的 SHA-256 必须为 `f928a5dbc3d4d54e71f1968f38a75745882ef29f459d81b7b0fe50ac7cf490ad`。
- `--out`：生成文件路径；父目录不存在时会自动创建。

路径可以是绝对路径，也可以是相对于当前工作目录的相对路径。成功后脚本会打印输入路径、输出路径和生成文件的 SHA-256。

## English

`patch_game_dll_vip.py` generates a VIP `Game.dll` for Aion 5.8. It does not modify the source file, and both paths are provided on the command line.

> **Warning:** This patch may cause the client to crash. Use it with caution.
>
> **Installation:** The patched file must be named `Game.dll` and placed in the Aion 5.8 client's `bin64` directory.

```bash
python3 scripts/patch_game_dll_vip.py \
  --source /path/to/Game.dll.sts-auth-original \
  --out /path/to/Game.vip-world.dll
```

- `--source`: Path to the original `Game.dll`. Its SHA-256 must be `f928a5dbc3d4d54e71f1968f38a75745882ef29f459d81b7b0fe50ac7cf490ad`.
- `--out`: Output DLL path. Missing parent directories are created automatically.

Paths may be absolute or relative to the current working directory. On success, the script prints the source path, output path, and generated file SHA-256.
