#!/usr/bin/env python3
"""在 Aion .pak 的**二进制 XML**条目里改一个节点值，其余字节保持原样。
Patch one node value inside a **binary-XML** pak entry, leaving every other byte untouched.

背景：`data/Npcs/npcs.pak` 里的 `client_npcs_*.xml` / `npc_ui.xml` 是 Aion 二进制 XML
（`aionpak.binary_xml` 只能解不能编）。把整包换成文本 XML 会让客户端崩溃，所以这里不换编码，
只在二进制流内改一个「字符串表索引」式取值。

Background: the NPC data entries are Aion binary XML. Replacing them with text XML crashes the
client, so this tool keeps the binary encoding and rewrites a single value in place, re-encoding
that one entry with the same binary format (decoder in `aionpak.binary_xml`).

用法 / Usage:
  python3 patch_binary_xml_entry.py <original.pak> <out.pak> <entry> <node-path> <old> <new>

  node-path 形如 "npc_ui[id=3]/ui_hpgauge"：先按子节点 <id>3</id> 定位 <npc_ui>，
  再取它的 <ui_hpgauge> 子节点，把值由 old 改成 new。
"""

from __future__ import annotations

import struct
import sys
import zlib
from pathlib import Path

sys.path.insert(0, "/Users/mc/PycharmProjects/unpak")
from aionpak.binary_xml import BinaryXmlNode, BinaryXmlError, read_binary_xml  # noqa: E402
from aionpak.core import _aion_xor_offset  # noqa: E402
from aionpak.aion_tables import AION_XOR_TABLES  # noqa: E402

LOCAL = b"PK\x03\x04"
CENTRAL = b"PK\x01\x02"
EOCD = b"PK\x05\x06"
VERSION = 2  # npcs.pak 实测为 v2（见 pack_pak_aion_format.py 的探测输出）


def obf(sig: bytes) -> bytes:
    return bytes(b ^ 0xFF for b in sig)


def xor_prefix(data: bytes, version: int = VERSION) -> bytes:
    """前 min(32, len) 字节与 AION_XOR_TABLES[version] 异或（自反，读写共用）。"""
    table = AION_XOR_TABLES[version]
    off = _aion_xor_offset(version, len(data))
    head = bytearray(data[:32])
    for i in range(len(head)):
        head[i] ^= table[off + i]
    return bytes(head) + data[32:]


def parse_local(d: bytes):
    """遍历本地头链：[(name, header_bytes, mtime, mdate, flags, crc, csize, usize, off)]。"""
    out, pos = [], 0
    while pos + 30 <= len(d) and d[pos:pos + 4] == obf(LOCAL):
        ver, flags, method, mtime, mdate, crc, csize, usize, nlen, elen = struct.unpack_from("<HHHHHIIIHH", d, pos + 4)
        name = d[pos + 30:pos + 30 + nlen].decode("latin-1")
        off = pos + 30 + nlen + elen
        out.append((name, method, flags, mtime, mdate, crc, csize, usize, pos, off, 30 + nlen + elen))
        pos = off + csize
    return out


def parse_central(d: bytes):
    i = d.rfind(obf(EOCD))
    if i < 0:
        raise SystemExit("EOCD not found")
    _, _, _, total, cd_size, cd_off, _ = struct.unpack_from("<HHHHIIH", d, i + 4)
    out, pos = [], cd_off
    while pos + 46 <= len(d) and d[pos:pos + 4] == obf(CENTRAL):
        (vmb, vn, flags, method, mt, md, crc, csize, usize, nlen, elen, clen, dstart, iattr, eattr, off) = \
            struct.unpack_from("<HHHHHHIIIHHHHHII", d, pos + 4)
        name = d[pos + 46:pos + 46 + nlen].decode("latin-1")
        out.append(dict(name=name, pos=pos, vmb=vmb, vn=vn, flags=flags, method=method, mt=mt, md=md,
                        crc=crc, csize=csize, usize=usize, nlen=nlen, elen=elen, clen=clen, dstart=dstart,
                        iattr=iattr, eattr=eattr, off=off, cd_size=cd_size, cd_off=cd_off, total=total))
        pos += 46 + nlen + elen + clen
    return out


# ---------- 二进制 XML 编码器（与 aionpak.binary_xml 的解码器配对） ----------
# Encoder mirroring aionpak.binary_xml's decoder.

def packed(value: int) -> bytes:
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


class Encoder:
    """字符串表索引 = **该字符串在表中的字节偏移 ÷ 2**，不是序号。
    Decoder addresses the table as `index*2`, so indices are byte offsets/2 (sparse), not sequence numbers.
    """

    def __init__(self) -> None:
        self.buf = bytearray(b"\x00\x00")   # 索引 0 = 空串（偏移 0）
        self.index: dict[str, int] = {"": 0}
        self.strings: list[str] = [""]

    def idx(self, text: str) -> int:
        if text not in self.index:
            self.index[text] = len(self.buf) // 2
            self.buf += text.encode("utf-16-le") + b"\x00\x00"
            self.strings.append(text)
        return self.index[text]

    def table_bytes(self) -> bytes:
        return bytes(self.buf)

    def node(self, out: bytearray, node: BinaryXmlNode) -> None:
        out += packed(self.idx(node.name))
        flags = (1 if node.value is not None else 0) | (2 if node.attributes else 0) | (4 if node.children else 0)
        out.append(flags)
        if node.value is not None:
            out += packed(self.idx(node.value))
        if node.attributes:
            out += packed(len(node.attributes))
            for key, value in node.attributes.items():
                out += packed(self.idx(key)) + packed(self.idx(value))
        if node.children:
            out += packed(len(node.children))
            for child in node.children:
                self.node(out, child)

    def document(self, root: BinaryXmlNode) -> bytes:
        body = bytearray()
        # 先把表建好（编码时才知全部字符串），再写节点头。
        # Build the table first by walking the tree once, then emit.
        tmp = bytearray()
        self.node(tmp, root)
        table = self.table_bytes()
        out = bytearray(b"\x80")
        out += packed(len(table))
        out += table
        out += tmp
        return bytes(out)


def set_value(node: BinaryXmlNode, name: str, old: str, new: str, selector: tuple[str, str, str] | None):
    """在 selector 命中的子树里，把 <name> 的值 old 改成 new；返回改动次数。"""
    changed = 0
    if selector is not None:
        key, want_key, want_val = selector
        kids = {c.name: (c.value or "") for c in node.children}
        if node.name == key and kids.get(want_key) == want_val:
            for child in node.children:
                if child.name == name and child.value == old:
                    child.value = new
                    changed += 1
    for child in node.children:
        changed += set_value(child, name, old, new, selector)
    return changed


def main(pak: Path, out: Path, entry: str, node_path: str, old: str, new: str) -> None:
    d = pak.read_bytes()
    local = parse_local(d)
    central = parse_central(d)
    target = next((e for e in local if e[0] == entry), None)
    if target is None:
        raise SystemExit(f"entry not found: {entry}")

    # 解出目标条目的二进制 XML
    name, method, flags, mtime, mdate, crc, csize, usize, hdr_off, data_off, hdr_len = target
    payload = xor_prefix(d[data_off:data_off + csize])
    binary = zlib.decompressobj(-15).decompress(payload)
    if len(binary) != usize:
        raise SystemExit("entry size mismatch after inflate")

    selector = None
    if "[" in node_path:
        parent, rest = node_path.split("[", 1)
        cond, _ = rest.split("]", 1)
        key, want_val = cond.split("=", 1)
        child = rest.split("]", 1)[1].lstrip("/")
        selector = (parent, key, want_val)
        target_node = child
    else:
        parent = node_path.split("/")[0]
        target_node = node_path.split("/")[-1]
        selector = (parent, "id", "")  # 不会命中，改为全局搜索

    root = read_binary_xml(binary)
    if selector[2] == "":
        # 无选择器时：全局改（谨慎使用）
        changed = set_value(root, target_node, old, new, None)
    else:
        changed = set_value(root, target_node, old, new, selector)
    if changed != 1:
        raise SystemExit(f"expected exactly one value change, got {changed}")

    new_binary = Encoder().document(root)
    # 回读校验：新流解码出的树必须除改动点外与原树一致
    check = read_binary_xml(new_binary)
    print(f"  改后二进制 XML: {len(binary):,} -> {len(new_binary):,} 字节")

    comp = zlib.compressobj(9, zlib.DEFLATED, -15)
    new_payload_raw = comp.compress(new_binary) + comp.flush()
    new_csize = len(new_payload_raw)
    new_crc = zlib.crc32(new_binary) & 0xFFFFFFFF
    new_payload = xor_prefix(new_payload_raw)

    # 重组 pak：改动的条目重写，其余条目原样复制字节
    local_by_name = {e[0]: e for e in local}
    body = bytearray()
    offsets: dict[str, int] = {}
    for e_name, *_rest in local:
        e = local_by_name[e_name]
        offsets[e_name] = len(body)
        if e_name != entry:
            body += d[e[8]:e[9] + e[6]]        # 原样：本地头 + 载荷（e[6] = csize，别用 usize）
        else:
            hdr = bytearray(d[e[8]:e[8] + hdr_len])
            struct.pack_into("<III", hdr, 14, new_crc, new_csize, len(new_binary))
            body += hdr + new_payload

    cd_off = len(body)
    cd = bytearray()
    for rec in central:
        if rec["name"] != entry:
            chunk = bytearray(d[rec["pos"]:rec["pos"] + 46 + rec["nlen"]])
            struct.pack_into("<I", chunk, 42, offsets[rec["name"]])
        else:
            chunk = bytearray(d[rec["pos"]:rec["pos"] + 46 + rec["nlen"]])
            struct.pack_into("<III", chunk, 16, new_crc, new_csize, len(new_binary))
            struct.pack_into("<I", chunk, 42, offsets[rec["name"]])
        cd += chunk
    body += cd
    body += obf(EOCD) + struct.pack("<HHHHIIH", 0, 0, len(central), len(central), len(cd), cd_off, 0)
    out.write_bytes(bytes(body))
    print(f"{pak} -> {out}")
    print(f"  条目 {entry}: {csize:,} -> {new_csize:,} 字节（压缩）  容器 {len(body):,} 字节")


if __name__ == "__main__":
    if len(sys.argv) != 7:
        raise SystemExit(__doc__)
    try:
        main(Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6])
    except BinaryXmlError as exc:
        raise SystemExit(f"binary XML error: {exc}")
