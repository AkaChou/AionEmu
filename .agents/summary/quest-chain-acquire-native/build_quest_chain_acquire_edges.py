#!/usr/bin/env python3
"""生成 DD 链式接取（`category_acquire_=none`）发放边资源。

数据源（全部仓库内可复算）：
  ① 真端 DD 表 `data_driven_quest.xml`：确定 `none` 行域（本表只登记其中"有链路证据"的行）。
  ② 真端 `quest.xml finished_quest_condN`：26 行的前序（Q<id> 词法，经行名索引回退解析）。
  ③ 退役/存活任务定义 XML 与退役 Java handler（git 历史，`git show <commit>^:<path>`）：
     8 行（10032/10033/10034/10035/20032/20033/20034/20035）的 `<prerequisites>` /
     `<start-conditions type="finished">` / `defaultOnLvlUpEvent(env, <前序>, true)` 证据。
     handler 证据与前序集合一致时优先写入 evidence（更强的行为证据）；不一致时不猜、只记 XML 证据并打印告警。

用法 / Usage:
  python3 -B build_quest_chain_acquire_edges.py            # 重写资源（生成物，禁止手改）
  python3 -B build_quest_chain_acquire_edges.py --check    # 复算并与入仓资源逐字节比对（陈旧 = 退出码 1）
  python3 -B build_quest_chain_acquire_edges.py --print    # 只打印推导结果（人工复核用）
"""
import re
import subprocess
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
DD_XML = REPO / "src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml"
RETAIL_Q = REPO / "src/main/resources/aion/data/static_data/quest/retail/quest.xml"
LIVE_DEFS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
OUT = REPO / "src/main/resources/aion/data/static_data/quest/retail/quest_chain_acquire_edges.xml"

# 退役 Java handler 的历史路径（migrate 前后两代目录）。
HANDLER_PATTERNS = ("**/_%d*.java", "**/%d*.java")

HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<!--
DD 链式接取发放边（`category_acquire_=none`；本表生成物，禁止手改）。
生成器：.agents/summary/quest-chain-acquire-native/build_quest_chain_acquire_edges.py
数据源：① 真端 quest.xml finished_quest_condN（source=retail-finished-cond）；
        ② 退役/存活任务定义 XML 的 <prerequisites> / <start-conditions type="finished"> 与
           退役 Java handler 的 defaultOnLvlUpEvent(env, <前序>, true)（source=retired-xml / retired-handler，
           证据 = git 提交 ^ 路径）。
运行期消费：DataDrivenNativeRuntime 只注册「owned ∧ routed ∧ acquire=none ∧ 非冻结」的后继
（XML_RETENTION 行与冻结行被过滤，逐行理由见门禁 QuestChainAcquireResourceGateTest）。
DD chain-acquire edges (acquire=none rows; generated file — do not hand-edit). The runtime registers
only successors that are owned, routed, acquire=none and unfrozen; XML_RETENTION and frozen rows are
filtered with per-row reasons pinned by the resource gate.
列 / Columns: successor / predecessors（空格分隔）/ source（retail-finished-cond|retired-xml|retired-handler）/ evidence
-->
<quest_chain_acquire_edges>
"""


def run_git(*args):
    result = subprocess.run(["git", *args], cwd=REPO, capture_output=True, text=True)
    return result.stdout if result.returncode == 0 else ""


def dd_acquire_kinds():
    """DD 表逐行接取类别（小写）。 / DD rows by acquire kind (lowercase)."""
    raw = DD_XML.read_bytes()
    enc = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    text = raw.decode(enc, errors="ignore")
    kinds = {}
    for body in re.findall(r"<quest_data_driven>(.*?)</quest_data_driven>", text, re.S):
        qid = re.search(r"<id>(\d+)</id>", body)
        acquire = re.search(r"<category_acquire_>([^<]*)</category_acquire_>", body)
        if qid:
            kinds[int(qid.group(1))] = (acquire.group(1).strip().lower() if acquire else "none")
    return kinds


def retail_rows():
    """真端 quest.xml 行：name / finished_quest_condN。 / Retail rows: name + finished conditions."""
    text = RETAIL_Q.read_text(encoding="utf-8", errors="ignore")
    rows = {}
    for body in re.findall(r"<quest>(.*?)</quest>", text, re.S):
        qid = re.search(r"<id>(\d+)</id>", body)
        name = re.search(r"<name>([^<]*)</name>", body)
        if not qid:
            continue
        finished = re.findall(r"<finished_quest_cond\d+>([^<]*)</finished_quest_cond\d+>", body)
        rows[int(qid.group(1))] = (name.group(1) if name else "", finished)
    return rows


def prune_definition(body):
    """<prerequisites>/<start-conditions type="finished"> 的前序 id（保持出现序、去重）。"""
    preds = []
    for block in re.findall(r"<prerequisites>\s*(.*?)\s*</prerequisites>", body, re.S):
        preds += [int(x) for x in re.findall(r'<quest id="(\d+)"', block)]
    preds += [int(x) for x in re.findall(r'<condition type="finished" quest-id="(\d+)"', body)]
    return list(dict.fromkeys(preds))


def live_definition(quest_id):
    path = LIVE_DEFS / f"{quest_id}.xml"
    return (path.read_text(encoding="utf-8"), f"HEAD {path.relative_to(REPO)}") if path.is_file() else None


def history_definition(quest_id):
    """删除批里的定义 XML（新/旧两代路径，取最近一次删除的上一版）。"""
    candidates = [
        f"src/main/resources/aion/data/static_data/quest/definitions/quests/{quest_id}.xml",
        f"src/main/resources/aion/data/static_data/quest_definition/quests/{quest_id}.xml",
    ]
    best = None
    for path in candidates:
        commit = run_git("log", "--all", "--diff-filter=D", "--format=%H %ad", "--date=short", "-1", "--", path).strip()
        if not commit:
            continue
        sha, date = commit.split()[0], commit.split()[1]
        body = run_git("show", f"{sha}^:{path}")
        if body and (best is None or date > best[0]):
            best = (date, body, f"{sha} {path}")
    return (best[1], best[2]) if best else None


def handler_evidence(quest_id):
    """退役 handler 的默认升级接取边（证据行 + 前序集合）；无则 None。"""
    listing = run_git("log", "--all", "--diff-filter=D", "--format=%H", "--name-only", "--",
                      *[p % quest_id for p in HANDLER_PATTERNS])
    lines = [line for line in listing.splitlines() if line.strip()]
    shas = [line for line in lines if re.fullmatch(r"[0-9a-f]{40}", line)]
    paths = [line for line in lines if line.endswith(".java")]
    if not shas or not paths:
        return None
    sha, path = shas[0], paths[0]
    body = run_git("show", f"{sha}^:{path}")
    if not body:
        return None
    for line in body.splitlines():
        match = re.search(r"defaultOnLvlUpEvent\(env,\s*(.+?)\)", line)
        if not match:
            continue
        preds = [int(x) for x in re.findall(r"\d+", match.group(1))]
        if preds:
            return preds, f"{sha}^:{path} {line.strip()}"
    return None


def build_rows():
    kinds = dd_acquire_kinds()
    none_ids = {q for q, kind in kinds.items() if kind == "none"}
    rows = retail_rows()
    name_to_id = {name: qid for qid, (name, _) in rows.items() if name}

    def resolve(token):
        token = token.strip()
        if re.fullmatch(r"Q\d+", token):
            return int(token[1:])
        return name_to_id.get(token)

    out = []
    for quest_id in sorted(none_ids):
        finished = rows.get(quest_id, ("", []))[1]
        if finished:
            preds = [resolve(token) for token in finished]
            if any(p is None for p in preds):
                raise SystemExit(f"unresolved finished_quest_cond token: quest {quest_id} {finished}")
            out.append((quest_id, preds, "retail-finished-cond",
                        "quest.xml finished_quest_cond=" + " ".join(finished)))
            continue
        definition = live_definition(quest_id) or history_definition(quest_id)
        if definition is None:
            continue
        preds = prune_definition(definition[0])
        if not preds:
            continue
        handler = handler_evidence(quest_id)
        if handler is not None and sorted(handler[0]) == sorted(preds):
            out.append((quest_id, preds, "retired-handler", handler[1]))
        else:
            if handler is not None:
                print(f"warn: handler predecessors {handler[0]} != xml {preds} for {quest_id}; "
                      f"keeping the xml evidence", file=sys.stderr)
            out.append((quest_id, preds, "retired-xml", definition[1]))
    return out


def render(rows):
    body = []
    for quest_id, preds, source, evidence in rows:
        body.append(f"  <edge>\n"
                    f"    <successor>{quest_id}</successor>\n"
                    f"    <predecessors>{' '.join(str(p) for p in preds)}</predecessors>\n"
                    f"    <source>{source}</source>\n"
                    f"    <evidence>{evidence}</evidence>\n"
                    f"  </edge>")
    return HEADER + "\n".join(body) + "\n</quest_chain_acquire_edges>\n"


def main():
    rows = build_rows()
    rendered = render(rows)

    if "--print" in sys.argv:
        for quest_id, preds, source, evidence in rows:
            print(f"{quest_id}\t{' '.join(map(str, preds))}\t{source}\t{evidence}")
        print(f"rows={len(rows)}", file=sys.stderr)
        return 0

    if "--check" in sys.argv:
        current = OUT.read_text(encoding="utf-8") if OUT.is_file() else ""
        if current != rendered:
            print(f"STALE: {OUT.relative_to(REPO)} differs from the regenerated form", file=sys.stderr)
            return 1
        print(f"ok: {OUT.relative_to(REPO)} rows={len(rows)}")
        return 0

    OUT.write_text(rendered, encoding="utf-8")
    print(f"wrote {OUT.relative_to(REPO)} rows={len(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
