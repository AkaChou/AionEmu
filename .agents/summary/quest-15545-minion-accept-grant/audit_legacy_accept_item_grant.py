"""Audit accept-time quest item grants: legacy Java handlers vs typed definitions.

只读审计：把 7e9f0316c^（Java handler 存在时的最后一版）中“接受任务时 giveQuestItem”
的行为，与当前 production quest_definition XML 的 give-item / accept-actions 对比。
Read-only audit: compare accept-time giveQuestItem calls in the last Java-handler revision
with the give-item / accept-actions declared by the current typed definitions.
"""
from __future__ import annotations

import re
import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
REV = "7e9f0316c^"
DEF_DIR = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

GIVE_CALL = re.compile(r"giveQuestItem\s*\(\s*env\s*,\s*(\d+)\s*(?:,\s*([0-9]+))?")
QUEST_ID_DECL = re.compile(r"questId\s*=\s*(\d+)")
HANDLER_PATH = re.compile(r"^[^:]+:(src/main/java/com/aionemu/gameserver/quest/handlers/\S+\.java):(\d+):(.*)$")


def git(*args: str) -> str:
    return subprocess.run(["git", *args], cwd=REPO, check=True, capture_output=True, text=True).stdout


def read_lines(rel_path: str) -> list[str]:
    return git("show", f"{REV}:{rel_path}").splitlines()


def quest_id_of(rel_path: str) -> int | None:
    try:
        text = git("show", f"{REV}:{rel_path}")
    except subprocess.CalledProcessError:
        return None
    match = QUEST_ID_DECL.search(text)
    return int(match.group(1)) if match else None


def legacy_accept_grants() -> dict[int, list[tuple[int, int, str]]]:
    """questId -> [(itemId, count, evidence)] for accept-branch giveQuestItem calls."""
    grants: dict[int, list[tuple[int, int, str]]] = {}
    # 直接从 git 历史取旧 handler 的 giveQuestItem 调用，脚本自洽、不依赖中间导出文件。
    # Read the legacy giveQuestItem calls straight from git so the script stays self-contained.
    raw = git("grep", "-n", "giveQuestItem", REV, "--",
              "src/main/java/com/aionemu/gameserver/quest/handlers")
    per_file: dict[str, list[tuple[int, str]]] = {}
    for line in raw.splitlines():
        match = HANDLER_PATH.match(line)
        if not match:
            continue
        rel_path, lineno, code = match.group(1), int(match.group(2)), match.group(3)
        per_file.setdefault(rel_path, []).append((lineno, code))
    for rel_path, calls in per_file.items():
        qid = quest_id_of(rel_path)
        if qid is None:
            continue
        lines = read_lines(rel_path)
        for lineno, code in calls:
            call = GIVE_CALL.search(code)
            if not call:
                continue
            item_id = int(call.group(1))
            count = int(call.group(2) or 1)
            # 最近的 dialog 分支（case X: 或 QuestDialog.X）才是该发放的真实上下文；
            # 不能用固定窗口，否则会把上一条 ACCEPT 分支后的其它步骤误判为接取发放。
            # The nearest dialog branch (case X: or QuestDialog.X) is the real context of the grant;
            # a fixed window would misclassify later steps that follow an ACCEPT branch.
            context = ""
            for line in reversed(lines[max(0, lineno - 40):lineno]):
                match = re.search(r"case\s+([A-Z_0-9]+)\s*:|QuestDialog\.([A-Z_0-9]+)", line)
                if match:
                    context = (match.group(1) or match.group(2)).strip()
                    break
            if "ACCEPT" not in context.upper():
                continue
            grants.setdefault(qid, []).append((item_id, count, f"{rel_path}:{lineno} {context}"))
    return grants


def definition_grants() -> dict[int, dict[str, set[int]]]:
    result: dict[int, dict[str, set[int]]] = {}
    for path in sorted(DEF_DIR.glob("*.xml")):
        text = path.read_text(encoding="utf-8")
        match = re.search(r'quest-definition id="(\d+)"', text)
        if not match:
            continue
        qid = int(match.group(1))
        all_give = {int(x) for x in re.findall(r'<give-item item-id="(\d+)"', text)}
        accept_give: set[int] = set()
        for block in re.findall(r"<accept-actions>(.*?)</accept-actions>", text, re.S):
            accept_give |= {int(x) for x in re.findall(r'<give-item item-id="(\d+)"', block)}
        result[qid] = {"all": all_give, "accept": accept_give}
    return result


def main() -> None:
    legacy = legacy_accept_grants()
    defs = definition_grants()
    rows = []
    for qid in sorted(legacy):
        grants = legacy[qid]
        definition = defs.get(qid)
        items = sorted({g[0] for g in grants})
        missing = [i for i in items if not definition or i not in definition["all"]]
        rows.append((qid, items, missing, grants, definition))
    out_path = OUT / "legacy-accept-item-grant-audit.tsv"
    with out_path.open("w", encoding="utf-8") as handle:
        handle.write("quest_id\tlegacy_accept_items\tdefinition_give_items\tdefinition_accept_items\tverdict\tevidence\n")
        for qid, items, missing, grants, definition in rows:
            definition_all = sorted(definition["all"]) if definition else []
            definition_accept = sorted(definition["accept"]) if definition else []
            verdict = "MISSING_DEFINITION_GRANT" if missing else "COVERED"
            evidence = " | ".join(g[2] for g in grants)
            handle.write(f"{qid}\t{' '.join(map(str, items))}\t{' '.join(map(str, definition_all))}\t"
                         f"{' '.join(map(str, definition_accept))}\t{verdict}\t{evidence}\n")
    missing_rows = [r for r in rows if r[2]]
    print(f"legacy accept-grant quests: {len(rows)}")
    print(f"missing any definition grant: {len(missing_rows)}")
    for qid, items, missing, _, _ in missing_rows[:60]:
        print(f"  {qid}: legacy={items} missing={missing}")
    print(f"report: {out_path.relative_to(REPO)}")


if __name__ == "__main__":
    main()
