# patch_game_dll_sts_ip.py

把 `Game.dll` 里硬编码的 STS IP 改成你的服务器 IP。

Patch the hardcoded STS IP in `Game.dll` to your server IP.

## 用法 / Usage

```bash
# 查看当前 IP / show current IP
python3 scripts/patch_game_dll_sts_ip.py "/path/to/Game.dll" --show

# 改 IP，并备份为 Game.dll.sts-ip-original
# patch IP and create backup Game.dll.sts-ip-original
python3 scripts/patch_game_dll_sts_ip.py "/path/to/Game.dll" 192.168.1.18 --backup

# 输出到新文件，不改原文件 / write a new file, leave original unchanged
python3 scripts/patch_game_dll_sts_ip.py "/path/to/Game.dll" 192.168.1.18 --out Game.dll.sts-192.168.1.18
```

修改前先关闭客户端。  
Close the client before patching.
