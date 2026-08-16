#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import subprocess
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass
from pathlib import Path


DEFAULT_REVISION = "origin/history"
DEFAULT_RESOURCE = "src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml"
DEFAULT_OUTPUT = "docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv"


@dataclass(frozen=True)
class Contract:
    quest_id: int
    template_type: str
    contract_scope: str
    start_type: str
    start_npc_ids: str
    end_npc_ids: str
    start_page_id: str
    start_page: str
    report_open_action_id: str
    report_open_action: str
    report_page_id: str
    report_page: str
    report_source_status: str
    report_action_id: str
    report_action: str
    report_target_status: str
    reward_page_id: str
    reward_page: str
    progress_vars: str
    progress_npc_ids: str
    progress_page_ids: str
    progress_pages: str
    progress_action_ids: str
    progress_actions: str
    unresolved_reason: str
    source_revision: str
    source_resource: str
    source_git_object: str
    source_sha256: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Extract legacy retail quest dialog contracts.")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--revision", default=DEFAULT_REVISION)
    parser.add_argument("--resource", default=DEFAULT_RESOURCE)
    parser.add_argument("--output", type=Path, default=Path(DEFAULT_OUTPUT))
    parser.add_argument("--check", action="store_true", help="Fail if the generated output differs.")
    return parser.parse_args()


def git_bytes(root: Path, revision: str, resource: str) -> tuple[bytes, str]:
    spec = f"{revision}:{resource}"
    content = subprocess.run(
        ["git", "show", spec], cwd=root, check=True, stdout=subprocess.PIPE
    ).stdout
    git_object = subprocess.run(
        ["git", "rev-parse", spec], cwd=root, check=True, text=True, stdout=subprocess.PIPE
    ).stdout.strip()
    return content, git_object


def ids(element: ET.Element, attribute: str) -> str:
    return " ".join(element.get(attribute, "").split())


def page(value: int) -> tuple[str, str]:
    names = {
        5: "SHOW_SELECT_QUEST_REWARD_WINDOW1",
        1011: "SELECT1",
        1352: "SELECT2",
        1693: "SELECT3",
        2034: "SELECT4",
        2375: "SELECT5",
        4762: "SELECT_NONE",
        10002: "DEFAULT_SUCCESS",
    }
    return str(value), names.get(value, "")


def extract_contracts(
    content: bytes,
    revision: str,
    resource: str,
    git_object: str,
) -> list[Contract]:
    root = ET.fromstring(content)
    digest = hashlib.sha256(content).hexdigest()
    contracts: list[Contract] = []
    for element in root:
        raw_id = element.get("id")
        if raw_id is None:
            continue
        template = element.tag
        start_type = element.get("start_type", "TALK")
        start_npcs = ids(element, "start_ids") if template == "data_driven_quest" else ids(element, "start_npc_ids")
        end_npcs = ids(element, "end_npc_ids")
        if template == "monster_hunt" and not end_npcs:
            end_npcs = start_npcs
        elif template == "item_order" and not end_npcs:
            end_npcs = ids(element, "end_npc_id")

        values: dict[str, str] = {
            "contract_scope": "PARTIAL",
            "start_page_id": "",
            "start_page": "",
            "report_open_action_id": "",
            "report_open_action": "",
            "report_page_id": "",
            "report_page": "",
            "report_source_status": "",
            "report_action_id": "",
            "report_action": "",
            "report_target_status": "",
            "reward_page_id": "",
            "reward_page": "",
            "progress_vars": "",
            "progress_npc_ids": "",
            "progress_page_ids": "",
            "progress_pages": "",
            "progress_action_ids": "",
            "progress_actions": "",
            "unresolved_reason": "template dialog lifecycle is not modeled by this extractor",
        }

        if template == "data_driven_quest" and start_type == "TALK":
            start_page_id, start_page = page(int(element.get("start_dialog_id", "0")) or 4762)
            report_page_id, report_page = page(10002)
            reward_page_id, reward_page = page(5)
            values.update(
                contract_scope="FULL",
                start_page_id=start_page_id,
                start_page=start_page,
                report_open_action_id="31",
                report_open_action="QUEST_SELECT",
                report_page_id=report_page_id,
                report_page=report_page,
                report_source_status="REWARD",
                report_action_id="1009",
                report_action="SELECT_QUEST_REWARD",
                report_target_status="REWARD",
                reward_page_id=reward_page_id,
                reward_page=reward_page,
                unresolved_reason="",
            )
        elif template == "monster_hunt" and element.get("reward", "false").lower() != "true":
            start_page_id, start_page = page(int(element.get("start_dialog_id", "0")) or 1011)
            end_dialog = int(element.get("end_dialog_id", "0"))
            report_page_id, report_page = page(end_dialog or 1352)
            reward_page_id, reward_page = page(5)
            values.update(
                contract_scope="FULL",
                start_page_id=start_page_id,
                start_page=start_page,
                report_open_action_id="31" if end_dialog == 0 else "-1",
                report_open_action="QUEST_SELECT" if end_dialog == 0 else "USE_OBJECT",
                report_page_id=report_page_id,
                report_page=report_page,
                report_source_status="START",
                report_action_id="1009",
                report_action="SELECT_QUEST_REWARD",
                report_target_status="REWARD",
                reward_page_id=reward_page_id,
                reward_page=reward_page,
                unresolved_reason="",
            )
        elif template in {"report_to", "report_to_many"}:
            start_page_id, start_page = page(int(element.get("start_dialog_id", "0")) or 1011)
            report_page_attribute = "end_dialog_id" if template == "report_to_many" else "start_dialog_id2"
            report_page_id, report_page = page(int(element.get(report_page_attribute, "0")) or 2375)
            reward_page_id, reward_page = page(5)
            values.update(
                contract_scope="FULL",
                start_page_id=start_page_id,
                start_page=start_page,
                report_open_action_id="31",
                report_open_action="QUEST_SELECT",
                report_page_id=report_page_id,
                report_page=report_page,
                report_source_status="REWARD" if template == "report_to_many" else "START",
                report_action_id="1009",
                report_action="SELECT_QUEST_REWARD",
                report_target_status="REWARD",
                reward_page_id=reward_page_id,
                reward_page=reward_page,
                unresolved_reason="",
            )
            if template == "report_to_many":
                steps = sorted(element.findall("npc_infos"), key=lambda step: int(step.get("var", "-1")))
                variables = [int(step.get("var", "-1")) for step in steps]
                npc_ids = [int(step.get("npc_id", "0")) for step in steps]
                if variables != list(range(len(steps))) or len(npc_ids) != len(set(npc_ids)):
                    values.update(
                        contract_scope="PARTIAL",
                        unresolved_reason="report_to_many steps are not one contiguous unique-NPC sequence",
                    )
                else:
                    page_ids = [int(step.get("quest_dialog", "0")) for step in steps]
                    action_ids = [int(step.get("close_dialog", "0")) or 10000 + variable
                                  for step, variable in zip(steps, variables)]
                    values.update(
                        progress_vars=" ".join(map(str, variables)),
                        progress_npc_ids=" ".join(map(str, npc_ids)),
                        progress_page_ids=" ".join(map(str, page_ids)),
                        progress_pages=" ".join(page(value)[1] for value in page_ids),
                        progress_action_ids=" ".join(map(str, action_ids)),
                        progress_actions=" ".join(f"SETPRO{value - 9999}" if 10000 <= value <= 10039
                                                  else str(value) for value in action_ids),
                    )
        elif template == "kill_in_world":
            start_page_id, start_page = page(4762)
            report_page_id, report_page = page(int(element.get("reward_dialog_id", "0")))
            reward_page_id, reward_page = page(5)
            values.update(
                contract_scope="FULL",
                start_page_id=start_page_id,
                start_page=start_page,
                report_open_action_id="31",
                report_open_action="QUEST_SELECT",
                report_page_id=report_page_id,
                report_page=report_page,
                report_source_status="REWARD",
                report_action_id="1009",
                report_action="SELECT_QUEST_REWARD",
                report_target_status="REWARD",
                reward_page_id=reward_page_id,
                reward_page=reward_page,
                unresolved_reason="" if report_page_id != "0" else "reward page is not explicit",
            )
        elif template == "item_collecting":
            start_page_id, start_page = page(int(element.get("start_dialog_id", "0")) or 1011)
            report_page_id, report_page = page(int(element.get("start_dialog_id2", "0")) or 2375)
            reward_page_id, reward_page = page(int(element.get("check_ok_dialog_id", "5")))
            values.update(
                contract_scope="FULL",
                start_page_id=start_page_id,
                start_page=start_page,
                report_open_action_id="31",
                report_open_action="QUEST_SELECT",
                report_page_id=report_page_id,
                report_page=report_page,
                report_source_status="START",
                report_action_id="39",
                report_action="CHECK_COLLECTED_ITEMS",
                report_target_status="REWARD",
                reward_page_id=reward_page_id,
                reward_page=reward_page,
                unresolved_reason="",
            )
        elif template == "item_order":
            report_page_id, report_page = page(2375)
            reward_page_id, reward_page = page(5)
            talk_npcs = [element.get(attribute, "")
                         for attribute in ("talk_npc_id1", "talk_npc_id2")]
            talk_npcs = [npc_id for npc_id in talk_npcs if npc_id and npc_id != "0"]
            values.update(
                contract_scope="FULL",
                report_open_action_id="31",
                report_open_action="QUEST_SELECT",
                report_page_id=report_page_id,
                report_page=report_page,
                report_source_status="START",
                report_action_id="1009",
                report_action="SELECT_QUEST_REWARD",
                report_target_status="REWARD",
                reward_page_id=reward_page_id,
                reward_page=reward_page,
                progress_vars=" ".join(str(index) for index in range(len(talk_npcs))),
                progress_npc_ids=" ".join(talk_npcs),
                progress_page_ids=" ".join("1352" for _ in talk_npcs),
                progress_pages=" ".join("SELECT2" for _ in talk_npcs),
                progress_action_ids=" ".join("10000" for _ in talk_npcs),
                progress_actions=" ".join("SETPRO1" for _ in talk_npcs),
                unresolved_reason="",
            )

        contracts.append(
            Contract(
                quest_id=int(raw_id),
                template_type=template,
                contract_scope=values["contract_scope"],
                start_type=start_type,
                start_npc_ids=start_npcs,
                end_npc_ids=end_npcs,
                start_page_id=values["start_page_id"],
                start_page=values["start_page"],
                report_open_action_id=values["report_open_action_id"],
                report_open_action=values["report_open_action"],
                report_page_id=values["report_page_id"],
                report_page=values["report_page"],
                report_source_status=values["report_source_status"],
                report_action_id=values["report_action_id"],
                report_action=values["report_action"],
                report_target_status=values["report_target_status"],
                reward_page_id=values["reward_page_id"],
                reward_page=values["reward_page"],
                progress_vars=values["progress_vars"],
                progress_npc_ids=values["progress_npc_ids"],
                progress_page_ids=values["progress_page_ids"],
                progress_pages=values["progress_pages"],
                progress_action_ids=values["progress_action_ids"],
                progress_actions=values["progress_actions"],
                unresolved_reason=values["unresolved_reason"],
                source_revision=revision,
                source_resource=resource,
                source_git_object=git_object,
                source_sha256=digest,
            )
        )
    return sorted(contracts, key=lambda contract: (contract.quest_id, contract.template_type))


def render(contracts: list[Contract]) -> bytes:
    stream = io.StringIO(newline="")
    fields = list(Contract.__dataclass_fields__)
    writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
    writer.writeheader()
    writer.writerows(asdict(contract) for contract in contracts)
    return b"\xef\xbb\xbf" + stream.getvalue().encode("utf-8")


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    output = args.output if args.output.is_absolute() else root / args.output
    content, git_object = git_bytes(root, args.revision, args.resource)
    generated = render(extract_contracts(content, args.revision, args.resource, git_object))
    if args.check:
        if not output.is_file() or output.read_bytes() != generated:
            raise SystemExit(f"legacy quest dialog contracts are stale: {output}")
        return 0
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(generated)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
