#!/usr/bin/env python3
"""Thin retired entries out of *Fallbacks classes: drop the eager holder, resolve via getInstance()."""
import re
import sys
from pathlib import Path

CLASS_DOC_NOTE = (
    " * 已退役双源兜底的组件不再返回静态单例，而是直接 fail-fast。\n"
    " * Components whose dual-source fallback is retired no longer return a static singleton;\n"
    " * they fail fast instead.\n"
)


def method_doc(indent, cls):
    return (
        f"{indent}/**\n"
        f"{indent} * 返回 {cls}：双源兜底已退役，交由 {{@link {cls}#getInstance()}} fail-fast。\n"
        f"{indent} * Returns {cls}: the dual-source fallback is retired; delegates to "
        f"{cls}.getInstance() and fails fast.\n"
        f"{indent} *\n"
        f"{indent} * @return {cls} 实例 / {cls} instance\n"
        f"{indent} */\n"
    )


def thin(path, entries):
    p = Path(path)
    lines = p.read_text(encoding="utf-8").splitlines()
    for accessor, cls in entries:
        # locate accessor declaration
        decl = next(i for i, l in enumerate(lines) if re.search(rf'\bstatic\b[^;=]*\b{accessor}\(\)\s*\{{', l))
        indent = re.match(r'[ \t]*', lines[decl]).group(0)
        if any(f"{cls}.getInstance()" in lines[i] for i in range(decl, min(len(lines), decl + 6))):
            continue  # already thinned / 已处理
        # replace the fallback body
        body = next(i for i in range(decl, decl + 6) if "Fallback.INSTANCE" in lines[i])
        holder_name = re.search(r'(\w+Fallback)\.INSTANCE', lines[body]).group(1)
        lines[body] = f"{indent}    return {cls}.getInstance();"
        # replace javadoc right above the declaration
        j = decl - 1
        assert lines[j].strip() == "*/", f"{path}:{decl+1} no javadoc"
        k = j
        while lines[k].strip() != "/**":
            k -= 1
        lines[k:j + 1] = method_doc(indent, cls).splitlines()
        # locate and drop the holder class
        h = next(i for i, l in enumerate(lines) if f"class {holder_name}" in l)
        hs = h
        while hs - 1 >= 0 and (lines[hs - 1].strip() == "" or lines[hs - 1].lstrip().startswith(("/**", "*", "*/"))):
            hs -= 1
        balance = 0
        i = h
        while True:
            balance += lines[i].count("{") - lines[i].count("}")
            if balance == 0:
                break
            i += 1
        del lines[hs:i + 1]
    text = "\n".join(lines)
    # class-level note
    if "已退役双源兜底的组件不再返回静态单例" not in text:
        m = re.search(r'( \* [^\n]*\n)', text)
        assert m, f"{path}: class javadoc not found"
        text = text[:m.end()] + CLASS_DOC_NOTE + text[m.end():]
    p.write_text(text + "\n" if not text.endswith("\n") else text, encoding="utf-8")
    print(f"THINNED {path}: {[e[1] for e in entries]}")


if __name__ == "__main__":
    BASE = "src/main/java/com/aionemu/gameserver/lifecycle/"
    thin(BASE + "GameServerNetworkFallbacks.java", [
        ("loginServer", "LoginServer"),
        ("chatServer", "ChatServer"),
        ("aionPacketHandlerFactory", "AionPacketHandlerFactory"),
        ("packetFloodFilter", "PacketFloodFilter"),
        ("lsPacketHandlerFactory", "LsPacketHandlerFactory"),
    ])
    thin(BASE + "GameWorldBootstrapFallbacks.java", [
        ("idFactory", "IDFactory"),
        ("world", "World"),
    ])
    thin(BASE + "GameCoreServiceFallbacks.java", [
        ("dataManager", "DataManager"),
    ])
    thin(BASE + "GameEventRuntimeFallbacks.java", [
        ("eventScheduler", "EventScheduler"),
    ])
