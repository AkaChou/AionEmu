#!/usr/bin/env python3
"""D1 证据表生成器：真端 Quest-AI 注册面（ScriptDLL (name, questId) × npcs.xml quest_ai_name）。

匹配口径 = **大小写不敏感**（真端语义，第一手证据）：
  1. NPCDB::Load 的 quest_ai_name 走名→id 表 FUN_140d18530，二分比较用 `_wcsicmp`
     —— server58-source/MainServer_Server64/fun/fun_249.cpp:3018（比较两侧均为宽串）；
     调用点 = fun_040.cpp:9923（case 0x886，NPCDB::Load quest_ai_name）。
  2. ScriptDLL 的对话名 map 遍历同样用 `_wcsicmp` —— ScriptDLL64.c:2075978/2076005。
  3. 自洽性：10110/10522/10525/10528/10529 注册 L"LF6_WEATHA_E"，而 npcs.xml 只有
     LF6_Weatha_E（806075）；大小写敏感语义下这些注册会悬空，与真端可运行事实矛盾。

输出 src/main/resources/aion/data/static_data/quest/retail/retail-quest-ai-registrations.xml：
  <quest_ai_registrations>
    <quest_ai_npc_ids>…全局 Quest-AI NPC id（升序逗号串）…</quest_ai_npc_ids>
    <quest_ai_registration>
      <quest_id>1001</quest_id>
      <registered_names>Kalio,Muranes,WCherubimL_3_n</registered_names>
      <registered_npc_ids>203067,203071,210670</registered_npc_ids>
    </quest_ai_registration>
    …
行只覆盖 XML 车道（quests/*.xml）里的任务 id —— 门禁只审这一面，表体保持精简。
用法：python3 emit_quest_ai_registrations.py [--emit]
"""
import re
import sys
from collections import defaultdict
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
DLL = Path("/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c")
NPCS = Path("/Users/mc/IdeaProjects/58Server/Map/XML/npcs.xml")
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
OUT = REPO / "src/main/resources/aion/data/static_data/quest/retail/retail-quest-ai-registrations.xml"


def registry() -> dict[int, set[str]]:
    src = DLL.read_text(encoding="latin-1", errors="replace")
    by_quest: dict[int, set[str]] = defaultdict(set)
    for call in re.findall(r"FUN_180cb5920\(([^;\n]*)\);", src):
        parts = [p.strip() for p in call.rsplit(",", 2)]
        if len(parts) != 3:
            continue
        m = re.search(r'L"([^"]+)"', parts[1])
        if not m:
            continue
        last = parts[2]
        try:
            qid = int(last, 16) if last.startswith("0x") else int(last)
        except ValueError:
            continue
        by_quest[qid].add(m.group(1))
    return by_quest


def npcs_by_name() -> dict[str, set[int]]:
    data = NPCS.read_bytes()
    txt = data.decode("utf-16") if data[:2] in (b"\xff\xfe", b"\xfe\xff") else data.decode("utf-8", "replace")
    cur, out = None, defaultdict(set)
    for line in txt.splitlines():
        s = line.strip()
        if s.startswith("<id>") and s.endswith("</id>"):
            cur = int(s[4:-5])
        elif s.startswith("<quest_ai_name>") and s.endswith("</quest_ai_name>") and cur is not None:
            out[s[15:-16]].add(cur)
    return out


def folded(by_name: dict[str, set[int]]) -> dict[str, set[int]]:
    """大小写折叠索引（真端 `_wcsicmp` 口径；名字全 ASCII ⇒ casefold == lower）。"""
    out: dict[str, set[int]] = defaultdict(set)
    for name, ids in by_name.items():
        out[name.casefold()].update(ids)
    return out


def main() -> int:
    emit = "--emit" in sys.argv
    by_quest = registry()
    by_name = npcs_by_name()
    by_fold = folded(by_name)
    xml_ids = sorted(int(f.stem) for f in QUESTS.glob("*.xml"))

    registered_names = {n for names in by_quest.values() for n in names}
    global_ai = {i for n in registered_names for i in by_fold.get(n.casefold(), set())}
    rows = []
    for q in xml_ids:
        names = sorted(by_quest.get(q, set()))
        ids = sorted({i for n in names for i in by_fold.get(n.casefold(), set())})
        rows.append((q, names, ids))

    covered = sum(1 for _, _, ids in rows if ids)
    unresolved = sum(1 for _, names, ids in rows if names and not ids)
    ambiguous = sum(1 for n in registered_names if len(by_fold.get(n.casefold(), ())) > 1)
    print(f"注册面任务 {len(by_quest)}；注册名 {len(registered_names)}（大小写折叠后歧义 {ambiguous}）")
    print(f"全局 Quest-AI NPC id {len(global_ai)}；XML 车道任务 {len(xml_ids)}；"
          f"有注册 {covered}；注册名全不解析 {unresolved}")
    if not emit:
        print("(dry run; pass --emit to write)")
        return 0

    lines = ['<?xml version="1.0" encoding="UTF-8"?>',
             '<!-- D1 证据表：真端 Quest-AI 注册面（生成器 p11-quest-ai-lane/emit_quest_ai_registrations.py）——',
             '     ScriptDLL FUN_180cb5920(name, questId) × 真端 npcs.xml quest_ai_name，名匹配按真端',
             '     _wcsicmp 语义大小写不敏感（证据见生成器 docstring）；行覆盖 XML 车道任务；',
             '     门禁 QuestAiDialogBindingGateTest 消费本表。 -->',
             '<quest_ai_registrations>',
             '  <quest_ai_npc_ids>' + ",".join(str(i) for i in sorted(global_ai)) + '</quest_ai_npc_ids>']
    for q, names, ids in rows:
        lines.append('  <quest_ai_registration>')
        lines.append(f'    <quest_id>{q}</quest_id>')
        lines.append(f'    <registered_names>{",".join(names)}</registered_names>')
        lines.append(f'    <registered_npc_ids>{",".join(str(i) for i in ids)}</registered_npc_ids>')
        lines.append('  </quest_ai_registration>')
    lines.append('</quest_ai_registrations>')
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {OUT.relative_to(REPO)} ({OUT.stat().st_size} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
