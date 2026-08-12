#!/usr/bin/env python3

from __future__ import annotations

import argparse
import csv
import hashlib
import html
import json
import re
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

try:
    import lxml.etree as LET
except ImportError:
    LET = None


ACTION_PATTERN = re.compile(r"\bHACTION_[A-Z0-9_]+\b", re.IGNORECASE)
QUEST_FILE_PATTERN = re.compile(r"^quest_q(\d+)\.html$", re.IGNORECASE)
HTML_PAGE_START_PATTERN = re.compile(
    r"<HtmlPage\b[^>]*\bname\s*=\s*(?P<quote>['\"])(?P<name>.*?)"
    r"(?P=quote)[^>]*>",
    re.IGNORECASE | re.DOTALL,
)
ACT_ELEMENT_PATTERN = re.compile(
    r"<Act\b(?P<attributes>[^>]*)>(?P<body>.*?)</Act\s*>",
    re.IGNORECASE | re.DOTALL,
)
HREF_PATTERN = re.compile(
    r"\bhref\s*=\s*(?P<quote>['\"])(?P<href>.*?)(?P=quote)",
    re.IGNORECASE | re.DOTALL,
)
TAG_PATTERN = re.compile(r"<[^>]+>", re.DOTALL)


@dataclass(frozen=True)
class ActionDefinition:
    action_id: int
    constant: str


@dataclass(frozen=True)
class PageDefinition:
    page_id: int
    constant: str
    html_page_name: str


@dataclass(frozen=True)
class ActionOccurrence:
    quest_id: int
    source_file: str
    source_variant: str
    html_page_name: str
    select_index: int
    token_index: int
    href: str
    action_constant: str
    button_text_zh: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate Aion client dialog page/action mapping tables."
    )
    parser.add_argument(
        "--definitions-dir",
        type=Path,
        default=Path("/Users/mc/PycharmProjects/unpak/dialog_unpacked"),
        help="Directory containing decoded HyperLinks.xml and HtmlPages.xml.",
    )
    parser.add_argument(
        "--zh-dialogs-dir",
        type=Path,
        default=Path("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs"),
        help="Directory containing decoded Chinese quest HTML files.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("docs/quest/client-dialog-mapping"),
        help="Directory where CSV and summary files are written.",
    )
    return parser.parse_args()


def require_text(element: ET.Element, child_name: str, source: Path) -> str:
    child = element.find(child_name)
    if child is None or child.text is None or not child.text.strip():
        raise ValueError(f"Missing {child_name!r} in {source}: {ET.tostring(element)}")
    return child.text.strip()


def parse_actions(path: Path) -> list[ActionDefinition]:
    root = ET.parse(path).getroot()
    actions = [
        ActionDefinition(
            action_id=int(require_text(element, "id", path)),
            constant=require_text(element, "name", path).upper(),
        )
        for element in root.findall("hyperlink")
    ]
    validate_unique(actions, "action_id", path)
    return sorted(actions, key=lambda item: (item.action_id, item.constant))


def parse_pages(path: Path) -> list[PageDefinition]:
    root = ET.parse(path).getroot()
    pages = []
    for element in root.findall("htmlpage"):
        page_name_element = element.find("htmlpagename")
        pages.append(
            PageDefinition(
                page_id=int(require_text(element, "id", path)),
                constant=require_text(element, "name", path).upper(),
                html_page_name=(
                    page_name_element.text.strip()
                    if page_name_element is not None and page_name_element.text
                    else ""
                ),
            )
        )
    validate_unique(pages, "page_id", path)
    return sorted(pages, key=lambda item: (item.page_id, item.constant))


def validate_unique(items: Sequence[object], attribute: str, source: Path) -> None:
    values = [getattr(item, attribute) for item in items]
    duplicates = sorted(value for value, count in Counter(values).items() if count > 1)
    if duplicates:
        preview = ", ".join(str(value) for value in duplicates[:20])
        raise ValueError(f"Duplicate {attribute} values in {source}: {preview}")


def normalized_source_text(source: str) -> str:
    return " ".join(html.unescape(TAG_PATTERN.sub("", source)).split())


def parse_quest_document(path: Path) -> tuple[object | None, str]:
    if path.stat().st_size == 0:
        return None, "empty file"
    try:
        return ET.parse(path).getroot(), ""
    except ET.ParseError as strict_error:
        if LET is None:
            raise RuntimeError(
                f"{strict_error}; install lxml to recover malformed client HTML"
            ) from strict_error

        parser = LET.XMLParser(
            recover=True,
            resolve_entities=False,
            no_network=True,
            huge_tree=True,
        )
        document = LET.parse(str(path), parser)
        recovered_root = document.getroot()
        diagnostics = " | ".join(
            f"line {entry.line}: {entry.message}" for entry in parser.error_log
        )
        return recovered_root, diagnostics or str(strict_error)


def parse_quest_file(
    path: Path, root: Path
) -> tuple[list[ActionOccurrence], str]:
    match = QUEST_FILE_PATTERN.match(path.name)
    if match is None:
        return [], ""

    quest_id = int(match.group(1))
    relative_path = path.relative_to(root).as_posix()
    variant = "unused" if "unused" in {part.lower() for part in path.parts} else "active"
    document, recovery_diagnostic = parse_quest_document(path)
    if document is None:
        return [], recovery_diagnostic or "empty document"
    source = path.read_text(encoding="utf-8-sig")
    occurrences = []

    events = [
        (match.start(), "page", match)
        for match in HTML_PAGE_START_PATTERN.finditer(source)
    ]
    events.extend(
        (match.start(), "action", match) for match in ACT_ELEMENT_PATTERN.finditer(source)
    )
    page_name = ""
    select_index = 0
    for _, event_type, match in sorted(events, key=lambda item: item[0]):
        if event_type == "page":
            page_name = html.unescape(match.group("name")).strip()
            select_index = 0
            continue

        href_match = HREF_PATTERN.search(match.group("attributes"))
        if href_match is None:
            continue
        href = html.unescape(href_match.group("href")).strip()
        constants = ACTION_PATTERN.findall(href)
        if not constants:
            continue
        select_index += 1
        for token_index, constant in enumerate(constants, start=1):
            occurrences.append(
                ActionOccurrence(
                    quest_id=quest_id,
                    source_file=relative_path,
                    source_variant=variant,
                    html_page_name=page_name,
                    select_index=select_index,
                    token_index=token_index,
                    href=href,
                    action_constant=constant.upper(),
                    button_text_zh=normalized_source_text(match.group("body")),
                )
            )
    return occurrences, recovery_diagnostic


def discover_quest_files(root: Path) -> list[Path]:
    return sorted(
        (
            path
            for path in root.rglob("*")
            if path.is_file() and QUEST_FILE_PATTERN.match(path.name)
        ),
        key=lambda path: path.relative_to(root).as_posix().lower(),
    )


def write_csv(path: Path, fieldnames: Sequence[str], rows: Iterable[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fieldnames, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def grouped_by(items: Iterable[object], attribute: str, normalize=str) -> dict[str, list[object]]:
    result: dict[str, list[object]] = defaultdict(list)
    for item in items:
        value = getattr(item, attribute)
        if value:
            result[normalize(value)].append(item)
    return result


def joined_ids(items: Sequence[object], attribute: str) -> str:
    return "|".join(str(getattr(item, attribute)) for item in items)


def joined_values(items: Sequence[object], attribute: str) -> str:
    return "|".join(str(getattr(item, attribute)) for item in items)


def mapping_status(items: Sequence[object]) -> str:
    if not items:
        return "missing"
    if len(items) == 1:
        return "exact"
    return "ambiguous"


def symbol_stem(constant: str) -> str:
    for prefix in ("HACTION_", "HTML_PAGE_"):
        if constant.startswith(prefix):
            return constant[len(prefix) :]
    return constant


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def limited_join(values: Iterable[str], limit: int = 5) -> str:
    unique_values = []
    seen = set()
    for value in values:
        if not value or value in seen:
            continue
        seen.add(value)
        unique_values.append(value)
        if len(unique_values) == limit:
            break
    return " | ".join(unique_values)


def main() -> None:
    args = parse_args()
    definitions_dir = args.definitions_dir.resolve()
    zh_dialogs_dir = args.zh_dialogs_dir.resolve()
    output_dir = args.output_dir.resolve()
    hyperlinks_path = definitions_dir / "HyperLinks.xml"
    html_pages_path = definitions_dir / "HtmlPages.xml"

    for required_path in (hyperlinks_path, html_pages_path, zh_dialogs_dir):
        if not required_path.exists():
            raise FileNotFoundError(required_path)

    actions = parse_actions(hyperlinks_path)
    pages = parse_pages(html_pages_path)
    actions_by_id = {item.action_id: item for item in actions}
    pages_by_id = {item.page_id: item for item in pages}
    actions_by_name = grouped_by(actions, "constant", str.upper)
    pages_by_name = grouped_by(pages, "html_page_name", str.lower)
    action_stems = grouped_by(actions, "constant", lambda value: symbol_stem(value.upper()))
    page_stems = grouped_by(pages, "constant", lambda value: symbol_stem(value.upper()))

    quest_files = discover_quest_files(zh_dialogs_dir)
    occurrences = []
    parse_errors = []
    parse_recoveries = []
    for quest_file in quest_files:
        try:
            file_occurrences, recovery_diagnostic = parse_quest_file(
                quest_file, zh_dialogs_dir
            )
            occurrences.extend(file_occurrences)
            if recovery_diagnostic:
                parse_recoveries.append(
                    {
                        "source_file": quest_file.relative_to(zh_dialogs_dir).as_posix(),
                        "diagnostic": recovery_diagnostic,
                        "recovered_action_occurrences": len(file_occurrences),
                    }
                )
        except (ET.ParseError, UnicodeError, ValueError, RuntimeError) as error:
            parse_errors.append(
                {
                    "source_file": quest_file.relative_to(zh_dialogs_dir).as_posix(),
                    "error": str(error),
                }
            )

    output_dir.mkdir(parents=True, exist_ok=True)

    write_csv(
        output_dir / "client-hyperlinks.csv",
        ("action_id", "action_constant"),
        (
            {"action_id": item.action_id, "action_constant": item.constant}
            for item in actions
        ),
    )
    write_csv(
        output_dir / "client-html-pages.csv",
        ("page_id", "page_constant", "html_page_name"),
        (
            {
                "page_id": item.page_id,
                "page_constant": item.constant,
                "html_page_name": item.html_page_name,
            }
            for item in pages
        ),
    )

    same_id_rows = []
    for item_id in sorted(set(actions_by_id) | set(pages_by_id)):
        action = actions_by_id.get(item_id)
        page = pages_by_id.get(item_id)
        if action and page:
            relation = (
                "same_stem"
                if symbol_stem(action.constant) == symbol_stem(page.constant)
                else "same_id_only"
            )
        elif action:
            relation = "action_only"
        else:
            relation = "page_only"
        same_id_rows.append(
            {
                "id": item_id,
                "action_constant": action.constant if action else "",
                "page_constant": page.constant if page else "",
                "html_page_name": page.html_page_name if page else "",
                "relation": relation,
            }
        )
    write_csv(
        output_dir / "same-id-map.csv",
        ("id", "action_constant", "page_constant", "html_page_name", "relation"),
        same_id_rows,
    )

    symbol_rows = []
    for stem in sorted(set(action_stems) & set(page_stems)):
        for action in action_stems[stem]:
            for page in page_stems[stem]:
                symbol_rows.append(
                    {
                        "symbol_stem": stem,
                        "action_id": action.action_id,
                        "action_constant": action.constant,
                        "page_id": page.page_id,
                        "page_constant": page.constant,
                        "html_page_name": page.html_page_name,
                        "same_id": str(action.action_id == page.page_id).lower(),
                    }
                )
    write_csv(
        output_dir / "same-symbol-map.csv",
        (
            "symbol_stem",
            "action_id",
            "action_constant",
            "page_id",
            "page_constant",
            "html_page_name",
            "same_id",
        ),
        symbol_rows,
    )

    detail_rows = []
    for occurrence in occurrences:
        page_matches = pages_by_name.get(occurrence.html_page_name.lower(), [])
        action_matches = actions_by_name.get(occurrence.action_constant, [])
        detail_rows.append(
            {
                "quest_id": occurrence.quest_id,
                "source_file": occurrence.source_file,
                "source_variant": occurrence.source_variant,
                "html_page_name": occurrence.html_page_name,
                "page_id": joined_ids(page_matches, "page_id"),
                "page_constant": joined_values(page_matches, "constant"),
                "page_mapping": mapping_status(page_matches),
                "select_index": occurrence.select_index,
                "token_index": occurrence.token_index,
                "href": occurrence.href,
                "action_id": joined_ids(action_matches, "action_id"),
                "action_constant": occurrence.action_constant,
                "action_mapping": mapping_status(action_matches),
                "button_text_zh": occurrence.button_text_zh,
            }
        )
    write_csv(
        output_dir / "quest-dialog-action-details.csv",
        (
            "quest_id",
            "source_file",
            "source_variant",
            "html_page_name",
            "page_id",
            "page_constant",
            "page_mapping",
            "select_index",
            "token_index",
            "href",
            "action_id",
            "action_constant",
            "action_mapping",
            "button_text_zh",
        ),
        detail_rows,
    )

    page_action_groups: dict[tuple[str, ...], list[dict[str, object]]] = defaultdict(list)
    action_groups: dict[tuple[str, ...], list[dict[str, object]]] = defaultdict(list)
    for row in detail_rows:
        page_action_key = (
            str(row["html_page_name"]),
            str(row["page_id"]),
            str(row["page_constant"]),
            str(row["page_mapping"]),
            str(row["action_id"]),
            str(row["action_constant"]),
            str(row["action_mapping"]),
        )
        action_key = (
            str(row["action_id"]),
            str(row["action_constant"]),
            str(row["action_mapping"]),
        )
        page_action_groups[page_action_key].append(row)
        action_groups[action_key].append(row)

    page_action_rows = []
    for key, rows in sorted(page_action_groups.items()):
        page_action_rows.append(
            {
                "html_page_name": key[0],
                "page_id": key[1],
                "page_constant": key[2],
                "page_mapping": key[3],
                "action_id": key[4],
                "action_constant": key[5],
                "action_mapping": key[6],
                "occurrences": len(rows),
                "distinct_quests": len({row["quest_id"] for row in rows}),
                "button_text_samples_zh": limited_join(
                    str(row["button_text_zh"]) for row in rows
                ),
            }
        )
    write_csv(
        output_dir / "page-action-map.csv",
        (
            "html_page_name",
            "page_id",
            "page_constant",
            "page_mapping",
            "action_id",
            "action_constant",
            "action_mapping",
            "occurrences",
            "distinct_quests",
            "button_text_samples_zh",
        ),
        page_action_rows,
    )

    action_summary_rows = []
    for key, rows in sorted(action_groups.items(), key=lambda item: item[0][1]):
        action_summary_rows.append(
            {
                "action_id": key[0],
                "action_constant": key[1],
                "action_mapping": key[2],
                "occurrences": len(rows),
                "distinct_quests": len({row["quest_id"] for row in rows}),
                "distinct_source_files": len({row["source_file"] for row in rows}),
                "distinct_pages": len({row["html_page_name"] for row in rows}),
                "button_text_samples_zh": limited_join(
                    str(row["button_text_zh"]) for row in rows
                ),
            }
        )
    write_csv(
        output_dir / "quest-action-summary.csv",
        (
            "action_id",
            "action_constant",
            "action_mapping",
            "occurrences",
            "distinct_quests",
            "distinct_source_files",
            "distinct_pages",
            "button_text_samples_zh",
        ),
        action_summary_rows,
    )
    write_csv(
        output_dir / "parse-errors.csv",
        ("source_file", "error"),
        parse_errors,
    )
    write_csv(
        output_dir / "parse-recoveries.csv",
        ("source_file", "diagnostic", "recovered_action_occurrences"),
        parse_recoveries,
    )

    summary = {
        "sources": {
            "hyperlinks_xml": str(hyperlinks_path),
            "hyperlinks_sha256": sha256(hyperlinks_path),
            "html_pages_xml": str(html_pages_path),
            "html_pages_sha256": sha256(html_pages_path),
            "zh_dialogs_dir": str(zh_dialogs_dir),
        },
        "counts": {
            "hyperlink_definitions": len(actions),
            "html_page_definitions": len(pages),
            "same_id_rows": len(same_id_rows),
            "same_symbol_rows": len(symbol_rows),
            "quest_html_files": len(quest_files),
            "quest_action_occurrences": len(detail_rows),
            "page_action_rows": len(page_action_rows),
            "quest_action_constants": len(action_summary_rows),
            "parse_errors": len(parse_errors),
            "parse_recoveries": len(parse_recoveries),
            "missing_action_mappings": sum(
                row["action_mapping"] == "missing" for row in detail_rows
            ),
            "missing_page_mappings": sum(
                row["page_mapping"] == "missing" for row in detail_rows
            ),
        },
    }
    (output_dir / "mapping-summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(json.dumps(summary["counts"], ensure_ascii=False, sort_keys=True))
    if parse_errors:
        raise SystemExit(
            f"Generated partial tables with {len(parse_errors)} parse errors; "
            f"see {output_dir / 'parse-errors.csv'}"
        )


if __name__ == "__main__":
    main()
