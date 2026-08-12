#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import os
import re
import shutil
import subprocess
from pathlib import Path

from quest_dialog_symbols import LEGACY_ACTION_ALIASES, action_expression, attributes, load_maps, page_name


SELF_CLOSING = r"<(?P<name>{names})\b(?P<attrs>[^>]*)/>"
NPC_COMPLETE_PATTERN = re.compile(r"(?P<indent>^[ \t]*)<npc-complete\b(?P<attrs>[^>]*?)(?:/>|>(?P<body>.*?)</npc-complete>)", re.MULTILINE | re.DOTALL)
NPC_DIALOG_PATTERN = re.compile(r"(?P<indent>^[ \t]*)<npc-dialog\b(?P<attrs>[^>]*)>(?P<body>.*?)</npc-dialog>", re.MULTILINE | re.DOTALL)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Migrate quest dialog XML to typed client symbols.")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--source-dir", type=Path)
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--write", action="store_true", help="Replace source files after hash revalidation.")
    return parser.parse_args()


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def render_attrs(items: list[tuple[str, str]]) -> str:
    return " ".join(f'{name}="{value}"' for name, value in items)


def rewrite_simple_tags(source: str, actions_by_id: dict[int, str], pages_by_id: dict[int, str]) -> str:
    pattern = re.compile(SELF_CLOSING.format(names="talk-to-npc|quest-dialog|show-quest-dialog|show-quest-selection-dialog|npc-start|npc-report"))

    def replace(match: re.Match[str]) -> str:
        name = match.group("name")
        attrs = attributes(match.group(0))
        if name == "talk-to-npc":
            action_value = attrs.get("dialog")
            if action_value:
                action_value = LEGACY_ACTION_ALIASES[action_value]
            elif "dialog-id" in attrs:
                action_value = actions_by_id[int(attrs["dialog-id"])]
            elif "dialog-ids" in attrs:
                action_value = action_expression(attrs["dialog-ids"], actions_by_id)
            else:
                return match.group(0)
            key = "actions" if " " in action_value or ".." in action_value else "action"
            return f'<dialog type="TALK_TO_NPC" npc-id="{attrs["npc-id"]}" {key}="{action_value}"/>'
        if name == "quest-dialog":
            action_value = LEGACY_ACTION_ALIASES.get(attrs.get("dialog", ""))
            if action_value is None:
                action_value = actions_by_id[int(attrs["dialog-id"])]
            return f'<dialog type="QUEST_ACTION" action="{action_value}"/>'
        if name == "show-quest-dialog":
            return f'<dialog type="SHOW_QUEST_PAGE" page="{page_name(int(attrs["dialog-id"]), pages_by_id)}"/>'
        if name == "show-quest-selection-dialog":
            return f'<dialog type="SHOW_SELECTION_PAGE" page="{page_name(int(attrs["dialog-id"]), pages_by_id)}"/>'
        if name == "npc-start":
            items = [("type", "NPC_START"), ("npc-id", attrs["npc-id"]), ("source", attrs["source"]), ("target", attrs["target"])]
            if "selection-sources" in attrs:
                items.append(("selection-sources", attrs["selection-sources"]))
            items.append(("start-page", page_name(int(attrs["start-dialog-id"]), pages_by_id) if "start-dialog-id" in attrs else "SELECT1"))
            return f'<dialog {render_attrs(items)}/>'
        items = [("type", "NPC_REPORT"), ("npc-id", attrs["npc-id"]), ("source", attrs["source"]),
                 ("target", attrs["target"]), ("page", page_name(int(attrs["page"]), pages_by_id))]
        return f'<dialog {render_attrs(items)}/>'

    return pattern.sub(replace, source)


def rewrite_npc_start_blocks(source: str, pages_by_id: dict[int, str]) -> str:
    pattern = re.compile(r"<npc-start\b(?P<attrs>[^>]*)>(?P<body>.*?)</npc-start>", re.DOTALL)

    def replace(match: re.Match[str]) -> str:
        attrs = attributes(match.group(0))
        items = [("type", "NPC_START"), ("npc-id", attrs["npc-id"]), ("source", attrs["source"]), ("target", attrs["target"])]
        if "selection-sources" in attrs:
            items.append(("selection-sources", attrs["selection-sources"]))
        items.append(("start-page", page_name(int(attrs["start-dialog-id"]), pages_by_id) if "start-dialog-id" in attrs else "SELECT1"))
        return f'<dialog {render_attrs(items)}>{match.group("body")}</dialog>'

    return pattern.sub(replace, source)


def rewrite_npc_complete(source: str, actions_by_id: dict[int, str]) -> str:
    def replace(match: re.Match[str]) -> str:
        indent = match.group("indent")
        attrs = attributes("<npc-complete" + match.group("attrs") + ">")
        body = match.group("body") or ""
        if "dialog-ids" in attrs and "actions" in attrs:
            raise ValueError("npc-complete must declare dialog-ids or actions, not both")
        if "preview-dialog-ids" in attrs and "<preview" in body:
            raise ValueError("npc-complete must declare preview-dialog-ids or preview, not both")
        items: list[tuple[str, str]] = []
        for key, value in attrs.items():
            if key == "dialog-ids":
                items.append(("actions", action_expression(value, actions_by_id)))
            elif key != "preview-dialog-ids":
                items.append((key, value))
        preview = attrs.get("preview-dialog-ids")

        def choice_replace(choice: re.Match[str]) -> str:
            choice_attrs = attributes(choice.group(0))
            if "dialog-id" not in choice_attrs:
                return choice.group(0)
            if "action" in choice_attrs or "actions" in choice_attrs:
                raise ValueError("choice must declare dialog-id or action/actions, not both")
            return f'<choice action="{actions_by_id[int(choice_attrs["dialog-id"])]}" reward-index="{choice_attrs["reward-index"]}"/>'

        def fallback_replace(fallback: re.Match[str]) -> str:
            fallback_attrs = attributes(fallback.group(0))
            if "dialog-ids" not in fallback_attrs:
                return fallback.group(0)
            if "action" in fallback_attrs or "actions" in fallback_attrs:
                raise ValueError("fallback must declare dialog-ids or action/actions, not both")
            return f'<fallback actions="{action_expression(fallback_attrs["dialog-ids"], actions_by_id)}"/>'

        body = re.sub(r"<choice\b[^>]*/>", choice_replace, body)
        body = re.sub(r"<fallback\b[^>]*/>", fallback_replace, body)
        if preview:
            preview_line = f'{indent}  <preview actions="{action_expression(preview, actions_by_id)}"/>'
            fallback_offset = body.find("<fallback")
            after_offset = body.find("<after-commit")
            offsets = [offset for offset in (fallback_offset, after_offset) if offset >= 0]
            if offsets:
                offset = min(offsets)
                line_start = body.rfind("\n", 0, offset) + 1
                body = body[:line_start] + preview_line + "\n" + body[line_start:]
            elif body.strip():
                body = body.rstrip() + "\n" + preview_line + "\n" + indent
            else:
                body = "\n" + preview_line + "\n" + indent
        if not body.strip():
            raise ValueError("npc-complete must retain preview or child routes after migration")
        return f'{indent}<npc-complete {render_attrs(items)}>{body}</npc-complete>'

    return NPC_COMPLETE_PATTERN.sub(replace, source)


def rewrite_npc_dialog(source: str, actions_by_id: dict[int, str], pages_by_id: dict[int, str]) -> str:
    def replace(match: re.Match[str]) -> str:
        indent = match.group("indent")
        attrs = attributes("<npc-dialog" + match.group("attrs") + ">")
        response = match.group("body").strip()
        if response.startswith("<show-quest-dialog"):
            response_attrs = attributes(response)
            after = f'<dialog type="SHOW_QUEST_PAGE" page="{page_name(int(response_attrs["dialog-id"]), pages_by_id)}"/>'
        elif response.startswith("<show-quest-selection-dialog"):
            response_attrs = attributes(response)
            after = f'<dialog type="SHOW_SELECTION_PAGE" page="{page_name(int(response_attrs["dialog-id"]), pages_by_id)}"/>'
        elif response.startswith("<close-dialog"):
            after = "<close-dialog/>"
        else:
            raise ValueError(f"unsupported npc-dialog response {response!r}")
        actions = action_expression(attrs["dialog-ids"], actions_by_id)
        action_attr = "actions" if " " in actions or ".." in actions else "action"
        rows = []
        for npc_id in attrs["npc-ids"].split():
            rows.append(
                f'{indent}<transition source="{attrs["source"]}" target="{attrs["source"]}">'
                f'<event><dialog type="TALK_TO_NPC" npc-id="{npc_id}" {action_attr}="{actions}"/></event>'
                f'<after-commit>{after}</after-commit></transition>'
            )
        return "\n".join(rows)

    return NPC_DIALOG_PATTERN.sub(replace, source)


def migrate(source: str, actions_by_id: dict[int, str], pages_by_id: dict[int, str]) -> str:
    result = rewrite_npc_dialog(source, actions_by_id, pages_by_id)
    result = rewrite_npc_complete(result, actions_by_id)
    result = rewrite_npc_start_blocks(result, pages_by_id)
    result = rewrite_simple_tags(result, actions_by_id, pages_by_id)
    return result


def pretty_format(source: str) -> str:
    xmllint = shutil.which("xmllint")
    if xmllint is None:
        raise RuntimeError("xmllint is required to format migrated quest XML")
    env = os.environ.copy()
    env["XMLLINT_INDENT"] = "  "
    formatted = subprocess.run(
        [xmllint, "--format", "-"],
        input=source,
        text=True,
        capture_output=True,
        env=env,
        check=False,
    )
    if formatted.returncode != 0:
        raise ValueError(formatted.stderr.strip() or "xmllint failed to format migrated quest XML")
    return formatted.stdout


def main() -> None:
    args = parse_args()
    root = args.root.resolve()
    source_dir = (args.source_dir or root / "src/main/resources/aion/data/static_data/quest_definition/quests").resolve()
    output_dir = args.output_dir.resolve() if args.output_dir else None
    if args.write and output_dir:
        raise SystemExit("choose --write or --output-dir, not both")
    if not args.write and output_dir is None:
        raise SystemExit("choose --output-dir for a dry run or --write to replace sources")
    actions_by_id, _, pages_by_id, _ = load_maps(root)
    paths = sorted(source_dir.glob("*.xml"))
    snapshots = {path: path.read_bytes() for path in paths}
    outputs: dict[Path, bytes] = {}
    for path, raw in snapshots.items():
        source = raw.decode("utf-8")
        migrated = migrate(source, actions_by_id, pages_by_id)
        outputs[path] = (pretty_format(migrated) if migrated != source else migrated).encode("utf-8")
    changed = [path for path in paths if outputs[path] != snapshots[path]]
    if output_dir:
        output_dir.mkdir(parents=True, exist_ok=True)
        for path in paths:
            (output_dir / path.name).write_bytes(outputs[path])
    else:
        conflicts = []
        for path in changed:
            current = path.read_bytes()
            if sha256_bytes(current) != sha256_bytes(snapshots[path]):
                conflicts.append(path)
                continue
            path.write_bytes(outputs[path])
        if conflicts:
            raise SystemExit("concurrent changes detected; skipped: " + ", ".join(str(path) for path in conflicts))
    print(f"files={len(paths)} changed={len(changed)} output={output_dir or source_dir}")


if __name__ == "__main__":
    main()
