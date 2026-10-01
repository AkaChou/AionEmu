#!/usr/bin/env python3
"""ItemPlay 行集真实分解 + 真端节点槽/相机形态逐行复算（P5D，只读）。

真端不变量（P5D 坐实，`ScriptDLL64.c`）：
  * 每个 NPC 一个节点 `FUN_180cb5920(&DAT_node, L"<NPC>", <questId>)`；
  * `slot 0` = 接取节点（acquired_npc_name）；
  * `slot 3 #K` = 第 K 步节点：K = 0..relayCount-1 ⇒ `talk_npc(K+1)`，K = min(relayCount+1,3) ⇒ 交付节点；
  * `slot 4` = 交付节点（reward_npc_name）；
  * **相机（步进边）**：本行主注册 `FUN_180cb2eb0(..., questId, kind=5, ...)` 的 thunk 内，
    取 (状态, 步) 后 `if (status == 3 && step == <guard>) set(questId, <target>, 0)` ⇒
    `guard = relayCount`、`target = relayCount + 1`（用道具事件触发，闸门就是步数 vars 前置）；
    例外 = 80255/80256（无相机 ⇒ advance 轴在真端不存在）。

输出：`itemplay-shape.tsv`（逐行）+ stdout 汇总（不变量例外、行集分解）。
"""
from __future__ import annotations

import collections
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[5]
TABLE = ROOT / "src/main/resources/aion/data/static_data/quest/retail/Quest_SimpleItemPlay.xml"
RETENTION = ROOT / "src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv"
XML_DIR = ROOT / "src/main/resources/aion/data/static_data/quest/definitions/quests"
C_SOURCE = Path("/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c")
OUT = Path(__file__).resolve().parent.parent / "itemplay-shape.tsv"

NODE_RE = re.compile(r'FUN_180cb5920\(&(DAT_[0-9a-f]+),L"([^"]+)",(0x[0-9a-f]+|\d+)\)')
SLOT_RE = re.compile(
    r'FUN_180cb3070\(&(DAT_[0-9a-f]+),&(DAT_[0-9a-f]+),(0x[0-9a-f]+|\d+),(0x[0-9a-f]+|\d+),(0x[0-9a-f]+|\d+),0\)')
KIND_RE = re.compile(
    r'FUN_180cb2eb0\(&(DAT_[0-9a-f]+),(0x[0-9a-f]+|\d+),(0x[0-9a-f]+|\d+),(0x[0-9a-f]+|\d+),(FUN_[0-9a-f]+)\)')
GUARD_RE = re.compile(r'local_res9 == (-?\d+)')
SET_RE = re.compile(r'\(param_1,(0x[0-9a-f]+|\d+),(-?\d+),0\);')

ITEMPLAY_KIND = 5
NO_CAMERA_ROWS = (80255, 80256)


def num(text: str) -> int:
    return int(text, 16) if text.startswith("0x") else int(text)


def camera_of(lines: list[str], quest_id: int) -> tuple[int | None, int | None]:
    """本行主 thunk 的推进闸门与目标步（真端：用道具事件触发相机）。"""
    thunk = None
    for line in lines:
        m = KIND_RE.search(line)
        if m and num(m.group(2)) == quest_id and num(m.group(3)) == ITEMPLAY_KIND:
            thunk = m.group(5)
    if thunk is None:
        return (None, None)
    body, inside = [], False
    for line in lines:
        if not inside and line.startswith(f"void {thunk}("):
            inside = True
        if inside:
            body.append(line)
            if line.strip() == "}":
                break
    guard = target = None
    for line in body:
        g = GUARD_RE.search(line)
        if g and guard is None:
            guard = int(g.group(1))
        t = SET_RE.search(line)
        if t and num(t.group(1)) == quest_id and target is None:
            target = int(t.group(2))
    return (guard, target)


def text(row: ET.Element, tag: str) -> str:
    el = row.find(tag)
    return (el.text or "").strip() if el is not None and (el.text or "").strip() else ""


def main() -> int:
    lines = C_SOURCE.read_text(encoding="utf-8", errors="replace").splitlines()
    node_name, node_of_quest = {}, collections.defaultdict(list)
    for i, line in enumerate(lines, 1):
        m = NODE_RE.search(line)
        if m:
            node_name[m.group(1)] = m.group(2)
            node_of_quest[num(m.group(3))].append((m.group(1), m.group(2)))
    slots = collections.defaultdict(list)
    for i, line in enumerate(lines, 1):
        m = SLOT_RE.search(line)
        if m:
            slots[num(m.group(3))].append((m.group(2), num(m.group(4)), num(m.group(5)), i))

    retention = {}
    for line in RETENTION.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        cells = line.split("\t")
        retention[int(cells[0])] = (cells[1], cells[2] if len(cells) > 2 else "", cells[3] if len(cells) > 3 else "")

    root = ET.parse(TABLE).getroot()
    rows = [e for e in root if e.tag == "id"]
    out_lines = ["# questId\towner\tfamily\treason\txml_in_repo\tnodes\tslot0\tslot3\tstep-shape\t"
                 "camera-guard\tcamera-target\tinvariant"]
    buckets = collections.Counter()
    exceptions = []
    for e in rows:
        qid = int(e.get("id"))
        acquired, reward = text(e, "acquired_npc_name"), text(e, "reward_npc_name")
        relays = [text(e, f"talk_npc{i}") for i in (1, 2, 3) if text(e, f"talk_npc{i}")]
        by_node = collections.defaultdict(list)
        for node, slot, index, ln in slots.get(qid, []):
            by_node[node].append((slot, index, ln))
        registered = {n: name for n, name in node_of_quest.get(qid, [])}
        slot0 = [name for node, name in registered.items()
                 if any(s == 0 for s, _, _ in by_node.get(node, []))]
        slot4 = [name for node, name in registered.items()
                 if any(s == 4 for s, _, _ in by_node.get(node, []))]
        steps = {}
        for node, name in registered.items():
            for slot, index, _ in by_node.get(node, []):
                if slot == 3:
                    steps[index] = name
        want = {k: relays[k] for k in range(len(relays))}
        want[min(len(relays) + 1, 3)] = reward
        guard, target = camera_of(lines, qid)
        camera_ok = (guard == len(relays) and target == len(relays) + 1) or (
            guard is None and target is None and qid in NO_CAMERA_ROWS)
        shape_ok = steps == want and slot0 == [acquired] and slot4 == [reward] and camera_ok
        if not shape_ok:
            exceptions.append((qid, acquired, reward, relays, steps, want, slot0, slot4, guard, target))
        owner, family, reason = retention.get(qid, ("(absent)", "", ""))
        buckets[owner] += 1
        out_lines.append("\t".join([
            str(qid), owner, family or "-", reason or "-",
            "yes" if (XML_DIR / f"{qid}.xml").exists() else "no",
            "|".join(f"{n}:{s}" for n, s in sorted(registered.items(), key=lambda kv: kv[1])),
            "|".join(slot0) or "-", "|".join(f"{i}->{n}" for i, n in sorted(steps.items())) or "-",
            "|".join(f"{i}->{n}" for i, n in sorted(want.items())),
            "-" if guard is None else str(guard), "-" if target is None else str(target),
            "OK" if shape_ok else "MISMATCH",
        ]))
    OUT.write_text("\n".join(out_lines) + "\n", encoding="utf-8")

    print(f"# rows={len(rows)} owner 分布={dict(buckets)}")
    print(f"# 不变量例外={len(exceptions)}（0 = 43 行全部满足 slot0 / slot3#K / slot4 / 相机闸门规则）")
    for qid, acq, rew, relays, steps, want, s0, s4, guard, target in exceptions:
        print(f"  {qid}: acquired={acq} reward={rew} relays={relays} 实测={steps} 期望={want} "
              f"slot0={s0} slot4={s4} camera=({guard},{target})")
    print(f"# 证据写入 {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
