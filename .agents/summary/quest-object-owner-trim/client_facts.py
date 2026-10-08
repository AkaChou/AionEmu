#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""客户端 NPC 事实抽取：名称、对话 html 是否存在（物件能否成为发页目标）。

Client NPC facts: name + whether a dialog html exists (i.e. whether the entity can be the
target of a page send). 校准：798155(Atropos) 应有 html，700398(圣物) 应无 html（3036 结论）。

用法 / Usage: python3 client_facts.py <npcId> [<npcId> ...] [--unpack PATH]
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

from gather_evidence import resolve_unpack  # noqa: E402  （同目录复用解包根解析）


def client_npc_block(unpack: Path, npc_id: int) -> str | None:
    path = unpack / "npcs_unpacked/client_npcs_npc.xml"
    text = path.read_text(encoding="utf-8", errors="replace")
    match = re.search(rf"<npc_client>(?:(?!</npc_client>).)*?<id>{npc_id}</id>.*?</npc_client>",
                      text, re.S)
    return match.group(0) if match else None


def field(block: str, tag: str) -> str | None:
    match = re.search(rf"<{tag}>([^<]*)</{tag}>", block)
    return match.group(1).strip() if match else None


def dialog_html(unpack: Path, name: str) -> Path | None:
    """按名字大小写不敏感查找对话 html（解包树保留原大小写，客户端在 Windows 上不区分大小写）。
    Case-insensitive dialog-html lookup: the unpacked tree preserves the original case while the
    Windows client resolves names case-insensitively, so a lowercase file satisfies the request."""
    root = unpack / "data_unpacked/Dialogs"
    hits = sorted(root.rglob("*.html"))
    lowered = f"{name}.html".lower()
    for hit in hits:
        if hit.name.lower() == lowered:
            return hit
    return None


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    unpack = resolve_unpack(None)
    assert unpack is not None
    for raw in args:
        npc_id = int(raw)
        block = client_npc_block(unpack, npc_id)
        if not block:
            print(f"{npc_id}\t(client entry missing)")
            continue
        name = field(block, "name")
        quest_ai = field(block, "quest_ai_name")
        cursor = field(block, "cursor_type")
        tribe = field(block, "tribe")
        html = dialog_html(unpack, name) if name else None
        print(f"{npc_id}\tname={name}\tquest_ai_name={quest_ai}\tcursor={cursor}\ttribe={tribe}\t"
              f"html={'YES ' + str(html.relative_to(unpack)) if html else 'NONE'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
