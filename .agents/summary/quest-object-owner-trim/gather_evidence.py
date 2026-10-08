#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""物件 owner（QE-052 机型变体）同型收口 —— 全库扫描 + 逐候选证据抽取。

Object-owner (QE-052 machine variant) trim sweep — repository scan + per-candidate evidence.

口径 / Caliber:
- 候选 = 任务 XML 里 `<npc-complete npc-id="X">` 的 X 是「交互物件」：npc_template 的
  `ai="quest_use_item"`，或 `tribe` 以 `FIELD_OBJECT` 开头。物件在客户端没有对话 html
  （name_desc 无对应 Dialogs 文件），任何以它为目标对象的发页都会 load fail（3036 同型）。
- 证据分三层：
  1) 客户端任务书 `Dialogs/QUEST_Q<id>.html`（大写）：quest_summary 行 → 领奖行点名的 NPC
     （`[%dic:STR_DIC_N_*]` 键）；缺失表示该任务没有可见任务书行。
  2) 客户端对话页 `Dialogs/<数字段>/quest_q<id>.html`（小写）：该任务自身可被加载的页集合。
  3) legacy handler（`7e9f0316c^` 的 `src/main/java/com/aionemu/gameserver/quest/handlers/**/_<id>*.java`）：
     谁 `setStatus(REWARD)` / `sendQuestEndDialog`。
  4) 真端 `ScriptDLL64.dll` 反编译仅按需人工取证（本脚本不解析二进制）。

输出 / Output:
- scan-object-owners.tsv：全部候选（quest / object / object ai / tribe / owners）。
- evidence-<quest>.txt：单任务证据正文（客户端任务书行、可加载页、legacy 路径）。

外部根解析 / External root resolution:
  解包根按 ENVIRONMENT.md 的同宿主目录约定取 `<仓库根>/../PycharmProjects/unpak`；
  本机实际在 `$HOME/PycharmProjects/unpak`，故按候选顺序回退（可用 --unpack 显式覆盖）。
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").exists())
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
NPCS = REPO / "src/main/resources/aion/data/static_data/npcs"
LEGACY_TREE = "7e9f0316c^"  # 1500 个 Java handler 迁移成 typed 定义前的最后一个提交

OWNER_ELEMENTS = ("npc-complete", "npc-item-report", "npc-reach-target", "npc-lost-target",
                  "npc-hp-below-percent")
DIALOG_ELEMENTS = ("NPC_START", "NPC_REPORT")


def resolve_unpack(explicit: str | None) -> Path | None:
    candidates = []
    if explicit:
        candidates.append(Path(explicit))
    candidates += [REPO.parent / "PycharmProjects/unpak", Path.home() / "PycharmProjects/unpak"]
    for candidate in candidates:
        if (candidate / "data_unpacked/Dialogs").is_dir():
            return candidate
    return None


def npc_index() -> dict[int, dict[str, str]]:
    index: dict[int, dict[str, str]] = {}
    for path in sorted(NPCS.glob("npc_template_*.xml")):
        # 模板文件是「一行一 npc」的紧凑格式，直接按行取属性。
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            if not line.startswith("<npc_template "):
                continue
            npc_id = re.search(r'npc_id="(\d+)"', line)
            if not npc_id:
                continue
            index[int(npc_id.group(1))] = {
                "ai": (re.search(r'\bai="([^"]*)"', line) or [None, ""])[1],
                "tribe": (re.search(r'\btribe="([^"]*)"', line) or [None, ""])[1],
                "name_desc": (re.search(r'\bname_desc="([^"]*)"', line) or [None, ""])[1],
            }
    return index


def quest_owners(xml: str) -> dict[str, list[int]]:
    owners: dict[str, list[int]] = {}
    for element in OWNER_ELEMENTS:
        for match in re.finditer(rf'<{element} npc-id="(\d+)"', xml):
            owners.setdefault(element, []).append(int(match.group(1)))
    for page_type in DIALOG_ELEMENTS:
        for match in re.finditer(rf'<dialog type="{page_type}" npc-id="(\d+)"', xml):
            owners.setdefault(f"dialog:{page_type}", []).append(int(match.group(1)))
    return owners


def is_object(npc: dict[str, str] | None) -> bool:
    if not npc:
        return False
    return npc["ai"] == "quest_use_item" or npc["tribe"].startswith("FIELD_OBJECT")


def scan(limit: int | None) -> list[dict]:
    npcs = npc_index()
    rows = []
    files = sorted(QUESTS.glob("*.xml"))
    for path in files:
        quest_id = int(path.stem)
        if limit and quest_id not in limit:
            continue
        xml = path.read_text(encoding="utf-8")
        owners = quest_owners(xml)
        completes = owners.get("npc-complete", [])
        object_completes = [npc for npc in completes if is_object(npcs.get(npc))]
        if not object_completes:
            continue
        rows.append({
            "quest": quest_id,
            "path": path,
            "objects": object_completes,
            "owners": owners,
            "npcs": npcs,
        })
    return rows


def client_evidence(unpack: Path | None, quest_id: int) -> tuple[bool, str, list[str]]:
    """返回 (任务书是否存在, quest_summary 行文本列表, 该任务可加载页名列表)。"""
    steps: list[str] = []
    pages: list[str] = []
    has_journal = False
    if unpack:
        journal = unpack / f"data_unpacked/Dialogs/QUEST_Q{quest_id}.html"
        if journal.is_file():
            has_journal = True
            text = journal.read_text(encoding="utf-8", errors="replace")
            summary = re.search(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', text, re.S)
            if summary:
                steps = [re.sub(r"\s+", " ", s).strip()
                         for s in re.findall(r"<step>(.*?)</step>", summary.group(1), re.S)]
        quest_html = next(iter(sorted(unpack.glob(f"data_unpacked/Dialogs/*/quest_q{quest_id}.html"))), None)
        if quest_html is not None:
            text = quest_html.read_text(encoding="utf-8", errors="replace")
            pages = sorted(set(re.findall(r'<HtmlPage name="([^"]+)"', text)))
    return has_journal, steps, pages


def legacy_path(quest_id: int) -> str | None:
    out = subprocess.run(["git", "ls-tree", "-r", "--name-only", LEGACY_TREE], cwd=REPO,
                         capture_output=True, text=True).stdout
    hits = [line for line in out.splitlines()
            if re.search(rf"/_{quest_id}[A-Za-z]|/{quest_id}[A-Za-z].*\.java$", line)
            and "/quest/handlers/" in line]
    return hits[0] if hits else None


def legacy_body(path: str) -> str:
    return subprocess.run(["git", "show", f"{LEGACY_TREE}:{path}"], cwd=REPO,
                          capture_output=True, text=True).stdout


def dialog_key_npcs(unpack: Path | None, keys: list[str], npcs: dict[int, dict[str, str]]) -> dict[str, list[int]]:
    """把 quest_summary 行里的 [%dic:STR_DIC_N_<name_desc>] 键解析回 NPC id。"""
    resolved: dict[str, list[int]] = {}
    for key in keys:
        name_desc = key.removeprefix("STR_DIC_N_")
        resolved[key] = [npc_id for npc_id, npc in npcs.items() if npc["name_desc"] == name_desc]
    return resolved


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--quest", type=int, action="append", default=None)
    parser.add_argument("--unpack", default=None)
    parser.add_argument("--evidence-dir", default=str(Path(__file__).parent))
    args = parser.parse_args()

    unpack = resolve_unpack(args.unpack)
    limit = set(args.quest) if args.quest else None
    rows = scan(limit)
    out_dir = Path(args.evidence_dir)
    tsv = out_dir / "scan-object-owners.tsv"
    header = "quest\tobjects\tobject_ai_tribe\tall_owners\n"
    with tsv.open("w", encoding="utf-8") as handle:
        handle.write("quest\tobject_npc\tobject_ai\tobject_tribe\tobject_name_desc\tjournal\treward_rows\tloadable_pages\tlegacy_handler\n")
        for row in rows:
            npcs = row["npcs"]
            has_journal, steps, pages = client_evidence(unpack, row["quest"])
            legacy = legacy_path(row["quest"]) or ""
            for obj in row["objects"]:
                npc = npcs.get(obj) or {"ai": "?", "tribe": "?", "name_desc": "?"}
                handle.write("\t".join([
                    str(row["quest"]), str(obj), npc["ai"], npc["tribe"], npc["name_desc"],
                    "yes" if has_journal else "NO",
                    " | ".join(steps) if steps else "-",
                    ", ".join(pages) if pages else "-",
                    legacy,
                ]) + "\n")
            evidence = out_dir / f"evidence-{row['quest']}.txt"
            with evidence.open("w", encoding="utf-8") as doc:
                doc.write(f"# quest {row['quest']} （{row['path'].name}）\n\n")
                doc.write("## 服务端 owner 面 / server owners\n")
                for kind, ids in row["owners"].items():
                    for npc_id in ids:
                        npc = npcs.get(npc_id) or {}
                        doc.write(f"- {kind}: {npc_id} ai={npc.get('ai', '?')} "
                                  f"tribe={npc.get('tribe', '?')} name_desc={npc.get('name_desc', '?')}\n")
                doc.write("\n## 客户端任务书 / client journal\n")
                doc.write(f"- QUEST_Q{row['quest']}.html: {'存在' if has_journal else '缺失'}\n")
                for step in steps:
                    doc.write(f"  - {step}\n")
                keys = sorted({k for step in steps for k in re.findall(r"\[%dic:([A-Za-z0-9_]+)\]", step)})
                for key, npc_ids in dialog_key_npcs(unpack, keys, npcs).items():
                    doc.write(f"  - {key} -> {npc_ids or '未解析'}\n")
                doc.write("\n## 客户端可加载页 / loadable quest pages\n")
                doc.write(", ".join(pages) if pages else "(无 quest_q<id>.html)")
                doc.write("\n\n## legacy handler\n")
                if legacy:
                    doc.write(f"`{legacy}` @ {LEGACY_TREE}\n\n```java\n{legacy_body(legacy)}\n```\n")
                else:
                    doc.write("(未找到)\n")
    print(f"candidates: {len(rows)} (quests with an object completion owner)")
    for row in rows:
        print(f"  {row['quest']}: objects={row['objects']} "
              f"owners={sorted({n for ids in row['owners'].values() for n in ids})}")
    print(f"wrote {tsv}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
