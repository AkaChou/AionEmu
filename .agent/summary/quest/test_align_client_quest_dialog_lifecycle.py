#!/usr/bin/env python3
from __future__ import annotations

import csv
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import align_client_quest_dialog_lifecycle as alignment


class AlignClientQuestDialogLifecycleTest(unittest.TestCase):
    def test_replaces_only_hash_verified_legacy_npc_routes(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
            quest_dir.mkdir(parents=True)
            path = quest_dir / "25512.xml"
            path.write_text("""<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="25512" version="1">
  <nodes>
    <node label="unaccepted" status="NONE"/>
    <node label="started" status="START"/>
    <node label="reward" status="REWARD"/>
  </nodes>
  <transitions>
    <dialog type="NPC_START"
            npc-id="806105"
            source="unaccepted"
            target="started"
            start-page="SELECT1">
      <accept-actions>
        <give-item item-id="182200001" count="1"/>
      </accept-actions>
    </dialog>
    <dialog type="NPC_REPORT"
            npc-id="806105"
            source="reward"
            target="reward"
            page="SELECT2"/>
  </transitions>
</quest-definition>
""", encoding="utf-8")
            pages = {
                25512: (
                    alignment.ClientPage(4762, "SELECT_NONE", frozenset({20000, 20001}),
                                         "quest_q25512.html", "client-hash"),
                    alignment.ClientPage(10002, "DEFAULT_SUCCESS", frozenset({1009}),
                                         "quest_q25512.html", "client-hash"),
                )
            }
            contracts = {
                25512: alignment.LegacyContract("monster_hunt", frozenset({806105}),
                                                frozenset({806105}), "REWARD", 1009, "REWARD",
                                                "object-id", "legacy-hash")
            }

            rows = alignment.scan(root, pages, contracts)
            self.assertEqual(["READY", "READY"], [row.fix_status for row in rows])
            self.assertEqual(2, alignment.replace_ready(root, rows))
            updated = path.read_text(encoding="utf-8")

        self.assertIn('start-page="SELECT_NONE"', updated)
        self.assertIn('page="DEFAULT_SUCCESS"', updated)
        self.assertIn('\n            npc-id="806105"', updated)
        self.assertIn('<accept-actions>', updated)

    def test_reads_only_unique_exact_client_lifecycle_pages(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pages = root / "pages.csv"
            actions = root / "actions.csv"
            with pages.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_variant", "source_sha256", "page_id",
                    "page_constant", "page_mapping"
                ])
                writer.writeheader()
                writer.writerows([
                    {"quest_id": 1, "source_file": "q1", "source_variant": "active", "source_sha256": "h",
                     "page_id": 4762, "page_constant": "HTML_PAGE_SELECT_NONE", "page_mapping": "exact"},
                    {"quest_id": 1, "source_file": "q1", "source_variant": "active", "source_sha256": "h",
                     "page_id": 10002, "page_constant": "HTML_PAGE_DEFAULT_SUCCESS", "page_mapping": "exact"},
                ])
            with actions.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "page_id", "source_variant", "page_mapping", "action_mapping", "action_id",
                    "action_constant"
                ])
                writer.writeheader()
                for action in (20000, 20001):
                    writer.writerow({"quest_id": 1, "page_id": 4762, "source_variant": "active",
                                     "page_mapping": "exact", "action_mapping": "exact", "action_id": action,
                                     "action_constant": "HACTION_QUEST_ACCEPT_SIMPLE" if action == 20000
                                                        else "HACTION_QUEST_REFUSE_SIMPLE"})
                writer.writerow({"quest_id": 1, "page_id": 10002, "source_variant": "active",
                                 "page_mapping": "exact", "action_mapping": "exact", "action_id": 1009,
                                 "action_constant": "HACTION_SELECT_QUEST_REWARD"})

            start, report = alignment.read_client_pages(pages, actions)[1]

        self.assertEqual(4762, start.page_id)
        self.assertEqual(10002, report.page_id)

    def test_follows_symbol_edges_back_to_the_acquisition_root(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pages = root / "pages.csv"
            actions = root / "actions.csv"
            page_rows = [
                (1011, "HTML_PAGE_SELECT1"),
                (1012, "HTML_PAGE_SELECT1_1"),
                (4, "HTML_PAGE_SHOW_ASK_QUEST_ACCEPT_WINDOW"),
                (2375, "HTML_PAGE_SELECT5"),
            ]
            with pages.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_variant", "source_sha256", "page_id",
                    "page_constant", "page_mapping"
                ])
                writer.writeheader()
                for page_id, page_constant in page_rows:
                    writer.writerow({"quest_id": 1, "source_file": "q1", "source_variant": "active",
                                     "source_sha256": "h", "page_id": page_id,
                                     "page_constant": page_constant, "page_mapping": "exact"})
            action_rows = [
                (1011, 1012, "HACTION_SELECT1_1"),
                (1012, 1007, "HACTION_ASK_QUEST_ACCEPT"),
                (4, 1002, "HACTION_QUEST_ACCEPT_1"),
                (2375, 1009, "HACTION_SELECT_QUEST_REWARD"),
            ]
            with actions.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "page_id", "source_variant", "page_mapping", "action_mapping", "action_id",
                    "action_constant"
                ])
                writer.writeheader()
                for page_id, action_id, action_constant in action_rows:
                    writer.writerow({"quest_id": 1, "page_id": page_id, "source_variant": "active",
                                     "page_mapping": "exact", "action_mapping": "exact",
                                     "action_id": action_id, "action_constant": action_constant})

            start, report = alignment.read_client_pages(pages, actions)[1]

        self.assertEqual(1011, start.page_id)
        self.assertEqual(2375, report.page_id)

    def test_follows_symbol_edges_back_to_the_report_root(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pages = root / "pages.csv"
            actions = root / "actions.csv"
            page_rows = [
                (1352, "HTML_PAGE_SELECT2"),
                (1353, "HTML_PAGE_SELECT2_1"),
                (2375, "HTML_PAGE_SELECT5"),
            ]
            with pages.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_variant", "source_sha256", "page_id",
                    "page_constant", "page_mapping"
                ])
                writer.writeheader()
                for page_id, page_constant in page_rows:
                    writer.writerow({"quest_id": 1, "source_file": "q1", "source_variant": "active",
                                     "source_sha256": "h", "page_id": page_id,
                                     "page_constant": page_constant, "page_mapping": "exact"})
            action_rows = [
                (1352, 1353, "HACTION_SELECT2_1"),
                (1353, 2375, "HACTION_SELECT5"),
                (2375, 1009, "HACTION_SELECT_QUEST_REWARD"),
            ]
            with actions.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "page_id", "source_variant", "page_mapping", "action_mapping", "action_id",
                    "action_constant"
                ])
                writer.writeheader()
                for page_id, action_id, action_constant in action_rows:
                    writer.writerow({"quest_id": 1, "page_id": page_id, "source_variant": "active",
                                     "page_mapping": "exact", "action_mapping": "exact",
                                     "action_id": action_id, "action_constant": action_constant})

            _, report = alignment.read_client_pages(pages, actions)[1]

        self.assertEqual(1352, report.page_id)

    def test_rejects_conflicting_active_page_sources(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pages = root / "pages.csv"
            actions = root / "actions.csv"
            with pages.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_variant", "source_sha256", "page_id",
                    "page_constant", "page_mapping"
                ])
                writer.writeheader()
                for source_file, source_hash in (("q1-a", "hash-a"), ("q1-b", "hash-b")):
                    writer.writerow({"quest_id": 1, "source_file": source_file,
                                     "source_variant": "active", "source_sha256": source_hash,
                                     "page_id": 4762, "page_constant": "HTML_PAGE_SELECT_NONE",
                                     "page_mapping": "exact"})
            with actions.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_sha256", "page_id", "source_variant",
                    "page_mapping", "action_mapping", "action_id", "action_constant"
                ])
                writer.writeheader()
                writer.writerow({"quest_id": 1, "source_file": "q1-a", "source_sha256": "hash-a",
                                 "page_id": 4762, "source_variant": "active", "page_mapping": "exact",
                                 "action_mapping": "exact", "action_id": 20000,
                                 "action_constant": "HACTION_QUEST_ACCEPT_SIMPLE"})

            result = alignment.read_client_pages(pages, actions)

        self.assertNotIn(1, result)

    def test_rejects_different_pages_combined_from_multiple_active_sources(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pages = root / "pages.csv"
            actions = root / "actions.csv"
            with pages.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_variant", "source_sha256", "page_id",
                    "page_constant", "page_mapping"
                ])
                writer.writeheader()
                writer.writerows([
                    {"quest_id": 1, "source_file": "q1-a", "source_variant": "active",
                     "source_sha256": "hash-a", "page_id": 4762,
                     "page_constant": "HTML_PAGE_SELECT_NONE", "page_mapping": "exact"},
                    {"quest_id": 1, "source_file": "q1-b", "source_variant": "active",
                     "source_sha256": "hash-b", "page_id": 10002,
                     "page_constant": "HTML_PAGE_DEFAULT_SUCCESS", "page_mapping": "exact"},
                ])
            with actions.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_sha256", "page_id", "source_variant",
                    "page_mapping", "action_mapping", "action_id", "action_constant"
                ])
                writer.writeheader()
                writer.writerows([
                    {"quest_id": 1, "source_file": "q1-a", "source_sha256": "hash-a",
                     "page_id": 4762, "source_variant": "active", "page_mapping": "exact",
                     "action_mapping": "exact", "action_id": 20000,
                     "action_constant": "HACTION_QUEST_ACCEPT_SIMPLE"},
                    {"quest_id": 1, "source_file": "q1-b", "source_sha256": "hash-b",
                     "page_id": 10002, "source_variant": "active", "page_mapping": "exact",
                     "action_mapping": "exact", "action_id": 1009,
                     "action_constant": "HACTION_SELECT_QUEST_REWARD"},
                ])

            result = alignment.read_client_pages(pages, actions)

        self.assertNotIn(1, result)

    def test_allows_repeated_terminal_page_in_the_same_active_file(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pages = root / "pages.csv"
            actions = root / "actions.csv"
            with pages.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_variant", "source_sha256", "page_id",
                    "page_constant", "page_mapping", "action_count"
                ])
                writer.writeheader()
                for _ in range(2):
                    writer.writerow({"quest_id": 1, "source_file": "q1", "source_variant": "active",
                                     "source_sha256": "hash", "page_id": 5,
                                     "page_constant": "HTML_PAGE_SHOW_SELECT_QUEST_REWARD_WINDOW1",
                                     "page_mapping": "exact", "action_count": 0})
            with actions.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_sha256", "page_id", "source_variant",
                    "page_mapping", "action_mapping", "action_id", "action_constant"
                ])
                writer.writeheader()

            result = alignment.read_client_pages(pages, actions)

        self.assertIn(1, result)

    def test_rejects_repeated_interactive_page_in_the_same_active_file(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pages = root / "pages.csv"
            actions = root / "actions.csv"
            with pages.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_variant", "source_sha256", "page_id",
                    "page_constant", "page_mapping", "action_count"
                ])
                writer.writeheader()
                for _ in range(2):
                    writer.writerow({"quest_id": 1, "source_file": "q1", "source_variant": "active",
                                     "source_sha256": "hash", "page_id": 4762,
                                     "page_constant": "HTML_PAGE_SELECT_NONE", "page_mapping": "exact",
                                     "action_count": 1})
            with actions.open("w", encoding="utf-8-sig", newline="") as stream:
                writer = csv.DictWriter(stream, fieldnames=[
                    "quest_id", "source_file", "source_sha256", "page_id", "source_variant",
                    "page_mapping", "action_mapping", "action_id", "action_constant"
                ])
                writer.writeheader()
                writer.writerow({"quest_id": 1, "source_file": "q1", "source_sha256": "hash",
                                 "page_id": 4762, "source_variant": "active", "page_mapping": "exact",
                                 "action_mapping": "exact", "action_id": 20000,
                                 "action_constant": "HACTION_QUEST_ACCEPT_SIMPLE"})

            result = alignment.read_client_pages(pages, actions)

        self.assertNotIn(1, result)


if __name__ == "__main__":
    unittest.main()
