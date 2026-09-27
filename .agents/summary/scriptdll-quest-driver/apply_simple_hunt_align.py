#!/usr/bin/env python3
"""Phase 4-2：把 SimpleHunt 击杀计数器对齐到真端表（表为权威，客户端 CSV 为佐证）。

规则（仅处理「1 个真端计数器 ↔ 1 个仓库 dimension」的安全子集）：
  1. 仓库 dimension 的 npc-ids 必须与真端表所列怪**同源**（交集非空）；
  2. 允许包含客户端 CSV 列的额外怪（CSV 为客户端口径）；
  3. 去掉既不在真端表、也不在客户端 CSV 的 id（多算）；
  4. 补上真端表里有、仓库缺的 id（少算）；
  5. required 改为真端表的 countN。

跳过（另行报告，不做机械改写）：拆分成多个 1 杀 counter、与真端表 id 完全不相交、
链式建模、count>63 的宽字段。

用法：python3 apply_simple_hunt_align.py [--apply]
"""
from __future__ import annotations

import argparse
import importlib.util
import os
import re
import sys
import xml.etree.ElementTree as ET

BASE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(BASE, "../../.."))
QUEST_DIR = os.path.join(REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

spec = importlib.util.spec_from_file_location("recon", os.path.join(BASE, "reconcile_simple_hunt.py"))
R = importlib.util.module_from_spec(spec)
spec.loader.exec_module(R)

DIMENSION_RE = r'<dimension\b[^>]*field="%s"[^>]*/>'


def patch_dimension(text, field, required, npc_ids):
    pattern = re.compile(DIMENSION_RE % re.escape(field))

    def repl(match):
        tag = match.group(0)
        tag = re.sub(r'required="\d+"', f'required="{required}"', tag)
        if 'npc-ids="' in tag:
            tag = re.sub(r'npc-ids="[^"]*"', f'npc-ids="{" ".join(str(i) for i in npc_ids)}"', tag)
        return tag

    return pattern.sub(repl, text, count=1)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    index = R.load_index(False)
    lower = {k.lower(): v for k, v in index.items()}
    retail = R.parse_retail()
    csvx = R.load_client_csv()

    def resolve(names):
        ids = set()
        for n in names:
            ids |= index.get(n) or lower.get(n.lower()) or set()
        return ids

    fixed, skipped = [], []
    for qid, info in sorted(retail.items()):
        repo = R.parse_repo(qid)
        if repo is None:
            continue
        dims_by_field = {}
        for d in repo["dims"]:
            dims_by_field.setdefault(d["field"], []).append(d)
        sixbit = {f["offset"]: f for f in repo["fields"] if f["width"] == 6}
        # 安全闸：所有真端计数器都必须有对应的独立 dimension，否则属混用/合并建模，整体跳过
        partial = False
        for c in info["counters"]:
            f = sixbit.get(6 * (c["n"] - 1))
            if f is None or len(dims_by_field.get(f["name"], [])) != 1:
                partial = True
                break
        if partial:
            skipped.append((qid, 0, "partial-modeling", [], []))
            continue
        changes = []
        for c in info["counters"]:
            n, count = c["n"], c["count"]
            field = sixbit.get(6 * (n - 1))
            if field is None or count is None:
                continue
            dims = dims_by_field.get(field["name"], [])
            if len(dims) != 1:
                continue
            dim = dims[0]
            retail_ids = resolve(c["monsters"])
            slot = csvx.get(qid, {}).get(n - 1)
            csv_ids = resolve(slot["names"]) if slot else set()
            repo_ids = set(dim["npc_ids"])
            if not retail_ids or not (repo_ids & retail_ids):
                skipped.append((qid, n, "ids-disjoint", sorted(repo_ids), sorted(retail_ids)))
                continue
            # 拆分建模：后续 dimension 的 id 全部落在本计数器真端 id 内
            later = []
            for m in range(n + 1, 8):
                f2 = sixbit.get(6 * (m - 1))
                if f2 and dims_by_field.get(f2["name"]):
                    later += list(dims_by_field[f2["name"]][0]["npc_ids"])
            if later and set(later) <= retail_ids:
                skipped.append((qid, n, "split-modeling", sorted(repo_ids), sorted(retail_ids)))
                continue
            if count > 63:
                skipped.append((qid, n, "count>63", [], []))
                continue
            expected = sorted(retail_ids | csv_ids)
            if repo_ids == set(expected) and dim["required"] == count:
                continue
            # 本批只做「删除多算 id」：删 id 不可能引入 KILL_NPC 歧义，
            # 而 required 改动会牵动 counter-grid nodes 阶梯投影（编译期校验），与 id 增补一起另案。
            if dim["required"] != count or (set(expected) - repo_ids):
                skipped.append((qid, n, "needs-ladder-or-addition", sorted(repo_ids), expected))
                continue
            if len(set(expected)) == 0:
                skipped.append((qid, n, "empty-expected", sorted(repo_ids), []))
                continue
            changes.append((field["name"], dim["required"], count,
                            sorted(repo_ids), expected,
                            [i for i in sorted(repo_ids) if i not in expected],
                            [i for i in expected if i not in repo_ids]))
        if not changes:
            continue
        # 护栏：改写后同一 quest 内不得出现 npc-id 跨 dimension 重叠（否则展开为 KILL_NPC 歧义转移）
        proposed = {}
        for d in repo["dims"]:
            proposed[d["field"]] = set(d["npc_ids"])
        for name, _o, _r, _oi, new_ids, _rm, _ad in changes:
            proposed[name] = set(new_ids)
        seen = {}
        clash = False
        for field, ids in proposed.items():
            for npc_id in ids:
                if npc_id in seen:
                    clash = True
                    break
                seen[npc_id] = field
            if clash:
                break
        if clash:
            skipped.append((qid, 0, "id-overlap-after-fix", [], []))
            continue
        path = os.path.join(QUEST_DIR, f"{qid}.xml")
        text = open(path, encoding="utf-8").read()
        new_text = text
        for name, old_req, new_req, old_ids, new_ids, removed, added in changes:
            new_text = patch_dimension(new_text, name, new_req, new_ids)
            fixed.append((qid, name, old_req, new_req, removed, added))
        if new_text != text:
            if args.apply:
                open(path, "w", encoding="utf-8").write(new_text)
            else:
                print(f"[DRY] {qid}: " + " | ".join(
                    f"{n}: required {o}->{r}" + (f" -{rm}" if rm else "") + (f" +{ad}" if ad else "")
                    for n, o, r, _i, _e, rm, ad in changes))

    print()
    print(f"修正计数器: {len(fixed)}（任务 {len({f[0] for f in fixed})}）  "
          f"{'已写入' if args.apply else 'dry-run'}")
    print(f"跳过（需人工/另案）: {len(skipped)}")
    for qid, n, why, repo_ids, retail_ids in skipped[:20]:
        print(f"  SKIP {qid} n{n} {why} repo={repo_ids[:6]} retail={retail_ids[:6]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
