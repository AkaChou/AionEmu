#!/usr/bin/env python3
"""D1 度量：XML 车道任务的 NPC 引用是否落在真端 Quest-AI 注册集合内。

规则（门禁口径，消费方 QuestAiDialogBindingGateTest）：
  对每份 quests/<q>.xml：
    refs = 全部 <dialog npc-id> / <npc-complete npc-id>（对话 + 交付绑定面；唯一化）
    violation(q,N) ⇔ N ∈ 全局 Quest-AI NPC 集（= 真端注册面出现过的注册名在 npcs.xml
                     解析出的 npc id 集）∧ q ∈ 注册面 ∧ N ∉ registered(q)
    unregistered(q) ⇔ q ∉ 注册面 ∧ refs ∩ 全局集 ≠ ∅（真端无 Quest-AI ingress）

  名解析口径 = 真端 `_wcsicmp` 大小写不敏感（生成器 docstring 列了三条第一手证据）。

  击杀/掉落/目标类 npc-id（kill-npc/drop/…）不参与：它们不是 Quest-AI 对话位。

用法：
  python3 measure_binding_gate.py                # 读数
  python3 measure_binding_gate.py --emit-constants  # 追加输出门禁冻结常量（Java 源码片段）
"""
import re
import sys
from collections import defaultdict
from pathlib import Path

DLL = Path("/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c")
NPCS = Path("/Users/mc/IdeaProjects/58Server/Map/XML/npcs.xml")
QUESTS = Path("src/main/resources/aion/data/static_data/quest/definitions/quests")
REF_PATTERN = r'<(?:dialog|npc-complete)\b[^>]*npc-id="(\d+)"'


def registry():
    src = DLL.read_text(encoding="latin-1", errors="replace")
    by_quest = defaultdict(set)
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


def npcs_by_quest_ai():
    """name → ids（原文键）与 casefold 键两套索引。"""
    data = NPCS.read_bytes()
    txt = data.decode("utf-16") if data[:2] in (b"\xff\xfe", b"\xfe\xff") else data.decode("utf-8", "replace")
    cur, exact, fold = None, defaultdict(set), defaultdict(set)
    for line in txt.splitlines():
        s = line.strip()
        if s.startswith("<id>") and s.endswith("</id>"):
            cur = int(s[4:-5])
        elif s.startswith("<quest_ai_name>") and s.endswith("</quest_ai_name>") and cur is not None:
            name = s[15:-16]
            exact[name].add(cur)
            fold[name.casefold()].add(cur)
    return exact, fold


def audit():
    by_quest = registry()
    exact, fold = npcs_by_quest_ai()
    registered = {q: set().union(*(fold.get(n.casefold(), set()) for n in names)) if names else set()
                  for q, names in by_quest.items()}
    registered_names = {n for names in by_quest.values() for n in names}
    global_ai = {i for n in registered_names for i in fold.get(n.casefold(), set())}
    del exact  # 原文键索引只用于生成器报告折叠歧义，本度量按真端折叠语义

    violations = defaultdict(set)
    unregistered = defaultdict(set)
    files_with_refs = 0
    total_refs = 0
    refs_in_global = 0
    for f in sorted(QUESTS.glob("*.xml")):
        q = int(f.stem)
        text = f.read_text(encoding="utf-8")
        refs = {int(x) for x in re.findall(REF_PATTERN, text)}
        if not refs:
            continue
        files_with_refs += 1
        total_refs += len(refs)
        hits = refs & global_ai
        refs_in_global += len(hits)
        if q in registered:
            violations[q] = {n for n in hits if n not in registered[q]}
        else:
            unregistered[q] = hits
    violations = {q: v for q, v in violations.items() if v}
    unregistered = {q: v for q, v in unregistered.items() if v}
    return dict(by_quest=by_quest, global_ai=global_ai, registered=registered,
                registered_names=registered_names,
                violations=violations, unregistered=unregistered,
                files_with_refs=files_with_refs, total_refs=total_refs, refs_in_global=refs_in_global)


def emit_constants(result):
    violations = result["violations"]
    unregistered = result["unregistered"]
    print()
    print("	/** 跨界冻结：真端注册面按「注册名」组织，名↔npc id 是多对多（同一 NPC 可承接多条任务；")
    print("	 * 同一注册名可展开到多个 NPC），故既有 XML 引用的对话 NPC 不必然出现在本任务的注册展开里。")
    print("	 * 观测面 %d 任务 / %d 引用，逐元素冻结（新增即红，收缩须同批改常量）。 */"
          % (len(violations), sum(len(v) for v in violations.values())))
    print("	private static final Map<Integer, Set<Integer>> FROZEN_CROSS_QUEST = Map.ofEntries(")
    entries = []
    for q in sorted(violations):
        ids = ", ".join(str(n) for n in sorted(violations[q]))
        entries.append("			Map.entry(%d, Set.of(%s))" % (q, ids))
    print(",\n".join(entries) + ");")
    print()
    print("	/** 真端无 Quest-AI 注册但 XML 带对话引用的任务（%d 件，逐元素冻结）。 */" % len(unregistered))
    print("	private static final Set<Integer> FROZEN_UNREGISTERED_TASKS = Set.of(")
    ids = sorted(unregistered)
    lines, cur = [], []
    for q in ids:
        cur.append(str(q))
        if len(cur) == 8:
            lines.append(", ".join(cur)); cur = []
    if cur:
        lines.append(", ".join(cur))
    print(",\n".join("			" + l for l in lines) + ");")


def main():
    result = audit()
    by_quest, global_ai = result["by_quest"], result["global_ai"]
    violations, unregistered = result["violations"], result["unregistered"]
    print(f"注册面任务数={len(by_quest)}  展开 NPC id 集合大小={len(global_ai)}")
    print(f"XML 任务文件数={len(list(QUESTS.glob('*.xml')))}  有对话引用的文件={result['files_with_refs']}"
          f"  引用（文件×NPC 去重）={result['total_refs']}  其中命中全局集={result['refs_in_global']}")
    print(f"违反任务数={len(violations)}（引用合计 {sum(len(v) for v in violations.values())}）")
    for q in sorted(violations):
        print(f"  VIOLATION {q}: {sorted(violations[q])}")
    print(f"未注册但有 AI-NPC 引用的任务数={len(unregistered)}")
    for q in sorted(unregistered):
        print(f"  UNREGISTERED {q}: {sorted(unregistered[q])}")
    if "--emit-constants" in sys.argv:
        emit_constants(result)
    return 0


if __name__ == "__main__":
    sys.exit(main())
