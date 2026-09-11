#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import re
import subprocess
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
AUDIT = Path(".agent/summary/quest-load-fail/quest-order-audit-current.csv")
OUTPUT = Path(".agent/summary/quest-load-fail/legacy-handler-action-contracts.csv")
QUEST_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
HANDLER_PREFIX = "src/main/java/com/aionemu/gameserver/quest/handlers"


@dataclass(frozen=True)
class HandlerBranch:
    status: str
    npc_id: int
    response_page: int
    response_kind: str
    effects: str
    snippet_sha256: str


@dataclass(frozen=True)
class Contract:
    quest_id: int
    source_state: str
    source_status: str
    npc_id: int
    action_id: int
    action_name: str
    handler_path: str
    branch_count: int
    response_page: int
    response_kind: str
    effects: str
    evidence_status: str
    unresolved_reason: str
    source_git_object: str
    source_sha256: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Extract legacy Java handler action contracts for unresolved buttons.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path, default=AUDIT)
    parser.add_argument("--output", type=Path, default=OUTPUT)
    return parser.parse_args()


def git_output(root: Path, args: list[str]) -> str:
    return subprocess.check_output(["git", *args], cwd=root, text=True)


def handler_paths(root: Path) -> dict[int, str]:
    lines = git_output(root, [
        "grep", "-n", "-E", r"questId[[:space:]]*=", "origin/history", "--", HANDLER_PREFIX,
    ]).splitlines()
    result: dict[int, str] = {}
    for line in lines:
        match = re.match(r"origin/history:(.*?):\d+:.*questId\s*=\s*(\d+)", line)
        if match is not None:
            result[int(match.group(2))] = match.group(1)
    return result


def dialog_actions(root: Path) -> dict[int, str]:
    source = (root / "src/main/java/com/aionemu/gameserver/questEngine/model/QuestDialog.java").read_text(
        encoding="utf-8")
    return {int(value): name for name, value in re.findall(r"\b([A-Z][A-Z0-9_]+)\((-?\d+)\)", source)}


def quest_node_status(root: Path, quest_id: int) -> dict[str, str]:
    path = root / QUEST_DIR / f"{quest_id}.xml"
    document = ET.parse(path).getroot()
    return {node.get("label", ""): node.get("status", "") for node in document.findall("./nodes/node")}


def client_pages(root: Path) -> dict[int, set[int]]:
    result: dict[int, set[int]] = defaultdict(set)
    path = root / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] == "active" and row["page_mapping"] == "exact":
                result[int(row["quest_id"])].add(int(row["page_id"]))
    return result


def branch_ranges(lines: list[str], action_name: str, action_id: int, target_line: int | None = None
        ) -> list[tuple[int, int]]:
    needles = [f"case {action_name}:", f"QuestDialog.{action_name}"]
    if action_id >= 0:
        needles.append(f"getDialogId() == {action_id}")
    starts: list[int] = []
    for index, line in enumerate(lines):
        if any(needle in line for needle in needles):
            starts.append(index)
    ranges: list[tuple[int, int]] = []
    for start in starts:
        if target_line is not None and abs(start - target_line) > 120:
            continue
        indent = len(lines[start]) - len(lines[start].lstrip())
        end = min(len(lines), start + 160)
        for index in range(start + 1, end):
            stripped = lines[index].lstrip()
            current_indent = len(lines[index]) - len(stripped)
            if current_indent <= indent and (
                    stripped.startswith("case ") or stripped.startswith("else if (")
                    or stripped.startswith("else {") or stripped == "}"):
                end = index
                break
        ranges.append((start, end))
    return ranges


def nearest_status(lines: list[str], start: int) -> str:
    for index in range(start, max(-1, start - 220), -1):
        match = re.search(r"QuestStatus\.(NONE|START|REWARD|COMPLETE)", lines[index])
        if match is not None:
            return match.group(1)
    return ""


def nearest_npc(lines: list[str], start: int) -> int:
    for index in range(start, max(-1, start - 60), -1):
        match = re.search(r"(?:targetId|env\.getTargetId\(\))\s*==\s*(\d+)", lines[index])
        if match is not None:
            return int(match.group(1))
        match = re.search(r"case\s+(\d+)\s*:", lines[index])
        if match is not None and 200000 <= int(match.group(1)) <= 999999:
            return int(match.group(1))
    return 0


def extract_branch(lines: list[str], start: int, end: int) -> HandlerBranch:
    snippet = "\n".join(lines[start:end])
    status = nearest_status(lines, start)
    npc_id = nearest_npc(lines, start)
    pages = sorted({int(value) for value in re.findall(r"sendQuestDialog\([^;]*?\b(\d+)\b\s*\)", snippet)})
    dynamic_page = bool(re.search(r"sendQuestDialog\([^;]*?\b(?!\d+\b)[A-Za-z_]\w*\s*\)", snippet))
    if pages:
        response_page = pages[0]
        response_kind = "PAGE" if len(pages) == 1 and not dynamic_page else "AMBIGUOUS_PAGE"
    elif "sendQuestEndDialog" in snippet:
        response_page = 0
        response_kind = "END_DIALOG"
    elif any(token in snippet for token in ("closeDialogWindow", "defaultCloseDialog", "SM_DIALOG_WINDOW(0, 0)")):
        response_page = 0
        response_kind = "CLOSE"
    else:
        response_page = 0
        response_kind = "NO_RESPONSE"

    effects: list[str] = []
    for token in ("setQuestVar", "setQuestVarById", "setStatus", "changeQuestStep", "updateQuestStatus",
                  "removeQuestItem", "deleteQuestItems", "giveQuestItem", "useQuestItem", "finishQuest",
                  "startQuest", "teleport"):
        if token in snippet:
            effects.append(token)
    return HandlerBranch(status, npc_id, response_page, response_kind, " ".join(effects),
                         hashlib.sha256(snippet.encode("utf-8")).hexdigest())


def classify(contract_branches: list[HandlerBranch], source_status: str, pages: set[int]) -> tuple[str, str]:
    if not contract_branches:
        return "NO_MATCHING_BRANCH", "handler has no matching action occurrence near the audited state"
    by_status = [branch for branch in contract_branches if branch.status == source_status]
    candidates = by_status or contract_branches
    signatures = {(branch.response_page, branch.response_kind, branch.effects) for branch in candidates}
    if len(signatures) != 1:
        return "AMBIGUOUS", "matching handler branches disagree on response or side effects"
    branch = candidates[0]
    if branch.response_kind == "PAGE":
        if branch.response_page in pages:
            return "READY", ""
        return "STALE_PAGE", f"handler page {branch.response_page} is absent from the active quest HTML"
    if branch.response_kind == "AMBIGUOUS_PAGE":
        return "AMBIGUOUS", "handler response page is dynamic or multiple pages share the branch"
    if branch.response_kind in {"CLOSE", "END_DIALOG"}:
        return "READY", ""
    return "AMBIGUOUS", "handler branch does not expose a concrete response"


def collect(args: argparse.Namespace) -> list[Contract]:
    root = args.root.resolve()
    audit = args.audit if args.audit.is_absolute() else root / args.audit
    paths = handler_paths(root)
    actions = dialog_actions(root)
    pages_by_quest = client_pages(root)
    statuses: dict[int, dict[str, str]] = {}
    contracts: list[Contract] = []

    with audit.open(encoding="utf-8-sig", newline="") as stream:
        rows = [row for row in csv.DictReader(stream)
                if row["audit_status"] == "EVIDENCE_REQUIRED"
                and row["unresolved_reason"].startswith("visible client action has no route")]
    for row in rows:
        quest_id = int(row["quest_id"])
        action_id = int(row["client_visible_action"])
        action_name = actions.get(action_id, "")
        path = paths.get(quest_id, "")
        if not path or not action_name:
            contracts.append(Contract(
                quest_id, row["server_source_state"], "", int(row["npc_id"] or 0), action_id,
                action_name, path, 0, 0, "", "", "NO_HANDLER",
                "quest has no legacy Java handler" if not path else "action id has no QuestDialog name",
                "", "",
            ))
            continue
        payload = git_output(root, ["show", f"origin/history:{path}"])
        git_object = git_output(root, ["rev-parse", f"origin/history:{path}"]).strip()
        digest = hashlib.sha256(payload.encode("utf-8")).hexdigest()
        statuses.setdefault(quest_id, quest_node_status(root, quest_id))
        source_status = statuses[quest_id].get(row["server_source_state"], "")
        branches = [extract_branch(payload.splitlines(), start, end)
                    for start, end in branch_ranges(payload.splitlines(), action_name, action_id)]
        evidence_status, reason = classify(branches, source_status, pages_by_quest[quest_id])
        representative = branches[0] if branches else HandlerBranch("", 0, 0, "", "", "")
        contracts.append(Contract(
            quest_id, row["server_source_state"], source_status, int(row["npc_id"] or 0), action_id,
            action_name, path, len(branches), representative.response_page, representative.response_kind,
            representative.effects, evidence_status, reason,
            git_object, digest,
        ))
    return sorted(contracts, key=lambda contract: (
        contract.quest_id, contract.action_id, contract.npc_id))


def render(contracts: list[Contract]) -> bytes:
    import io
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=list(Contract.__dataclass_fields__), lineterminator="\n")
    writer.writeheader()
    writer.writerows(contract.__dict__ for contract in contracts)
    return stream.getvalue().encode("utf-8")


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    output = args.output if args.output.is_absolute() else root / args.output
    contracts = collect(args)
    output.write_bytes(render(contracts))
    counts: dict[str, int] = defaultdict(int)
    for contract in contracts:
        counts[contract.evidence_status] += 1
    print(f"contracts={len(contracts)} statuses={dict(sorted(counts.items()))} output={output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
