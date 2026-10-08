# -*- coding: utf-8 -*-
"""客户端对话 HTML 修复工具库（2026-10-08）
从实机 data.pak 提取 → 解密 → 文本修复 → 加密回 blob。
加密/解密为 XOR 对合：blob = _crypt(0x81 0x81 + utf16le_payload, filename)
"""
import sys, zipfile
from pathlib import Path

UNPAK = Path.home() / "PycharmProjects/unpak"
sys.path.insert(0, str(UNPAK))
from aionpak.core import _crypt_aion_html_blob, decrypt_aion_html_blob

LIVE_PAK = Path.home() / "IdeaProjects/5.8客户端/L10N/CHS/Data/data.pak"

def read_entry_text(pak, entry):
    """从 pak 读取条目 → 解密 → 返回 (text, utf16le_bytes_with_bom)。"""
    with zipfile.ZipFile(pak) as z:
        raw = z.read(entry)
    dec = decrypt_aion_html_blob(raw, entry)
    assert dec is not None, f"{entry} 不是 0x81 加密 blob"
    assert dec[:2] == b'\xff\xfe', f"{entry} 非 UTF-16LE BOM"
    return dec.decode('utf-16-le').lstrip('﻿'), dec

def encode_entry(text):
    """text → UTF-16LE+BOM 字节 → 加回 0x81 0x81 前缀。"""
    payload = '﻿' + text
    return payload.encode('utf-16-le')

def encrypt_blob(utf16le_bytes, entry):
    """UTF-16LE 字节（无前缀）→ pak 存储的加密 blob。"""
    return _crypt_aion_html_blob(b'\x81\x81' + utf16le_bytes, entry)

def repair_pak(src_pak, dst_pak, fixes):
    """按 fixes: {entry: fn(text)->text} 逐条目替换，重建 zip。
    其他条目原字节搬运（zipfile 重压缩，deflate），保留条目顺序与 metadata。"""
    with zipfile.ZipFile(src_pak) as zin, zipfile.ZipFile(dst_pak, 'w') as zout:
        for info in zin.infolist():
            data = zin.read(info.filename)
            if info.filename in fixes:
                old_text, _ = (lambda d: (d.decode('utf-16-le').lstrip('﻿'), d))(decrypt_aion_html_blob(data, info.filename))
                new_text = fixes[info.filename](old_text)
                data = encrypt_blob(encode_entry(new_text), info.filename)
            zout.writestr(info, data)
    return dst_pak
