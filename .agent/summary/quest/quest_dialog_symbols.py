#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
from pathlib import Path


ATTRIBUTE_PATTERN = re.compile(r'([A-Za-z_][\w-]*)="([^"]*)"')


def read_client_map(path: Path, id_field: str, constant_field: str, prefix: str) -> tuple[dict[int, str], dict[str, int]]:
    by_id: dict[int, str] = {}
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            item_id = int(row[id_field])
            constant = row[constant_field]
            if not constant.startswith(prefix):
                raise ValueError(f"unexpected client constant {constant!r} in {path}")
            symbol = constant[len(prefix) :]
            by_id[item_id] = symbol
    return by_id, {symbol: item_id for item_id, symbol in by_id.items()}


def load_maps(root: Path) -> tuple[dict[int, str], dict[str, int], dict[int, str], dict[str, int]]:
    mapping_dir = root / "docs/quest/client-dialog-mapping"
    actions_by_id, actions_by_name = read_client_map(
        mapping_dir / "client-hyperlinks.csv", "action_id", "action_constant", "HACTION_"
    )
    pages_by_id, pages_by_name = read_client_map(
        mapping_dir / "client-html-pages.csv", "page_id", "page_constant", "HTML_PAGE_"
    )
    actions_by_id[-1] = "USE_OBJECT"
    actions_by_name.pop("ERROR", None)
    actions_by_name["USE_OBJECT"] = -1
    return actions_by_id, actions_by_name, pages_by_id, pages_by_name


def attributes(tag: str) -> dict[str, str]:
    return dict(ATTRIBUTE_PATTERN.findall(tag))


def action_expression(raw: str, actions_by_id: dict[int, str]) -> str:
    result: list[str] = []
    for token in raw.strip().replace(",", " ").split():
        if ".." not in token:
            result.append(action_name(int(token), actions_by_id))
            continue
        pieces = token.split("..")
        if len(pieces) != 2:
            raise ValueError(f"invalid action range {token!r}")
        first, last = map(int, pieces)
        if first > last or last - first >= 256:
            raise ValueError(f"invalid action range {token!r}")
        names = [action_name(item_id, actions_by_id) for item_id in range(first, last + 1)]
        result.append(names[0] if first == last else f"{names[0]}..{names[-1]}")
    if not result:
        raise ValueError("empty action expression")
    return " ".join(result)


def action_name(item_id: int, actions_by_id: dict[int, str]) -> str:
    try:
        return actions_by_id[item_id]
    except KeyError as error:
        raise ValueError(f"client HyperLinks.xml has no action id {item_id}") from error


def page_name(item_id: int, pages_by_id: dict[int, str]) -> str:
    try:
        return pages_by_id[item_id]
    except KeyError as error:
        raise ValueError(f"client HtmlPages.xml has no page id {item_id}") from error


LEGACY_ACTION_ALIASES = {
    "USE_OBJECT": "USE_OBJECT",
    "ACCEPT_QUEST": "QUEST_ACCEPT_1",
    "REFUSE_QUEST": "QUEST_REFUSE_1",
}
