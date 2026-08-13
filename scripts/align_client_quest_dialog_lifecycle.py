#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import re
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass
from pathlib import Path

from quest_dialog_symbols import attributes


ACCEPT_SIMPLE_ACTIONS = frozenset({20000, 20001})
REPORT_ACTION = 1009
DIALOG_PATTERN = re.compile(r"<dialog\b[^>]*(?:/>|>.*?</dialog>)", re.DOTALL)


@dataclass(frozen=True)
class ClientPage:
    page_id: int
    page_name: str
    actions: frozenset[int]
    source_file: str
    source_sha256: str


@dataclass(frozen=True)
class ClientAction:
    action_id: int
    action_name: str


@dataclass(frozen=True)
class LegacyContract:
    template_type: str
    start_npcs: frozenset[int]
    end_npcs: frozenset[int]
    report_source_status: str
    report_action_id: int
    report_target_status: str
    source_git_object: str
    source_sha256: str


@dataclass(frozen=True)
class Alignment:
    quest_id: int
    quest_xml: str
    quest_xml_sha256: str
    route_type: str
    npc_id: int
    source_node: str
    source_status: str
    target_node: str
    target_status: str
    actual_page: str
    expected_page: str
    expected_page_id: int
    client_actions: str
    client_source_file: str
    client_source_sha256: str
    legacy_template_type: str
    legacy_source_git_object: str
    legacy_source_sha256: str
    audit_status: str
    fix_status: str
    unresolved_reason: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Align unambiguous quest lifecycle pages with active client HTML.")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--pages", type=Path,
                        default=Path("docs/quest/client-dialog-mapping/quest-dialog-pages.csv"))
    parser.add_argument("--actions", type=Path,
                        default=Path("docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"))
    parser.add_argument("--contracts", type=Path,
                        default=Path("docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv"))
    parser.add_argument("--output", type=Path,
                        default=Path("docs/quest/client-dialog-mapping/client-lifecycle-alignment.csv"))
    parser.add_argument("--write", action="store_true", help="Apply READY page replacements after hash revalidation.")
    parser.add_argument("--check", action="store_true", help="Fail if READY mismatches remain or the report is stale.")
    return parser.parse_args()


def rooted(root: Path, path: Path) -> Path:
    return path if path.is_absolute() else root / path


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def integer_set(value: str) -> frozenset[int]:
    return frozenset(int(token) for token in value.split() if token)


def read_client_pages(page_path: Path, action_path: Path) -> dict[int, tuple[ClientPage | None, ClientPage | None]]:
    page_rows: dict[tuple[int, int], dict[str, str]] = {}
    source_identities: dict[int, set[tuple[str, str]]] = {}
    ambiguous_quests: set[int] = set()
    with page_path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] != "active" or row["page_mapping"] != "exact":
                continue
            quest_id = int(row["quest_id"])
            source_identities.setdefault(quest_id, set()).add(
                (row["source_file"], row["source_sha256"])
            )
            key = (quest_id, int(row["page_id"]))
            previous = page_rows.get(key)
            source_identity = ("source_file", "source_sha256", "page_constant")
            if previous is not None:
                same_source = all(previous[field] == row[field] for field in source_identity)
                repeated_terminal = (same_source
                                     and previous.get("action_count") == "0"
                                     and row.get("action_count") == "0")
                if not repeated_terminal:
                    ambiguous_quests.add(quest_id)
                continue
            page_rows.setdefault(key, row)

    ambiguous_quests.update(
        quest_id for quest_id, identities in source_identities.items()
        if len(identities) != 1
    )

    actions: dict[tuple[int, int], list[ClientAction]] = {key: [] for key in page_rows}
    with action_path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if (row["source_variant"] != "active" or row["page_mapping"] != "exact"
                    or row["action_mapping"] != "exact"):
                continue
            key = (int(row["quest_id"]), int(row["page_id"]))
            if key in actions:
                page_row = page_rows[key]
                if (row.get("source_file", page_row["source_file"]) != page_row["source_file"]
                        or row.get("source_sha256", page_row["source_sha256"]) != page_row["source_sha256"]):
                    ambiguous_quests.add(key[0])
                    continue
                actions[key].append(ClientAction(
                    action_id=int(row["action_id"]),
                    action_name=row["action_constant"].removeprefix("HACTION_"),
                ))

    by_quest: dict[int, list[ClientPage]] = {}
    for key, row in page_rows.items():
        quest_id, page_id = key
        by_quest.setdefault(quest_id, []).append(ClientPage(
            page_id=page_id,
            page_name=row["page_constant"].removeprefix("HTML_PAGE_"),
            actions=frozenset(action.action_id for action in actions[key]),
            source_file=row["source_file"],
            source_sha256=row["source_sha256"],
        ))

    result: dict[int, tuple[ClientPage | None, ClientPage | None]] = {}
    for quest_id, pages in by_quest.items():
        if quest_id in ambiguous_quests:
            continue
        pages_by_name = {page.page_name: page for page in pages}
        pages_by_id = {page.page_id: page for page in pages}
        predecessors: dict[str, set[str]] = {page.page_name: set() for page in pages}
        for page in pages:
            for action in actions[(quest_id, page.page_id)]:
                target = pages_by_name.get(action.action_name)
                if target is None and action.action_id == 1007:
                    target = pages_by_id.get(4)
                if target is not None:
                    predecessors[target.page_name].add(page.page_name)
        accepting = [page for page in pages if page.actions.intersection({1002, 20000})]
        acquisition: set[str] = {page.page_name for page in accepting}
        pending = list(acquisition)
        while pending:
            current = pending.pop()
            for predecessor in predecessors[current]:
                if predecessor not in acquisition:
                    acquisition.add(predecessor)
                    pending.append(predecessor)
        roots = [pages_by_name[name] for name in acquisition if not predecessors[name].intersection(acquisition)]
        report_actions = [page for page in pages if REPORT_ACTION in page.actions]
        reporting: set[str] = {page.page_name for page in report_actions}
        pending = list(reporting)
        while pending:
            current = pending.pop()
            for predecessor in predecessors[current]:
                if predecessor not in acquisition and predecessor not in reporting:
                    reporting.add(predecessor)
                    pending.append(predecessor)
        report_roots = [pages_by_name[name] for name in reporting
                        if not predecessors[name].intersection(reporting)]
        result[quest_id] = (roots[0] if len(accepting) == 1 and len(roots) == 1 else None,
                            report_roots[0] if len(report_actions) == 1 and len(report_roots) == 1 else None)
    return result


def read_legacy_contracts(path: Path) -> dict[int, LegacyContract]:
    grouped: dict[int, list[LegacyContract]] = {}
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["contract_scope"] != "FULL":
                continue
            contract = LegacyContract(
                template_type=row["template_type"],
                start_npcs=integer_set(row["start_npc_ids"]),
                end_npcs=integer_set(row["end_npc_ids"]),
                report_source_status=row["report_source_status"],
                report_action_id=int(row["report_action_id"] or 0),
                report_target_status=row["report_target_status"],
                source_git_object=row["source_git_object"],
                source_sha256=row["source_sha256"],
            )
            grouped.setdefault(int(row["quest_id"]), []).append(contract)
    return {quest_id: contracts[0] for quest_id, contracts in grouped.items() if len(contracts) == 1}


def route_alignment(
    quest_id: int,
    relative: str,
    source_hash: str,
    element: ET.Element,
    statuses: dict[str, str],
    page: ClientPage,
    contract: LegacyContract | None,
) -> Alignment:
    route_type = element.get("type", "")
    npc_id = int(element.get("npc-id", "0"))
    source = element.get("source", "")
    target = element.get("target", "")
    page_attribute = "start-page" if route_type == "NPC_START" else "page"
    actual = element.get(page_attribute, "")
    expected = page.page_name
    legacy_npcs = (contract.start_npcs if route_type == "NPC_START" else contract.end_npcs) if contract else frozenset()
    if contract is None:
        audit_status = "EVIDENCE_REQUIRED"
        fix_status = "EVIDENCE_REQUIRED"
        reason = "quest has no unique full legacy template contract"
    elif npc_id not in legacy_npcs:
        audit_status = "EVIDENCE_REQUIRED"
        fix_status = "EVIDENCE_REQUIRED"
        reason = "current NPC is absent from the legacy template contract"
    elif (route_type == "NPC_REPORT"
          and (contract.report_action_id != REPORT_ACTION
               or statuses.get(source, "") != contract.report_source_status
               or statuses.get(target, "") != contract.report_target_status)):
        audit_status = "EVIDENCE_REQUIRED"
        fix_status = "EVIDENCE_REQUIRED"
        reason = "current report action or status timing differs from the legacy template contract"
    elif actual == expected:
        audit_status = "CLIENT_LIFECYCLE_ALIGNED"
        fix_status = "NOT_NEEDED"
        reason = ""
    else:
        audit_status = "CLIENT_LIFECYCLE_PAGE_MISMATCH"
        fix_status = "READY"
        reason = "active client has one lifecycle page exposing the required protocol action"
    return Alignment(
        quest_id=quest_id,
        quest_xml=relative,
        quest_xml_sha256=source_hash,
        route_type=route_type,
        npc_id=npc_id,
        source_node=source,
        source_status=statuses.get(source, ""),
        target_node=target,
        target_status=statuses.get(target, ""),
        actual_page=actual,
        expected_page=expected,
        expected_page_id=page.page_id,
        client_actions=" ".join(str(action) for action in sorted(page.actions)),
        client_source_file=page.source_file,
        client_source_sha256=page.source_sha256,
        legacy_template_type=contract.template_type if contract else "",
        legacy_source_git_object=contract.source_git_object if contract else "",
        legacy_source_sha256=contract.source_sha256 if contract else "",
        audit_status=audit_status,
        fix_status=fix_status,
        unresolved_reason=reason,
    )


def scan(
    root: Path,
    client_pages: dict[int, tuple[ClientPage | None, ClientPage | None]],
    contracts: dict[int, LegacyContract],
) -> list[Alignment]:
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    rows: list[Alignment] = []
    for path in sorted(quest_dir.glob("*.xml"), key=lambda candidate: int(candidate.stem)):
        quest_id = int(path.stem)
        candidates = client_pages.get(quest_id)
        if candidates is None:
            continue
        content = path.read_bytes()
        document = ET.fromstring(content)
        statuses = {node.get("label", ""): node.get("status", "") for node in document.findall("./nodes/node")}
        contract = contracts.get(quest_id)
        for element in document.findall("./transitions/dialog"):
            route_type = element.get("type")
            page = candidates[0] if route_type == "NPC_START" else candidates[1] if route_type == "NPC_REPORT" else None
            if page is None:
                continue
            rows.append(route_alignment(
                quest_id, path.relative_to(root).as_posix(), digest(content), element, statuses, page, contract
            ))
    return sorted(rows, key=lambda row: (row.quest_id, row.route_type, row.npc_id, row.source_node, row.target_node))


def replace_ready(root: Path, rows: list[Alignment]) -> int:
    grouped: dict[str, list[Alignment]] = {}
    for row in rows:
        if row.fix_status == "READY":
            grouped.setdefault(row.quest_xml, []).append(row)
    changed = 0
    for relative, replacements in grouped.items():
        path = root / relative
        content = path.read_bytes()
        expected_hashes = {replacement.quest_xml_sha256 for replacement in replacements}
        if expected_hashes != {digest(content)}:
            raise RuntimeError(f"concurrent change detected: {path}")
        source = content.decode("utf-8")
        remaining = list(replacements)

        def replace(match: re.Match[str]) -> str:
            nonlocal changed
            attrs = attributes(match.group(0))
            for index, replacement in enumerate(remaining):
                if (attrs.get("type") == replacement.route_type
                        and attrs.get("npc-id") == str(replacement.npc_id)
                        and attrs.get("source") == replacement.source_node
                        and attrs.get("target") == replacement.target_node):
                    attribute = "start-page" if replacement.route_type == "NPC_START" else "page"
                    if attrs.get(attribute) != replacement.actual_page:
                        continue
                    updated = re.sub(
                        rf'(\b{re.escape(attribute)}\s*=\s*")[^"]*(")',
                        rf'\g<1>{replacement.expected_page}\g<2>', match.group(0), count=1
                    )
                    if updated == match.group(0):
                        raise RuntimeError(f"failed to replace {attribute} in {path}")
                    remaining.pop(index)
                    changed += 1
                    return updated
            return match.group(0)

        updated = DIALOG_PATTERN.sub(replace, source)
        if remaining:
            raise RuntimeError(f"could not locate {len(remaining)} expected dialog routes in {path}")
        if digest(path.read_bytes()) not in expected_hashes:
            raise RuntimeError(f"concurrent change detected before write: {path}")
        path.write_text(updated, encoding="utf-8")
    return changed


def render(rows: list[Alignment]) -> bytes:
    stream = io.StringIO(newline="")
    fields = list(Alignment.__dataclass_fields__)
    writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
    writer.writeheader()
    writer.writerows(asdict(row) for row in rows)
    return b"\xef\xbb\xbf" + stream.getvalue().encode("utf-8")


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    pages = read_client_pages(rooted(root, args.pages), rooted(root, args.actions))
    contracts = read_legacy_contracts(rooted(root, args.contracts))
    rows = scan(root, pages, contracts)
    if args.write:
        changed = replace_ready(root, rows)
        rows = scan(root, pages, contracts)
        print(f"changed_routes={changed}")
    generated = render(rows)
    output = rooted(root, args.output)
    if args.check:
        ready = [row for row in rows if row.fix_status == "READY"]
        if ready:
            raise SystemExit(f"client lifecycle page mismatches remain: {len(ready)}")
        if not output.is_file() or output.read_bytes() != generated:
            raise SystemExit(f"client lifecycle alignment report is stale: {output}")
        return 0
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(generated)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
