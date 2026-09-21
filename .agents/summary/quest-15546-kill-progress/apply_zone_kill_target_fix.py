#!/usr/bin/env python3
"""把 5.8 Iluma/Norsvold 任务族的击杀目标对齐到客户端契约的完整变体集合。"""
from __future__ import annotations
import pathlib

ROOT = pathlib.Path("/Users/mc/IdeaProjects/AionEmu-test")
Q = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"

LF6_A = ("240475 240476 241656 241657 240477 240478 241658 241659 240479 240480 241660 241661 "
         "240481 240482 241662 241663 240483 240484 241664 241665")
LF6_A2 = ("240487 240488 241668 241669 240489 240490 241670 241671 240491 240492 241672 241673 "
          "240493 240494 241674 241675 240495 240496 241676 241677 240497 240498 241678 241679 "
          "241442 241443 243284 243285")
DF6_A1 = ("240369 240370 241496 241497 240371 240372 241498 241499 240373 240374 241500 241501 "
          "240375 240376 241502 241503 240377 240378 241504 241505 241177 241178 243264 243265")
DF6_A2 = ("240381 240382 241508 241509 240383 240384 241510 241511 240385 240386 241512 241513 "
          "240387 240388 241514 241515 240389 240390 241516 241517 240391 240392 241518 241519")

# 15546：四个独立计数器，每个对应一族（基础 66/67 + T_ 66/67）。
Q15546 = {
    "240475": "240475 240476 241656 241657",
    "240483": "240483 240484 241664 241665",
    "240495": "240495 240496 241676 241677",
    "240497": "240497 240498 241678 241679",
}
# 25546：魔族镜像，四族分别为 Popoku / Bookie / ElementalLightM / Mudthorn。
Q25546 = {
    "240377": "240377 240378 241504 241505",
    "240371": "240371 240372 241498 241499",
    "240381": "240381 240382 241508 241509",
    "240385": "240385 240386 241512 241513",
}

SET_REWRITES = {
    "80891": [("240475 240477 240479 240481 240483", LF6_A)],
    "80897": [("240475 240477 240479 240481 240483", LF6_A)],
    "80927": [("240475 240477 240479 240481 240483", LF6_A)],
    "42001": [("240475 240477 240479 240481 240483", LF6_A)],
    "25501": [("240475 240477 240479 240481 240483", LF6_A)],
    "80892": [("240487 240489 240491 240493 240495 240497 241442", LF6_A2)],
    "80898": [("240487 240489 240491 240493 240495 240497 241442", LF6_A2)],
    "80928": [("240487 240489 240491 240493 240495 240497 241442", LF6_A2)],
    "80929": [("240487 240489 240491 240493 240495 240497 241442", LF6_A2)],
    "42002": [("240487 240489 240491 240493 240495 240497 241442", LF6_A2)],
    "25504": [("240487 240489 240491 240493 240495 240497 241442", LF6_A2)],
    "25500": [("240369 240371 240373 240375 240377 241177", DF6_A1)],
    "25503": [("240381 240383 240385 240387 240389 240391", DF6_A2)],
}


def patch_single_id_quest(quest: str, groups: dict[str, str]) -> None:
    path = Q / f"{quest}.xml"
    text = path.read_text(encoding="utf-8")
    for old_id, new_ids in groups.items():
        old = f'<kill-npc npc-id="{old_id}"/>'
        new = f'<kill-npc npc-ids="{new_ids}"/>'
        if old not in text:
            raise SystemExit(f"{quest}: missing {old}")
        text = text.replace(old, new)
    path.write_text(text, encoding="utf-8")


def patch_metadata_kills(quest: str, groups: list[tuple[int, str]]) -> None:
    path = Q / f"{quest}.xml"
    text = path.read_text(encoding="utf-8")
    for sequence, ids in groups:
        old = f'      <kill sequence="{sequence}"><npc id="{ids.split()[0]}"/></kill>\n'
        lines = "\n".join(f'        <npc id="{i}"/>' for i in ids.split())
        new = f'      <kill sequence="{sequence}">\n{lines}\n      </kill>\n'
        if old not in text:
            raise SystemExit(f"{quest}: metadata kill sequence {sequence} not found as expected")
        text = text.replace(old, new)
    path.write_text(text, encoding="utf-8")


def patch_set_quest(quest: str, pairs: list[tuple[str, str]]) -> None:
    path = Q / f"{quest}.xml"
    text = path.read_text(encoding="utf-8")
    for old, new in pairs:
        old_attr = f'<kill-npc npc-ids="{old}"/>'
        new_attr = f'<kill-npc npc-ids="{new}"/>'
        if old_attr not in text:
            raise SystemExit(f"{quest}: missing {old_attr}")
        text = text.replace(old_attr, new_attr)
    path.write_text(text, encoding="utf-8")


for quest, groups in (("15546", Q15546), ("25546", Q25546)):
    patch_single_id_quest(quest, groups)
    patch_metadata_kills(quest, [(n, ids) for n, ids in enumerate(groups.values(), start=1)])

for quest, pairs in SET_REWRITES.items():
    patch_set_quest(quest, pairs)

print("patched", 2 + len(SET_REWRITES), "quest definitions")
