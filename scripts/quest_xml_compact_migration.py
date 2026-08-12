#!/usr/bin/env python3
"""Quest XML compact syntax migration tool.

Migrates legacy <node><project status="X"><vars><var/></vars></project></node> wrappers to the compact
<node label=".." status="X">[<var/>]*</node> syntax, and compresses runs of ordinary <transition> elements
into the nine domain blocks when they match the expander's exact lowering (strict matching only).

Contract: docs/QUEST_XML_COMPACT_MIGRATION_PLAN.zh-CN.md.

- Default mode is scan-only: nothing is written.
- --apply writes files only after a whole-batch preflight (no dirty targets, verifier available).
- Every rewritten file is backed up to target/quest-xml-migration/before/ and must pass a before/after
  Java IR equality check (QuestXmlMigrationVerifier); failures roll back and abort the batch.
- Writes a report.json in target/quest-xml-migration/ on every scan and apply.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import ProcessPoolExecutor
from datetime import datetime, timezone
from functools import lru_cache
from pathlib import Path
from xml.sax.saxutils import quoteattr
from xml.etree import ElementTree as ET

REPO_ROOT = Path(__file__).resolve().parent.parent
REPORT_DIR = REPO_ROOT / "target" / "quest-xml-migration"
REPORT_PATH = REPORT_DIR / "report.json"
BEFORE_DIR = REPORT_DIR / "before"
XSD = REPO_ROOT / "src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd"
PROD_QUESTS = REPO_ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
TEST_RESOURCES = REPO_ROOT / "src/test/resources"
TEST_JAVA = REPO_ROOT / "src/test/java"
DEFAULT_WORKERS = max(1, min(os.cpu_count() or 1, 32))

SCAN_SOURCES = {
    "prod": [PROD_QUESTS],
    "test_resources": [TEST_RESOURCES],
    "test_java": [TEST_JAVA],
}

# ---------------------------------------------------------------------------
# byte/char span scanning (lexical only; semantics are decided by the parser)
# ---------------------------------------------------------------------------

START_TAG = re.compile(r"<([A-Za-z][\w.-]*)((?:\s+[A-Za-z_][\w.-]*(?:\s*=\s*\"[^\"]*\"|\s*=\s*'[^']*'))*)\s*(/?)>")
TAG_END = re.compile(r"</([A-Za-z][\w.-]*)\s*>")
COMMENT = re.compile(r"<!--.*?-->", re.S)
CDATA = re.compile(r"<!\[CDATA\[.*?\]\]>", re.S)


def element_spans(text: str) -> list[tuple[str, int, int, bool]]:
    """Return (tag, start, end, self_closing) for every element, top-level nesting only handled by parser."""
    spans = []
    i = 0
    n = len(text)
    while i < n:
        if text.startswith("<!--", i):
            m = COMMENT.match(text, i)
            i = m.end() if m else i + 4
            continue
        if text.startswith("<![CDATA[", i):
            m = CDATA.match(text, i)
            i = m.end() if m else i + 9
            continue
        if text.startswith("<?", i):
            # processing instruction (e.g. <?xml version="1.0"?>) — skip to '?>'
            end_pi = text.find("?>", i)
            i = end_pi + 2 if end_pi >= 0 else i + 2
            continue
        if text.startswith("<!DOCTYPE", i) or text.startswith("<!doctype", i):
            # skip doctype declaration to matching '>'
            depth = 0
            j = i
            while j < n:
                if text[j] == "<":
                    depth += 1
                elif text[j] == ">":
                    depth -= 1
                    if depth == 0:
                        break
                j += 1
            i = j + 1 if j < n else n
            continue
        if text[i] == "<":
            m = START_TAG.match(text, i)
            if m:
                start = i
                tag = m.group(1)
                self_closing = m.group(3) == "/"
                if self_closing:
                    spans.append((tag, start, m.end(), True))
                    i = m.end()
                    continue
                end_m = TAG_END.search(text, m.end())
                if end_m and end_m.group(1) == tag:
                    spans.append((tag, start, end_m.end(), False))
                    i = end_m.end()
                    continue
                # unterminated; bail out of scanning
                break
            i += 1
        else:
            i += 1
    return spans


def locate_element(text: str, start: int) -> int:
    """Return the end offset of the element whose opening tag starts at ``start``.

    Depth-aware scan: handles self-closing tags, nested elements, comments, CDATA and
    processing instructions. Only used to locate spans of elements the structure parser
    has already identified; never performs semantic matching.
    """
    n = len(text)
    i = start
    # consume opening tag
    while i < n and text[i] != ">":
        i += 1
    if i >= n:
        return -1
    if text[i - 1] == "/":
        return i + 1  # self-closing
    i += 1
    depth = 0
    while i < n:
        if text.startswith("<!--", i):
            m = COMMENT.match(text, i)
            i = m.end() if m else i + 4
            continue
        if text.startswith("<![CDATA[", i):
            m = CDATA.match(text, i)
            i = m.end() if m else i + 9
            continue
        if text.startswith("<?", i):
            end_pi = text.find("?>", i)
            i = end_pi + 2 if end_pi >= 0 else i + 2
            continue
        if text[i] == "<":
            if text.startswith("</", i):
                end_t = text.find(">", i)
                if end_t < 0:
                    return -1
                if depth == 0:
                    return end_t + 1
                depth -= 1
                i = end_t + 1
                continue
            m = START_TAG.match(text, i)
            if m:
                if m.group(3) == "/":
                    i = m.end()
                else:
                    depth += 1
                    i = m.end()
                continue
            i += 1
        else:
            i += 1
    return -1


# ---------------------------------------------------------------------------
# XML structure parsing (ElementTree with namespace handling)
# ---------------------------------------------------------------------------

def parse_xml(text: str):
    """Parse with ElementTree, stripping namespace noise. Raises ET.ParseError on malformed XML."""
    return ET.fromstring(text)


def localname(tag: str) -> str:
    return tag.rsplit("}", 1)[-1] if tag.startswith("{") else tag


def children_of(elem) -> list:
    return list(elem)


def attr(elem, name: str) -> str | None:
    return elem.get(name)


def read_text_exact(path: Path) -> str:
    with path.open("r", encoding="utf-8", newline="") as source:
        return source.read()


def write_text_exact(path: Path, text: str) -> None:
    with path.open("w", encoding="utf-8", newline="") as destination:
        destination.write(text)


# ---------------------------------------------------------------------------
# normalization of a legacy node into compact form (in-memory; mirrors the Java normalizer)
# ---------------------------------------------------------------------------

def normalize_legacy_node(node):
    """Rewrite a legacy <node><project status=..><vars><var/></vars></project></node> in place."""
    status = None
    var_elems = []
    for child in list(node):
        if localname(child.tag) == "project":
            status = child.get("status")
            for inner in list(child):
                if localname(inner.tag) == "vars":
                    var_elems = list(inner)
            node.remove(child)
    if status is not None:
        node.set("status", status)
    for v in var_elems:
        node.append(v)


# ---------------------------------------------------------------------------
# report
# ---------------------------------------------------------------------------

def empty_report(mode: str) -> dict:
    return {
        "mode": mode,
        "started_at": datetime.now(timezone.utc).isoformat(),
        "repository": str(REPO_ROOT),
        "git_head": git_head(),
        "scan_count": 0,
        "changed_file_count": 0,
        "candidate_files": [],
        "node_migration_count": 0,
        "domain_block_counts": {
            "npc-start": 0, "npc-complete": 0, "npc-item-report": 0, "npc-report": 0,
            "counter-grid": 0, "counter": 0, "kill-chain": 0, "kill-routes": 0, "npc-dialog": 0,
        },
        "no_strict_match": [],
        "unsupported_inline_xml": [],
        "dirty": [],
        "authorized_dirty": [],
        "parse_failures": [],
        "compile_failures": [],
        "ir_mismatches": [],
        "remaining_legacy_wrappers": [],
        "parallelism": {"python_processes": 1},
    }


def git_head() -> str:
    try:
        return subprocess.run(["rtk", "git", "rev-parse", "HEAD"], capture_output=True, text=True,
                              cwd=REPO_ROOT).stdout.strip()
    except Exception:
        return "unknown"


def write_report(report: dict) -> None:
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")


def _git_dirty_paths() -> set[str]:
    """Return all tracked and untracked paths reported by Git, without hiding untracked files."""
    result = subprocess.run(["rtk", "git", "status", "--short", "--untracked-files=all"],
                            capture_output=True, text=True, cwd=REPO_ROOT)
    if result.returncode != 0:
        raise RuntimeError("git status failed: " + (result.stderr.strip() or str(result.returncode)))
    paths = set()
    for line in result.stdout.splitlines():
        if len(line) < 3:
            continue
        name = line[3:]
        # Rename/copy status may contain two paths separated by ` -> `; both are dirty.
        for candidate in name.split(" -> "):
            candidate = candidate.strip()
            if candidate:
                paths.add(candidate)
    return paths


def _relative_target(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(REPO_ROOT.resolve()))
    except ValueError:
        return str(path.resolve())


# ---------------------------------------------------------------------------
# legacy wrapper detection
# ---------------------------------------------------------------------------

LEGACY_TAG = re.compile(r"<project(?:[ >])|<vars(?:[ >])")
ORDINARY_TRANSITION_TAG = re.compile(r"<transition(?:\s|>)")


def file_has_legacy(text: str) -> bool:
    return bool(LEGACY_TAG.search(text))


# ---------------------------------------------------------------------------
# Java inline XML handling
# ---------------------------------------------------------------------------

def extract_java_inline_xml(path: Path) -> list[dict]:
    """Find quest-definition XML inside Java text blocks. Returns spans of the block content only.

    State machine over text block delimiters: a triple-quote pair either opens or closes a block.
    Openers may carry a prefix (String xml = triple-quote); closers may carry a suffix.
    A line with two or three triple-quote pairs is a single-line block or an immediate
    close+open pair.
    """
    text = read_text_exact(path)
    lines = text.split("\n")
    results = []
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        count = line.count('"""')
        if count == 1:
            triple_pos = stripped.index('"""')
            before = stripped[:triple_pos]
            after = stripped[triple_pos + 3:]
            # opener: code before the triple quote ('String xml = """')
            # closer: code after the triple quote ('""";' or '""".formatted')
            is_closer = not before and (not after or after.startswith(';') or after.startswith(')') or after.startswith('.') or after.startswith('+'))
            if not is_closer:
                # opener
                start_line = i
                content_start = line.index('"""') + 3
                opener_prefix = line[:content_start].rstrip()
                buf = [line[content_start:]]
                j = i + 1
                closer_suffix = ""
                while j < len(lines):
                    end_triple = lines[j].find('"""')
                    if end_triple >= 0:
                        buf.append(lines[j][:end_triple])
                        closer_suffix = lines[j][end_triple + 3:]
                        break
                    buf.append(lines[j])
                    j += 1
                results.append({"start_line": start_line, "end_line": j, "content": "\n".join(buf),
                                "kind": "textblock", "opener_prefix": opener_prefix,
                                "closer_suffix": closer_suffix})
                i = j + 1
                continue
            i += 1
            continue
        if count >= 2:
            # single-line block:  """content""" (with optional prefix/suffix code)
            start_line = i
            first = line.index('"""')
            content_start = first + 3
            opener_prefix = line[:content_start].rstrip()
            content = line[content_start:]
            close = content.rfind('"""')
            if close >= 0:
                content = content[:close]
                closer_suffix = line[content_start + close + 3:]
            else:
                closer_suffix = ""
            results.append({"start_line": start_line, "end_line": start_line, "content": content,
                            "kind": "textblock", "opener_prefix": opener_prefix,
                            "closer_suffix": closer_suffix})
            i += 1
            continue
        i += 1
    # keep only blocks that contain quest XML (full documents or node/transition fragments)
    return [r for r in results if "<quest-definition" in r["content"]
            or "<node" in r["content"] or "<pro" in r["content"] or "<va" in r["content"]]


def extract_java_concat_xml(path: Path) -> list[dict]:
    """Find quest XML assembled from Java string-literal concatenation.

    Matches runs of adjacent string literals that form a single argument/expression:
    a first line starting with `"`, followed by continuation lines starting with `+ "`.
    The run ends when a line ends with a comma, semicolon, or closing paren (parameter
    boundary), or when the next line does not continue the concatenation. Returns one
    block per run with (start_line, end_line, content) where content is the unescaped
    concatenated text.
    """
    text = read_text_exact(path)
    lines = text.split("\n")
    results = []
    i = 0
    while i < len(lines):
        stripped = lines[i].strip()
        if stripped.startswith('"') or stripped.startswith('("') or stripped.startswith('( "'):
            if stripped.startswith('"""'):
                i += 1
                continue
            # candidate run start
            buf = []
            joined = ""
            trailing = ""
            j = i
            while j < len(lines):
                s = lines[j].strip()
                if s.startswith('"'):
                    quote_start = 0
                elif s.startswith('("') or s.startswith('( "'):
                    quote_start = s.index('"')
                elif re.match(r'^\+\s*"', s):
                    quote_start = s.index('"')
                else:
                    break
                content_part = s[quote_start:]
                end = content_part.find('"', 1)
                while end >= 0 and content_part[end - 1] == "\\":
                    end = content_part.find('"', end + 1)
                if end < 0:
                    break
                frag = content_part[1:end]
                frag = frag.replace('\\"', '"').replace("\\\\", "\\")
                buf.append(frag)
                joined += frag
                j += 1
                rest = content_part[end + 1:].strip()
                if rest and (rest.startswith(",") or rest.startswith(")")):
                    trailing = rest
                    break
                if not rest:
                    nxt = lines[j].strip() if j < len(lines) else ""
                    if not nxt.startswith("+") and not nxt.startswith('+"'):
                        trailing = ""
                        break
                # if rest has trailing code (e.g. `;`), the run ends
                if rest and not rest.startswith(",") and not rest.startswith(")"):
                    trailing = rest
                    break
            if len(buf) >= 1 and ("<pro" in joined or "<va" in joined):
                # only treat runs that start with an XML element as migratable XML;
                # replacement-assertion fragments (e.g. "label=..<project status=..")
                # are negative-test construction strings and must stay untouched
                if joined.lstrip().startswith("<"):
                    results.append({"start_line": i, "end_line": j - 1, "content": joined,
                                    "kind": "concat", "trailing": trailing})
            i = j if j > i else i + 1
        else:
            i += 1
    return results


def dedent_text_block(lines: list[str]) -> list[str]:
    """Remove Java text block common indentation from a list of content lines."""
    if not lines:
        return lines
    non_empty = [ln for ln in lines if ln.strip()]
    if not non_empty:
        return lines
    indent = min(len(ln) - len(ln.lstrip(" ")) for ln in non_empty)
    return [ln[indent:] if len(ln) >= indent else ln for ln in lines]


def reindent_text_block(lines: list[str], indent: int) -> list[str]:
    return [(" " * indent) + ln if ln.strip() else ln for ln in lines]


def java_inline_rewrite(block: dict, new_xml: str, lines: list[str]) -> list[str]:
    """Replace block['content'] inside lines with new_xml, preserving text-block structure.

    Returns the modified lines list. Only handles multi-line blocks whose content is a plain XML
    document (no format specifiers, no string concatenation inside).
    """
    content = block["content"]
    start = block["start_line"]
    end = block["end_line"]
    content_lines = content.split("\n")
    dedented = dedent_text_block(content_lines)
    opener = block.get("opener_prefix", "")
    closer = block.get("closer_suffix", "")
    if len(content_lines) == 1:
        # single-line block: rewrite inline
        lines[start] = lines[start].replace(content, new_xml)
        return lines
    indent = len(lines[start]) - len(lines[start].lstrip(" "))
    # opener_prefix already ends with the opening triple quote; build the replacement
    # preserving original indentation of the opener and closer lines.
    opener_line = lines[start]
    opener_indent = opener_line[:len(opener_line) - len(opener_line.lstrip())]
    closer_line = lines[end]
    closer_indent = closer_line[:len(closer_line) - len(closer_line.lstrip())]
    # The opener line may carry trailing content after the opening triple quote
    # (e.g. `("""<quest-definition ...>`); strip it so the migrated XML is not duplicated.
    opener_clean = opener_line[:opener_line.index('"""') + 3]
    # Text block content aligns at the opener line's indentation (Java convention).
    # Dedent the migrated XML, then re-indent every non-empty line at opener_indent,
    # keeping the relative indentation inside the XML intact.
    xml_lines = dedent_text_block(new_xml.split("\n"))
    replacement = [opener_clean]
    for ln in xml_lines:
        replacement.append(opener_indent + ln if ln.strip() else "")
    replacement.append(closer_indent + '"""' + closer)
    lines[start:end + 1] = replacement
    return lines


def java_concat_rewrite(block: dict, new_xml: str, lines: list[str]) -> list[str]:
    """Replace a concatenated string-literal XML run with the migrated XML.

    The run (start_line..end_line) is collapsed into a single quoted literal line
    when the new XML has no embedded newlines, or a multi-line run of quoted
    literals otherwise. Returns the modified lines list.
    """
    start = block["start_line"]
    end = block["end_line"]
    trailing = block.get("trailing", "")
    base_indent = len(lines[start]) - len(lines[start].lstrip(" "))
    prefix = " " * base_indent
    # continuation prefix: '+' if the original run used '+' continuations, else ''
    first_stripped = lines[start].strip()
    continuation = "+ " if any(lines[j].strip().startswith("+") for j in range(start + 1, end + 1)) else ""
    # first line keeps the original leading tokens before the string (e.g. '(' of a call)
    first_lead = first_stripped[:first_stripped.index('"')] if '"' in first_stripped else ""
    new_lines = new_xml.split("\n")
    if len(new_lines) == 1 and "\\n" in new_xml:
        # XML came from escaped \n literals in Java strings: re-split into one string per
        # source line, preserving the multi-line concatenation style of the original.
        parts = new_xml.split("\\n")
        replacement = []
        for idx, part in enumerate(parts):
            escaped = part.replace("\\", "\\\\").replace('"', '\\"')
            if idx == 0:
                # first line keeps the original leading tokens (e.g. '(' of a call)
                replacement.append(prefix + first_lead + '"' + escaped + '\\n"')
            elif idx == len(parts) - 1:
                replacement.append(prefix + continuation + '"' + escaped + '"' + trailing)
            else:
                replacement.append(prefix + continuation + '"' + escaped + '\\n"')
    elif len(new_lines) == 1:
        escaped = new_xml.replace("\\", "\\\\").replace('"', '\\"')
        replacement = [prefix + '"' + escaped + '"' + trailing]
    else:
        replacement = []
        for ln in new_lines:
            escaped = ln.replace("\\", "\\\\").replace('"', '\\"')
            replacement.append(prefix + '"' + escaped + '\n"')
        last = new_lines[-1].replace("\\", "\\\\").replace('"', '\\"')
        replacement[-1] = prefix + '"' + last + '"' + trailing
    lines[start:end + 1] = replacement
    return lines


# ---------------------------------------------------------------------------
# node migration on a single XML document (span-based rewrite)
# ---------------------------------------------------------------------------

def migrate_nodes_in_xml(xml_text: str, file_rel: str) -> tuple[str, int, list[str]]:
    """Rewrite legacy node wrappers to compact syntax. Returns (new_text, node_count, failures)."""
    try:
        root = parse_xml(xml_text)
    except ET.ParseError as e:
        return xml_text, 0, [{"file": file_rel, "reason": "parse_failure: " + str(e)}]

    # validate structure: every node either has legacy project or is already compact
    for node in root.iter():
        if localname(node.tag) != "node":
            continue
        has_project = any(localname(c.tag) == "project" for c in list(node))
        has_status = node.get("status") is not None
        direct_vars = [c for c in list(node) if localname(c.tag) == "var"]
        if has_project:
            if has_status or direct_vars:
                return xml_text, 0, [{"file": file_rel, "reason": "mixed node format"}]
            if len([c for c in list(node) if localname(c.tag) == "project"]) != 1:
                return xml_text, 0, [{"file": file_rel, "reason": "node has multiple project wrappers"}]
        elif not has_status:
            return xml_text, 0, [{"file": file_rel, "reason": "node missing status attribute"}]

    # collect node spans: the structure parser (lxml) owns semantics; a lexical,
    # depth-aware scan locates each element's span. Node tags inside comments or text
    # are never matched because spans start only at real opening tags.
    from lxml import etree as LETree
    parser = LETree.XMLParser(remove_comments=False, remove_pis=False, recover=False,
                              resolve_entities=False, no_network=True)
    lxml_root = LETree.fromstring(xml_text.encode("utf-8"), parser)
    lines = xml_text.split("\n")
    line_offsets = []
    offset = 0
    for ln in lines:
        line_offsets.append(offset)
        offset += len(ln) + 1  # +1 for the newline

    def line_start(line_no: int) -> int:
        return line_offsets[line_no - 1] if 1 <= line_no <= len(lines) else 0

    def open_tag_end(line_no: int, line_index: int) -> int:
        """Offset just past the '>' of the opening tag whose '<' sits at (line, col)."""
        base = line_start(line_no)
        i = base
        for _ in range(line_index):
            i = xml_text.find("<", i + 1)
        return i

    replacements = []
    node_spans = []
    # locate every <node...> opening tag occurrence on its line
    for match in re.finditer(r"<node\b", xml_text):
        node_spans.append(match.start())
    for start in node_spans:
        end = locate_element(xml_text, start)
        if end < 0:
            continue  # leave to compiler for errors
        node_xml = xml_text[start:end]
        proj_match = re.search(r"<project\b[^>]*>", node_xml)
        if not proj_match:
            continue  # already compact
        status_m = re.search(r'status\s*=\s*"([^"]*)"', proj_match.group(0))
        if not status_m:
            continue  # leave to compiler for errors
        status = status_m.group(1)
        # var children live between the project open tag and its matching close
        proj_open_end = node_xml.index(proj_match.group(0)) + len(proj_match.group(0))
        proj_element_end = locate_element(node_xml, node_xml.index(proj_match.group(0)))
        if proj_element_end < 0:
            continue
        inner_text = node_xml[proj_open_end:proj_element_end]
        # vars wrapper optional; collect var element spans inside the project body
        var_parts = []
        for vm in re.finditer(r"<var\b[^>]*?/>", inner_text):
            var_parts.append(vm.group(0))
        # build new node preserving the original open tag minus status, plus our status
        open_m = re.match(r"(\s*)<node\b[^>]*>", node_xml)
        if not open_m:
            continue
        open_tag = open_m.group(0)
        new_open = re.sub(r'\s+status\s*=\s*"[^"]*"', "", open_tag)
        new_open = new_open[:-1] + ' status="' + status + '">' if new_open.endswith(">") else new_open + ' status="' + status + '">'
        single_line = "\n" not in node_xml
        if not var_parts:
            new_node = new_open[:-1] + "/>"
        elif single_line:
            new_node = new_open + "".join(vp for vp in var_parts) + "</node>"
        else:
            node_indent = re.match(r"(\s*)", open_tag).group(1)
            # align vars at the original <var> indentation when present
            var_indent = node_indent + "  "
            vm = re.search(r"\n(\s+)<var\b", node_xml)
            if vm:
                var_indent = vm.group(1)
            close_indent = node_indent
            cm = re.search(r"\n(\s+)</node>", node_xml)
            if cm:
                close_indent = cm.group(1)
            new_node = new_open + "\n" + "\n".join(var_indent + vp for vp in var_parts) + "\n" + close_indent + "</node>"
        replacements.append((start, end, new_node))

    if not replacements:
        return xml_text, 0, []

    # apply from end to start
    new_text = xml_text
    count = 0
    for (start, end, new_node) in sorted(replacements, reverse=True):
        new_text = new_text[:start] + new_node + new_text[end:]
        count += 1
    return new_text, count, []


def migrate_blocks_in_xml(xml_text: str, file_rel: str) -> tuple[str, dict, list[dict], list[dict]]:
    """Apply strict domain-block matching to a fully migrated XML document.

    Returns (new_text, counts, no_strict_match, failures) where counts maps block name to count.
    Block replacements are applied on the compact-syntax document; the Java
    verifier arbitrates IR equality afterwards (mismatches roll back).
    """
    try:
        root = parse_xml(xml_text)
    except ET.ParseError as error:
        return xml_text, {}, [], [{
            "file": file_rel,
            "line": getattr(error, "position", (1, 0))[0],
            "category": "parse_failure",
            "reason": str(error),
        }]
    transitions_el = None
    for ch in root:
        if localname(ch.tag) == "transitions":
            transitions_el = ch
            break
    if transitions_el is None:
        return xml_text, {}, [], []
    replacements, unmatched = match_domain_blocks(transitions_el, root)
    # apply replacements by element span: locate the transitions element, then its
    # direct transition children with the depth-aware locator (span scan only)
    trans_start = xml_text.find("<transitions")
    if trans_start < 0:
        return xml_text, {}, [], []
    trans_end = locate_element(xml_text, trans_start)
    if trans_end < 0:
        return xml_text, {}, [], [{
            "file": file_rel, "line": xml_text.count("\n", 0, trans_start) + 1,
            "category": "span_failure", "reason": "could not locate transitions element end",
        }]
    # locate spans for ALL direct children of transitions (in document order) so the
    # absolute children indices from match_domain_blocks align with inner indices
    inner = []
    open_end = xml_text.find(">", trans_start)
    search_from = open_end + 1 if open_end >= 0 else trans_start
    while search_from < trans_end:
        lt = xml_text.find("<", search_from)
        if lt < 0 or lt >= trans_end:
            break
        if xml_text.startswith("<!--", lt):
            cm = COMMENT.match(xml_text, lt)
            search_from = cm.end() if cm else lt + 4
            continue
        if xml_text.startswith("<![CDATA[", lt):
            cm = CDATA.match(xml_text, lt)
            search_from = cm.end() if cm else lt + 9
            continue
        if xml_text.startswith("<?", lt):
            pi_end = xml_text.find("?>", lt)
            search_from = pi_end + 2 if pi_end >= 0 else lt + 2
            continue
        end = locate_element(xml_text, lt)
        if end < 0 or end > trans_end:
            break
        inner.append((lt, end))
        search_from = end
    # children count must match the span list; comments/whitespace-only mismatches
    # make span mapping unsafe, so refuse to rewrite this file
    if len(inner) != len(list(transitions_el)):
        return xml_text, {}, [], [{
            "file": file_rel, "line": xml_text.count("\n", 0, trans_start) + 1,
            "category": "span_failure",
            "reason": "structured transition children do not map one-to-one to lexical spans",
        }]

    def issue(start_index, end_index, reason):
        start_offset = inner[start_index][0]
        end_offset = inner[end_index][1]
        return {
            "file": file_rel,
            "line": xml_text.count("\n", 0, start_offset) + 1,
            "span": {"start": start_offset, "end": end_offset},
            "category": "ordinary-transition-run",
            "reason": reason,
        }

    no_strict = [issue(start, end, reason) for start, end, reason in unmatched]
    edits = []
    newline = "\r\n" if "\r\n" in xml_text else "\n"
    for (abs_start, abs_end, block_name, block_xml) in replacements:
        crossed_markup = False
        for index in range(abs_start, abs_end):
            gap = xml_text[inner[index][1]:inner[index + 1][0]]
            if "<!--" in gap or "<![CDATA[" in gap or "<?" in gap:
                crossed_markup = True
                break
        if crossed_markup:
            no_strict.append(issue(abs_start, abs_end,
                                   f"{block_name}: replacement would cross a comment, CDATA, or processing instruction"))
            continue
        s = inner[abs_start][0]
        e = inner[abs_end][1]
        line_start = xml_text.rfind("\n", 0, s) + 1
        indentation = xml_text[line_start:s]
        if not indentation.isspace() and indentation != "":
            no_strict.append(issue(abs_start, abs_end,
                                   f"{block_name}: child indentation is not a replaceable whitespace prefix"))
            continue
        rendered = block_xml.replace("\n", newline).replace(newline, newline + indentation)
        edits.append((s, e, block_name, rendered))
    new_text = xml_text
    counts = {}
    for (s, e, block_name, block_xml) in sorted(edits, reverse=True):
        new_text = new_text[:s] + block_xml + new_text[e:]
        counts[block_name] = counts.get(block_name, 0) + 1
    no_strict.sort(key=lambda item: (item["file"], item["span"]["start"]))
    return new_text, counts, no_strict, []


def migrate_nodes_in_fragment(fragment: str, file_rel: str) -> tuple[str, int, list[str], dict[str, str]]:
    """Migrate node wrappers inside a Java inline XML fragment.

    Fragments are wrapped into a synthetic root so ElementTree can parse them
    (multiple sibling roots are not well-formed). Format specifiers (%s / %d / {})
    are temporarily replaced by inert text and restored verbatim afterwards; node
    structure itself never contains format specifiers.
    """
    placeholders = {}

    def protect(match):
        key = "__PH" + str(len(placeholders)) + "__"
        placeholders[key] = match.group(0)
        return key

    protected = re.sub(r"%[sd]|\{\}", protect, fragment)
    # strip an XML declaration so the synthetic root stays the first element
    xml_decl = ""
    decl_m = re.match(r"\s*<\?xml[^>]*\?>", protected)
    if decl_m:
        xml_decl = decl_m.group(0)
        protected = protected[decl_m.end():]
    wrapped = "<synthetic-root>" + protected + "</synthetic-root>"
    new_text, count, fails = migrate_nodes_in_xml(wrapped, file_rel)
    if fails:
        return fragment, 0, fails, {}
    if new_text.startswith("<synthetic-root>") and new_text.endswith("</synthetic-root>"):
        new_text = new_text[len("<synthetic-root>"):-len("</synthetic-root>")]
    for key, value in placeholders.items():
        new_text = new_text.replace(key, value)
    return xml_decl + new_text, count, fails, placeholders


# ---------------------------------------------------------------------------
# domain block matching (strict; the Java verifier arbitrates every rewrite)
# ---------------------------------------------------------------------------

BLOCK_PRIORITY = ["npc-start", "npc-complete", "npc-item-report", "npc-report",
                  "counter-grid", "counter", "kill-chain", "kill-routes", "npc-dialog"]


@lru_cache(maxsize=None)
def _canonical(element):
    text = (element.text or "").strip()
    return (localname(element.tag), tuple(sorted(element.attrib.items())), text,
            tuple(_canonical(child) for child in list(element)))


def _same_elements(left, right) -> bool:
    return [_canonical(element) for element in left] == [_canonical(element) for element in right]


def _serialize(element) -> str:
    clone = ET.fromstring(ET.tostring(element, encoding="utf-8"))
    clone.tail = None
    return ET.tostring(clone, encoding="unicode", short_empty_elements=True).strip()


def _attrs(**values) -> str:
    return " ".join(f"{name.replace('_', '-')}={quoteattr(str(value))}"
                    for name, value in values.items() if value is not None)


def _block(tag: str, attributes: dict, children: list[str] | None = None) -> str:
    rendered_attributes = " ".join(f"{name}={quoteattr(str(value))}"
                                     for name, value in attributes.items())
    opening = f"<{tag}" + (" " + rendered_attributes if rendered_attributes else "")
    if not children:
        return opening + "/>"
    return opening + ">\n" + "\n".join("  " + child.replace("\n", "\n  ") for child in children) + f"\n</{tag}>"


def _direct_child(element, name: str):
    matches = [child for child in list(element) if localname(child.tag) == name]
    return matches[0] if len(matches) == 1 else None


def _children_or_empty(element, name: str) -> list:
    child = _direct_child(element, name)
    return list(child) if child is not None else []


@lru_cache(maxsize=None)
def _transition(element):
    if localname(element.tag) != "transition":
        return None
    allowed_attrs = {"source", "target", "priority"}
    if set(element.attrib) - allowed_attrs or "target" not in element.attrib:
        return None
    names = [localname(child.tag) for child in list(element)]
    allowed_names = {"event", "conditions", "actions", "after-commit"}
    if any(name not in allowed_names for name in names) or names.count("event") != 1:
        return None
    if any(names.count(name) > 1 for name in allowed_names):
        return None
    event_container = _direct_child(element, "event")
    if event_container is None or len(list(event_container)) != 1:
        return None
    return {
        "element": element,
        "source": element.get("source"),
        "target": element.get("target"),
        "priority": element.get("priority"),
        "event": list(event_container)[0],
        "conditions": _children_or_empty(element, "conditions"),
        "actions": _children_or_empty(element, "actions"),
        "after": _children_or_empty(element, "after-commit"),
    }


def _empty(parts, *, conditions=True, actions=True, after=False, priority=True) -> bool:
    return ((not conditions or not parts["conditions"])
            and (not actions or not parts["actions"])
            and (not after or not parts["after"])
            and (not priority or parts["priority"] is None))


def _simple(element, tag: str, attributes: dict | None = None) -> bool:
    if localname(element.tag) != tag or list(element) or (element.text or "").strip():
        return False
    return attributes is None or element.attrib == {name: str(value) for name, value in attributes.items()}


def _dialog_ids(raw: str) -> list[int] | None:
    seen = set()
    result = []
    for token in re.split(r"[\s,]+", raw.strip()):
        if not token:
            continue
        if ".." not in token:
            pieces = [token]
        else:
            if token.count("..") != 1:
                return None
            first_raw, last_raw = token.split("..")
            try:
                first, last = int(first_raw), int(last_raw)
            except ValueError:
                return None
            if first > last or last - first >= 256:
                return None
            pieces = [str(value) for value in range(first, last + 1)]
        for piece in pieces:
            try:
                value = int(piece)
            except ValueError:
                return None
            if value in seen or len(seen) >= 256:
                return None
            seen.add(value)
            result.append(value)
    return result or None


def _talk(parts):
    event = parts["event"]
    if localname(event.tag) != "talk-to-npc" or set(event.attrib) - {"npc-id", "dialog-id", "dialog-ids"}:
        return None
    if "npc-id" not in event.attrib or ("dialog-id" in event.attrib) == ("dialog-ids" in event.attrib):
        return None
    try:
        npc_id = int(event.get("npc-id"))
        dialogs = ([int(event.get("dialog-id"))] if "dialog-id" in event.attrib
                   else _dialog_ids(event.get("dialog-ids")))
    except (TypeError, ValueError):
        return None
    if npc_id <= 0 or not dialogs:
        return None
    return npc_id, dialogs


def _kill_ids(parts):
    event = parts["event"]
    if localname(event.tag) != "kill-npc" or set(event.attrib) - {"npc-id", "npc-ids"}:
        return None
    if ("npc-id" in event.attrib) == ("npc-ids" in event.attrib):
        return None
    raw = event.get("npc-id") if "npc-id" in event.attrib else event.get("npc-ids")
    try:
        values = [int(raw)] if "npc-id" in event.attrib else [int(value) for value in raw.split()]
    except (AttributeError, TypeError, ValueError):
        return None
    return values if values and all(value > 0 for value in values) and len(values) == len(set(values)) else None


def _single_kill_id(parts):
    ids = _kill_ids(parts)
    event = parts["event"]
    return ids[0] if ids is not None and len(ids) == 1 and "npc-id" in event.attrib else None


def _sync_mode(parts):
    if len(parts["after"]) != 1 or localname(parts["after"][0].tag) != "sync-quest-state":
        return None
    action = parts["after"][0]
    return action.get("mode") if set(action.attrib) == {"mode"} and not list(action) else None


def _expected_sync(context, target: str) -> str | None:
    node = context["node_by_label"].get(target)
    if node is None:
        return None
    return "LEVEL_AND_VISIBILITY_REFRESH" if node["status"] in {"REWARD", "COMPLETE"} else "PACKET_ONLY"


def _document_context(root) -> dict:
    nodes = []
    nodes_element = next((child for child in list(root) if localname(child.tag) == "nodes"), None)
    if nodes_element is not None:
        for node in list(nodes_element):
            if localname(node.tag) != "node":
                continue
            variables = {}
            for variable in list(node):
                if localname(variable.tag) == "var" and variable.get("name") is not None:
                    try:
                        variables[variable.get("name")] = int(variable.get("value"))
                    except (TypeError, ValueError):
                        pass
            nodes.append({"label": node.get("label"), "status": node.get("status"), "vars": variables})
    metadata = next((child for child in list(root) if localname(child.tag) == "metadata"), None)
    reward_groups = []
    if metadata is not None:
        rewards = next((child for child in list(metadata) if localname(child.tag) == "rewards"), None)
        grouped = next((child for child in list(metadata) if localname(child.tag) == "reward-groups"), None)
        if rewards is not None:
            reward_groups = [[dict(reward.attrib) for reward in list(rewards)
                              if localname(reward.tag) == "reward"]]
        elif grouped is not None:
            reward_groups = [[dict(reward.attrib) for reward in list(group)
                              if localname(reward.tag) == "reward"]
                             for group in list(grouped) if localname(group.tag) == "group"]
    return {
        "nodes": nodes,
        "node_by_label": {node["label"]: node for node in nodes if node["label"] is not None},
        "reward_groups": reward_groups,
    }


def _match_npc_start(children, cursor, context):
    if cursor + 4 >= len(children):
        return None
    parts = [_transition(children[cursor + offset]) for offset in range(5)]
    if any(part is None for part in parts):
        return None
    talks = [_talk(part) for part in parts]
    if any(talk is None for talk in talks):
        return None
    npc_id = talks[0][0]
    source = parts[0]["source"]
    target = parts[2]["target"]
    if npc_id <= 0 or source is None or target is None:
        return None
    if talks[0] != (npc_id, [31]) or talks[1] != (npc_id, [1007]) \
            or talks[2] != (npc_id, [1002]) or talks[3] != (npc_id, [20000]):
        return None
    if any(part["source"] != source for part in parts[:5]):
        return None
    if parts[0]["target"] != source or parts[1]["target"] != source:
        return None
    if parts[2]["target"] != target or parts[3]["target"] != target:
        return None
    if not (_empty(parts[0]) and _empty(parts[1])):
        return None
    if not (_same_elements(parts[0]["after"], [ET.fromstring('<show-quest-dialog dialog-id="1011"/>')])
            and _same_elements(parts[1]["after"], [ET.fromstring('<show-quest-dialog dialog-id="4"/>')])):
        return None
    start_eligible = [ET.fromstring("<start-eligible/>")]
    expected_accept_1002 = [ET.fromstring('<sync-quest-state mode="VISIBILITY_REFRESH"/>'),
                            ET.fromstring('<show-quest-dialog dialog-id="1003"/>')]
    expected_accept_20000 = [ET.fromstring('<sync-quest-state mode="VISIBILITY_REFRESH"/>'),
                             ET.fromstring("<close-dialog/>")]
    if not (_same_elements(parts[2]["conditions"], start_eligible)
            and _same_elements(parts[3]["conditions"], start_eligible)
            and _same_elements(parts[2]["actions"], parts[3]["actions"])
            and parts[2]["priority"] is None and parts[3]["priority"] is None
            and _same_elements(parts[2]["after"], expected_accept_1002)
            and _same_elements(parts[3]["after"], expected_accept_20000)):
        return None

    close_end = cursor + 4
    close_parts = parts[4]
    close_talk = talks[4]
    if close_talk == (npc_id, [1003, 1004, 20001]):
        close_candidates = [close_parts]
    elif close_talk == (npc_id, [1003]) and cursor + 6 < len(children):
        close_candidates = [_transition(children[cursor + offset]) for offset in (4, 5, 6)]
        if any(part is None for part in close_candidates):
            return None
        if [_talk(part) for part in close_candidates] != [
                (npc_id, [1003]), (npc_id, [1004]), (npc_id, [20001])]:
            return None
        close_end = cursor + 6
    else:
        return None
    for part in close_candidates:
        if part["source"] != source or part["target"] != source or not _empty(part) \
                or not _same_elements(part["after"], [ET.fromstring("<close-dialog/>")]):
            return None

    selection_sources = []
    position = close_end + 1
    while position < len(children):
        part = _transition(children[position])
        talk = _talk(part) if part is not None else None
        if talk != (npc_id, [1008]) or part["source"] is None or part["target"] != part["source"] \
                or not _empty(part) or not _same_elements(
                    part["after"], [ET.fromstring('<show-quest-selection-dialog dialog-id="10"/>')]):
            break
        if part["source"] in selection_sources:
            break
        selection_sources.append(part["source"])
        position += 1
    attributes = {"npc-id": npc_id, "source": source, "target": target}
    if selection_sources:
        attributes["selection-sources"] = " ".join(selection_sources)
    action_children = [_serialize(action) for action in parts[2]["actions"]]
    block_children = [_block("accept-actions", {}, action_children)] if action_children else None
    return position - 1, _block("npc-start", attributes, block_children)


def _reward_action_signature(action):
    if localname(action.tag) != "grant-reward" or list(action):
        return None
    if set(action.attrib) - {"kind", "id", "amount", "amount-mode"}:
        return None
    try:
        return (action.get("kind"), int(action.get("id")), int(action.get("amount")),
                action.get("amount-mode", "EXACT"))
    except (TypeError, ValueError):
        return None


def _reward_signature(reward):
    try:
        kind = reward["kind"]
        action_kind = "ITEM" if kind == "SELECTABLE_ITEM" else kind
        mode = "QUEST_BASE" if action_kind in {"GOLD", "KINAH", "EXP", "AP", "GP"} else "EXACT"
        return action_kind, int(reward["id"]), int(reward["amount"]), mode
    except (KeyError, TypeError, ValueError):
        return None


def _selected_rewards(context, complete_index):
    groups = context["reward_groups"]
    if not groups:
        return []
    if len(groups) == 1:
        return groups[0]
    return groups[complete_index] if 0 <= complete_index < len(groups) else None


def _match_npc_complete(children, cursor, context):
    position = cursor
    preview_ids = []
    npc_id = source = None
    while position < len(children):
        part = _transition(children[position])
        talk = _talk(part) if part is not None else None
        if part is None or talk is None or part["source"] is None or part["target"] != part["source"] \
                or not _empty(part) or not _same_elements(
                    part["after"], [ET.fromstring('<show-quest-dialog dialog-id="5"/>')]):
            break
        if npc_id is None:
            npc_id, source = talk[0], part["source"]
        if talk[0] != npc_id or part["source"] != source:
            break
        preview_ids.extend(talk[1])
        position += 1
    if not preview_ids or position >= len(children):
        return None

    routes = []
    common_after = None
    complete_index = None
    target = None
    while position < len(children):
        part = _transition(children[position])
        talk = _talk(part) if part is not None else None
        if part is None or talk is None or talk[0] != npc_id or part["source"] != source \
                or part["target"] == source or part["priority"] is not None \
                or part["conditions"] or not part["actions"]:
            break
        if target is None:
            target = part["target"]
        if part["target"] != target:
            break
        complete = part["actions"][-1]
        if localname(complete.tag) != "complete-quest" or set(complete.attrib) != {"reward-index"} or list(complete):
            break
        try:
            route_index = int(complete.get("reward-index"))
        except (TypeError, ValueError):
            break
        if complete_index is None:
            complete_index = route_index
        if route_index != complete_index or len(part["after"]) < 2 \
                or not _simple(part["after"][0], "refresh-player-stats") \
                or not _simple(part["after"][1], "sync-quest-state", {"mode": "COMPLETION"}):
            break
        after_key = [_canonical(action) for action in part["after"][2:]]
        if common_after is None:
            common_after = after_key
        if after_key != common_after:
            break
        grant_signatures = [_reward_action_signature(action) for action in part["actions"][:-1]]
        if any(signature is None for signature in grant_signatures):
            break
        routes.append({"dialogs": talk[1], "grants": grant_signatures, "after": part["after"][2:]})
        position += 1
    if not routes or target is None or complete_index is None:
        return None
    rewards = _selected_rewards(context, complete_index)
    if rewards is None:
        return None
    reward_signatures = [_reward_signature(reward) for reward in rewards]

    def unique_reward_index(signature):
        matches = [index for index, candidate in enumerate(reward_signatures) if candidate == signature]
        return matches[0] if len(matches) == 1 else None

    route_indices = []
    for route in routes:
        indices = [unique_reward_index(signature) for signature in route["grants"]]
        if any(index is None for index in indices):
            return None
        route_indices.append(indices)
    fixed = []
    offset = 0
    while route_indices and all(len(indices) > offset for indices in route_indices):
        candidate = route_indices[0][offset]
        if any(indices[offset] != candidate for indices in route_indices):
            break
        if rewards[candidate].get("kind") == "SELECTABLE_ITEM":
            break
        fixed.append(candidate)
        offset += 1
    if len(fixed) != len(set(fixed)):
        return None
    choice_indices = []
    for indices in route_indices:
        remaining = indices[offset:]
        if not remaining:
            choice_indices.append(None)
        elif len(remaining) == 1 and rewards[remaining[0]].get("kind") == "SELECTABLE_ITEM":
            choice_indices.append(remaining[0])
        else:
            return None

    trailing = routes[0]["after"]
    if trailing and _simple(trailing[-1], "show-quest-selection-dialog", {"dialog-id": "10"}):
        finish = "SELECTION_DIALOG"
        extras = trailing[:-1]
    elif trailing and _simple(trailing[-1], "close-dialog"):
        finish = "CLOSE_DIALOG"
        extras = trailing[:-1]
    else:
        finish = "NONE"
        extras = trailing

    leading_dialogs = []
    choices = []
    fallback_dialogs = []
    phase = "leading"
    for route, choice_index in zip(routes, choice_indices):
        if choice_index is None:
            if phase == "choice":
                phase = "fallback"
            if phase == "fallback":
                fallback_dialogs.extend(route["dialogs"])
            else:
                leading_dialogs.extend(route["dialogs"])
        else:
            if phase == "fallback" or len(route["dialogs"]) != 1:
                return None
            phase = "choice"
            choices.append((route["dialogs"][0], choice_index))
    attributes = {
        "npc-id": npc_id,
        "source": source,
        "target": target,
    }
    if fixed:
        attributes["fixed-reward-indices"] = " ".join(map(str, fixed))
    if leading_dialogs:
        attributes["dialog-ids"] = " ".join(map(str, leading_dialogs))
    attributes["complete-reward-index"] = complete_index
    attributes["preview-dialog-ids"] = " ".join(map(str, preview_ids))
    attributes["finish"] = finish
    block_children = [
        _block("choice", {"dialog-id": dialog_id, "reward-index": reward_index})
        for dialog_id, reward_index in choices
    ]
    if fallback_dialogs:
        block_children.append(_block("fallback", {"dialog-ids": " ".join(map(str, fallback_dialogs))}))
    if extras:
        block_children.append(_block("after-commit", {}, [_serialize(action) for action in extras]))
    if not leading_dialogs and not choices and not fallback_dialogs:
        return None
    return position - 1, _block("npc-complete", attributes, block_children or None)


def _match_npc_item_report(children, cursor, context):
    if cursor + 3 >= len(children):
        return None
    parts = [_transition(children[cursor + offset]) for offset in range(4)]
    if any(part is None for part in parts):
        return None
    talks = [_talk(part) for part in parts]
    if any(talk is None for talk in talks):
        return None
    npc_id = talks[0][0]
    if talks != [(npc_id, [39]), (npc_id, [39]), (npc_id, [20002]), (npc_id, [20002])]:
        return None
    source = parts[0]["source"]
    target = parts[0]["target"]
    if source is None or target is None or any(part["source"] != source for part in parts):
        return None
    if [part["target"] for part in parts] != [target, source, target, source] \
            or [part["priority"] for part in parts] != ["0", "1", "0", "1"]:
        return None
    has_item = parts[0]["conditions"]
    remove_item = parts[0]["actions"]
    if len(has_item) != 1 or len(remove_item) != 1 \
            or not _same_elements(parts[2]["conditions"], has_item) \
            or not _same_elements(parts[2]["actions"], remove_item) \
            or parts[1]["conditions"] or parts[1]["actions"] \
            or parts[3]["conditions"] or parts[3]["actions"]:
        return None
    condition, action = has_item[0], remove_item[0]
    if localname(condition.tag) != "has-item" or set(condition.attrib) != {"item-id", "count"} \
            or localname(action.tag) != "remove-item" or set(action.attrib) != {"item-id", "count"}:
        return None
    try:
        item_id = int(condition.get("item-id"))
        required = int(condition.get("count"))
        remove_raw = action.get("count")
        remove_count = remove_raw if remove_raw.upper() == "ALL" else int(remove_raw)
    except (AttributeError, TypeError, ValueError):
        return None
    if item_id <= 0 or required <= 0 or action.get("item-id") != str(item_id) \
            or (remove_count != "ALL" and remove_count != required):
        return None
    success_after = [ET.fromstring(f'<sync-quest-state mode="{_expected_sync(context, target)}"/>'),
                     ET.fromstring('<show-quest-dialog dialog-id="5"/>')]
    if _expected_sync(context, target) is None or not _same_elements(parts[0]["after"], success_after) \
            or not _same_elements(parts[2]["after"], success_after) \
            or not _same_elements(parts[1]["after"], [ET.fromstring('<show-quest-dialog dialog-id="2716"/>')]) \
            or not _same_elements(parts[3]["after"], [ET.fromstring("<close-dialog/>")]):
        return None
    attributes = {"npc-id": npc_id, "source": source, "target": target,
                  "item-id": item_id, "required": required}
    if remove_count == "ALL":
        attributes["remove-count"] = "ALL"
    return cursor + 3, _block("npc-item-report", attributes)


def _match_npc_report(children, cursor, context):
    if cursor + 1 >= len(children):
        return None
    first, second = _transition(children[cursor]), _transition(children[cursor + 1])
    if first is None or second is None:
        return None
    first_talk, second_talk = _talk(first), _talk(second)
    if first_talk is None or second_talk is None or first_talk[0] != second_talk[0] \
            or first_talk[1] != [31] or second_talk[1] != [1009]:
        return None
    source, target = first["source"], second["target"]
    if source is None or target is None or first["target"] != source or second["source"] != source \
            or not _empty(first) or not _empty(second):
        return None
    if len(first["after"]) != 1 or localname(first["after"][0].tag) != "show-quest-dialog" \
            or set(first["after"][0].attrib) != {"dialog-id"}:
        return None
    try:
        page = int(first["after"][0].get("dialog-id"))
    except (TypeError, ValueError):
        return None
    expected = [ET.fromstring(f'<sync-quest-state mode="{_expected_sync(context, target)}"/>'),
                ET.fromstring('<show-quest-dialog dialog-id="5"/>')]
    if page not in {1352, 2375, 10002} or _expected_sync(context, target) is None \
            or not _same_elements(second["after"], expected):
        return None
    return cursor + 1, _block("npc-report", {
        "npc-id": first_talk[0], "source": source, "target": target, "page": page,
    })


def _grid_arc(parts, context):
    cache = context.setdefault("grid_arc_cache", {})
    cache_key = id(parts["element"])
    if cache_key in cache:
        return cache[cache_key]
    npc_id = _single_kill_id(parts)
    source_node = context["node_by_label"].get(parts["source"])
    target_node = context["node_by_label"].get(parts["target"])
    if npc_id is None or source_node is None or target_node is None \
            or source_node["status"] != "START" or target_node["status"] != "START" \
            or not _empty(parts) or _sync_mode(parts) != "PACKET_ONLY":
        cache[cache_key] = None
        return None
    if source_node["vars"].keys() != target_node["vars"].keys():
        cache[cache_key] = None
        return None
    changed = [field for field in source_node["vars"]
               if target_node["vars"][field] != source_node["vars"][field]]
    if len(changed) != 1:
        cache[cache_key] = None
        return None
    field = changed[0]
    if target_node["vars"][field] != source_node["vars"][field] + 1:
        cache[cache_key] = None
        return None
    result = (field, npc_id)
    cache[cache_key] = result
    return result


def _match_counter_grid(children, cursor, context):
    arcs = []
    position = cursor
    while position < len(children):
        part = _transition(children[position])
        arc = _grid_arc(part, context) if part is not None else None
        if arc is None:
            break
        arcs.append((part, arc[0], arc[1]))
        position += 1
    if not arcs:
        return None
    fields = []
    groups = []
    for part, field, npc_id in arcs:
        if not fields or fields[-1] != field:
            if field in fields:
                return None
            fields.append(field)
            groups.append([])
        groups[-1].append((part, npc_id))
    start_nodes = [node for node in context["nodes"] if node["status"] == "START"]
    if not start_nodes or any(set(node["vars"]) != set(fields) for node in start_nodes):
        return None
    required = {field: max(node["vars"][field] for node in start_nodes) for field in fields}
    expected_product = 1
    for field in fields:
        if required[field] < 1 or min(node["vars"][field] for node in start_nodes) != 0:
            return None
        expected_product *= required[field] + 1
    if expected_product != len(start_nodes) \
            or len({tuple(node["vars"][field] for field in fields) for node in start_nodes}) != len(start_nodes):
        return None
    dimensions = []
    for field, group in zip(fields, groups):
        first_source = group[0][0]["source"]
        npc_ids = []
        for part, npc_id in group:
            if part["source"] != first_source:
                break
            npc_ids.append(npc_id)
        if not npc_ids or len(npc_ids) != len(set(npc_ids)):
            return None

        def expected(order):
            result = []
            sources = [node for node in order if node["vars"][field] < required[field]]
            for node in sources:
                target_vars = dict(node["vars"])
                target_vars[field] += 1
                targets = [candidate for candidate in start_nodes if candidate["vars"] == target_vars]
                if len(targets) != 1:
                    return None
                for npc in npc_ids:
                    result.append((node["label"], targets[0]["label"], npc))
            return result

        actual = []
        for part, npc_id in group:
            actual.append((part["source"], part["target"], npc_id))
        node_expected = expected(start_nodes)
        value_order = [node for value in range(required[field])
                       for node in start_nodes if node["vars"][field] == value]
        value_expected = expected(value_order)
        if actual == node_expected:
            source_order = "NODE"
        elif actual == value_expected:
            source_order = "VALUE_MAJOR"
        else:
            return None
        attributes = {"field": field, "required": required[field],
                      "npc-ids": " ".join(map(str, npc_ids))}
        if source_order != "NODE":
            attributes["source-order"] = source_order
        dimensions.append(_block("dimension", attributes))
    return position - 1, _block("counter-grid", {}, dimensions)


def _match_counter(children, cursor, context):
    if cursor + 1 >= len(children):
        return None
    continuing, completing = _transition(children[cursor]), _transition(children[cursor + 1])
    if continuing is None or completing is None or continuing["source"] is None \
            or continuing["source"] != continuing["target"] \
            or completing["source"] != continuing["source"] \
            or continuing["priority"] != "1" or completing["priority"] != "0" \
            or _canonical(continuing["event"]) != _canonical(completing["event"]) \
            or len(continuing["conditions"]) < 1 or len(completing["conditions"]) < 1 \
            or len(continuing["actions"]) != 1 or len(completing["actions"]) != 1:
        return None
    below, equal = continuing["conditions"][-1], completing["conditions"][-1]
    if localname(below.tag) != "variable-below" or localname(equal.tag) != "quest-variable-is" \
            or set(below.attrib) != {"field", "value"} or set(equal.attrib) != {"field", "value"} \
            or below.attrib != equal.attrib:
        return None
    try:
        required = int(below.get("value")) + 1
    except (TypeError, ValueError):
        return None
    field = below.get("field")
    increment = ET.fromstring(f'<increment-variable field={quoteattr(field)} delta="1"/>')
    if required < 1 or not _same_elements(continuing["conditions"][:-1], completing["conditions"][:-1]) \
            or not _same_elements(continuing["actions"], [increment]) \
            or not _same_elements(completing["actions"], [increment]) \
            or _sync_mode(continuing) != "PACKET_ONLY" \
            or _sync_mode(completing) != _expected_sync(context, completing["target"]):
        return None
    child_xml = [_block("event", {}, [_serialize(continuing["event"])])]
    if continuing["conditions"][:-1]:
        child_xml.append(_block("conditions", {}, [_serialize(condition)
                                                   for condition in continuing["conditions"][:-1]]))
    return cursor + 1, _block("counter", {
        "source": continuing["source"], "target": completing["target"],
        "field": field, "required": required,
    }, child_xml)


def _match_kill_chain(children, cursor, context):
    first = _transition(children[cursor])
    if first is None or _single_kill_id(first) is None or first["source"] is None \
            or first["priority"] is not None or first["actions"] \
            or _sync_mode(first) != _expected_sync(context, first["target"]):
        return None
    nodes = [first["source"], first["target"]]
    event_key = _canonical(first["event"])
    conditions = first["conditions"]
    position = cursor + 1
    while position < len(children):
        part = _transition(children[position])
        if part is None or part["source"] != nodes[-1] or part["target"] in nodes \
                or _single_kill_id(part) is None or _canonical(part["event"]) != event_key \
                or not _same_elements(part["conditions"], conditions) or part["actions"] \
                or part["priority"] is not None \
                or _sync_mode(part) != _expected_sync(context, part["target"]):
            break
        nodes.append(part["target"])
        position += 1
    if len(nodes) < 3:
        return None
    child_xml = [_block("event", {}, [_serialize(first["event"])])]
    if conditions:
        child_xml.append(_block("conditions", {}, [_serialize(condition) for condition in conditions]))
    return position - 1, _block("kill-chain", {"nodes": " ".join(nodes)}, child_xml)


def _match_kill_routes(children, cursor, context):
    first = _transition(children[cursor])
    if first is None or first["source"] is None or first["priority"] is not None \
            or first["conditions"] or first["actions"] \
            or _sync_mode(first) != _expected_sync(context, first["target"]):
        return None
    source, target = first["source"], first["target"]
    npc_ids = []
    position = cursor
    while position < len(children):
        part = _transition(children[position])
        npc_id = _single_kill_id(part) if part is not None else None
        if npc_id is None or part["source"] != source or part["target"] != target \
                or part["priority"] is not None or part["conditions"] or part["actions"] \
                or _sync_mode(part) != _expected_sync(context, target):
            break
        npc_ids.append(npc_id)
        position += 1
    if len(npc_ids) < 2 or len(npc_ids) != len(set(npc_ids)):
        return None
    return position - 1, _block("kill-routes", {
        "source": source, "target": target, "npc-ids": " ".join(map(str, npc_ids)),
    })


def _talk_info(element):
    parts = _transition(element)
    talk = _talk(parts) if parts is not None else None
    if parts is None or talk is None or len(talk[1]) != 1 or parts["source"] is None \
            or parts["target"] != parts["source"] or not _empty(parts) or len(parts["after"]) != 1:
        return None
    response = parts["after"][0]
    if localname(response.tag) not in {"show-quest-dialog", "show-quest-selection-dialog", "close-dialog"}:
        return None
    if localname(response.tag) == "close-dialog":
        if response.attrib or list(response):
            return None
    elif set(response.attrib) != {"dialog-id"} or list(response):
        return None
    return talk[0], talk[1][0], _serialize(response), parts["source"]


def _match_npc_dialog(children, cursor, context):
    first = _talk_info(children[cursor])
    if first is None:
        return None
    first_npc, _, response, source = first
    dialog_ids = []
    position = cursor
    while position < len(children):
        info = _talk_info(children[position])
        if info is None or info[0] != first_npc or info[2] != response or info[3] != source:
            break
        dialog_ids.append(info[1])
        position += 1
    if not dialog_ids or len(dialog_ids) != len(set(dialog_ids)):
        return None
    width = len(dialog_ids)
    npc_ids = [first_npc]
    while position + width <= len(children):
        chunk = [_talk_info(children[index]) for index in range(position, position + width)]
        if any(info is None for info in chunk):
            break
        npc_id = chunk[0][0]
        if npc_id in npc_ids or any(info[0] != npc_id or info[2] != response or info[3] != source
                                     for info in chunk) \
                or [info[1] for info in chunk] != dialog_ids:
            break
        npc_ids.append(npc_id)
        position += width
    if len(npc_ids) < 2:
        return None
    return position - 1, _block("npc-dialog", {
        "source": source,
        "npc-ids": " ".join(map(str, npc_ids)),
        "dialog-ids": " ".join(map(str, dialog_ids)),
    }, [response])


MATCHERS = {
    "npc-start": _match_npc_start,
    "npc-complete": _match_npc_complete,
    "npc-item-report": _match_npc_item_report,
    "npc-report": _match_npc_report,
    "counter-grid": _match_counter_grid,
    "counter": _match_counter,
    "kill-chain": _match_kill_chain,
    "kill-routes": _match_kill_routes,
    "npc-dialog": _match_npc_dialog,
}


def _no_match_reason(element) -> str:
    parts = _transition(element)
    if parts is None:
        return "ordinary transition could not be parsed structurally"
    if parts["priority"] is not None:
        return "explicit priority is not part of any exact remaining block template"
    if parts["conditions"] or parts["actions"]:
        return "non-standard conditions or actions prevent an exact domain-block expansion"
    if localname(parts["event"].tag) == "talk-to-npc":
        return "talk route is isolated, incomplete, or has a special response"
    return "no exact domain-block expansion matched this continuous transition run"


def match_domain_blocks(transitions_el, root=None):
    """Apply the fixed nine-block priority at each ordinary transition cursor.

    Returns (replacements, unmatched), where each replacement is
    (start_index, end_index, block_name, block_xml), and unmatched contains
    (start_index, end_index, reason) for every preserved ordinary run.
    """
    children = list(transitions_el)
    context = _document_context(root) if root is not None else {
        "nodes": [], "node_by_label": {}, "reward_groups": [],
    }
    replacements = []
    consumed = set()
    cursor = 0
    while cursor < len(children):
        if localname(children[cursor].tag) != "transition":
            cursor += 1
            continue
        matched = None
        for block_name in BLOCK_PRIORITY:
            candidate = MATCHERS[block_name](children, cursor, context)
            if candidate is not None:
                end, block_xml = candidate
                if end >= cursor and all(localname(children[index].tag) == "transition"
                                         for index in range(cursor, end + 1)):
                    matched = end, block_name, block_xml
                    break
        if matched is None:
            cursor += 1
            continue
        end, block_name, block_xml = matched
        replacements.append((cursor, end, block_name, block_xml))
        consumed.update(range(cursor, end + 1))
        cursor = end + 1

    unmatched = []
    cursor = 0
    while cursor < len(children):
        if localname(children[cursor].tag) != "transition" or cursor in consumed:
            cursor += 1
            continue
        start = cursor
        reasons = []
        while cursor < len(children) and localname(children[cursor].tag) == "transition" \
                and cursor not in consumed:
            reasons.append(_no_match_reason(children[cursor]))
            cursor += 1
        unmatched.append((start, cursor - 1, reasons[0]))
    return replacements, unmatched


# ---------------------------------------------------------------------------
# per-file analysis (process-safe; no writes and no shared XML parser state)
# ---------------------------------------------------------------------------

def legacy_locations(path: Path, repo_root: Path, text: str | None = None) -> list[dict]:
    rel = str(path.relative_to(repo_root))
    try:
        source = read_text_exact(path) if text is None else text
    except (OSError, UnicodeDecodeError):
        return []
    if path.suffix == ".java":
        locations = []
        blocks = extract_java_inline_xml(path) + extract_java_concat_xml(path)
        for block in blocks:
            if file_has_legacy(block["content"]):
                locations.append({"file": rel, "line": block["start_line"] + 1})
        return locations
    return [{"file": rel}] if file_has_legacy(source) else []


def analyze_file(task: tuple[str, str]) -> dict:
    """Analyze and rewrite one file in isolation. The caller owns aggregation and all writes."""
    # Element instances are file-local; bound cache growth to one analysis and avoid retaining
    # parsed documents across worker tasks.
    _canonical.cache_clear()
    _transition.cache_clear()
    path = Path(task[0])
    repo_root = Path(task[1])
    rel = str(path.relative_to(repo_root))
    result = {
        "path": str(path),
        "candidate": None,
        "parse_failures": [],
        "unsupported_inline_xml": [],
        "remaining_legacy_wrappers": [],
        "no_strict_match": [],
    }
    try:
        original_text = read_text_exact(path)
    except (OSError, UnicodeDecodeError) as e:
        result["parse_failures"].append({"file": rel, "reason": str(e)})
        return result

    if path.suffix == ".java":
        if not file_has_legacy(original_text):
            return result
        try:
            blocks = extract_java_inline_xml(path) + extract_java_concat_xml(path)
            new_lines = original_text.split("\n")
            node_total = 0
            changed = False
            legacy_blocks = sorted(
                [block for block in blocks if file_has_legacy(block["content"])],
                key=lambda block: block["start_line"], reverse=True)
            for block in legacy_blocks:
                new_xml, count, failures, placeholders = migrate_nodes_in_fragment(block["content"], rel)
                if failures:
                    result["unsupported_inline_xml"].append({
                        "file": rel,
                        "line": block["start_line"] + 1,
                        "reason": failures[0]["reason"],
                    })
                    return result
                if not count:
                    continue
                try:
                    if block["kind"] == "concat":
                        new_lines = java_concat_rewrite(block, new_xml, new_lines)
                    else:
                        new_lines = java_inline_rewrite(block, new_xml, new_lines)
                    for key, value in placeholders.items():
                        new_lines = [line.replace(key, value) for line in new_lines]
                except ValueError as e:
                    result["unsupported_inline_xml"].append({
                        "file": rel,
                        "line": block["start_line"] + 1,
                        "reason": "unsupported_inline_xml: " + str(e),
                    })
                    return result
                node_total += count
                changed = True
            if changed:
                result["candidate"] = {
                    "new_text": "\n".join(new_lines),
                    "node_count": node_total,
                    "block_counts": {},
                }
                if file_has_legacy(result["candidate"]["new_text"]):
                    result["remaining_legacy_wrappers"] = [{"file": rel}]
        except (OSError, UnicodeDecodeError, ET.ParseError) as e:
            result["parse_failures"].append({"file": rel, "reason": str(e)})
        return result

    has_legacy = file_has_legacy(original_text)
    if not has_legacy and not ORDINARY_TRANSITION_TAG.search(original_text):
        return result
    compact_text = original_text
    node_count = 0
    if has_legacy:
        compact_text, node_count, failures = migrate_nodes_in_xml(original_text, rel)
        if failures:
            result["parse_failures"].extend(failures)
            return result
    block_text, block_counts, no_strict_match, block_failures = migrate_blocks_in_xml(compact_text, rel)
    result["no_strict_match"].extend(no_strict_match)
    if block_failures:
        result["parse_failures"].extend(block_failures)
        return result
    if node_count or block_counts:
        result["candidate"] = {
            "new_text": block_text,
            "node_count": node_count,
            "block_counts": block_counts,
        }
        if file_has_legacy(block_text):
            result["remaining_legacy_wrappers"] = [{"file": rel}]
    return result


def analyze_files(files: list[Path], workers: int) -> list[dict]:
    tasks = [(str(path), str(REPO_ROOT)) for path in files]
    if len(tasks) < 2 or workers == 1:
        return [analyze_file(task) for task in tasks]
    chunksize = max(1, len(tasks) // (workers * 8))
    with ProcessPoolExecutor(max_workers=workers) as executor:
        return list(executor.map(analyze_file, tasks, chunksize=chunksize))


def collect_legacy_locations(files: list[Path], workers: int) -> list[dict]:
    tasks = [(str(path), str(REPO_ROOT)) for path in files]
    if len(tasks) < 2 or workers == 1:
        groups = [legacy_locations(Path(path), REPO_ROOT) for path, _ in tasks]
    else:
        # Reuse full per-file analysis so worker entrypoints remain spawn-picklable.
        groups = [result["remaining_legacy_wrappers"] for result in analyze_files(files, workers)]
    return [item for group in groups for item in group]


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def main() -> int:
    parser = argparse.ArgumentParser(description="Quest XML compact syntax migration (scan-only by default)")
    parser.add_argument("--apply", action="store_true", help="write migrated files (default: scan only)")
    parser.add_argument("--force-dirty", action="store_true",
                        help="coordinator-only: allow explicitly authorized dirty migration targets")
    parser.add_argument("--workers", type=int, default=DEFAULT_WORKERS,
                        help="file-analysis worker processes (default: min(CPU count, 32))")
    parser.add_argument("paths", nargs="*", help="optional path filters, may only narrow the scan")
    args = parser.parse_args()
    if args.workers < 1:
        parser.error("--workers must be at least 1")

    if args.force_dirty and not args.apply:
        parser.error("--force-dirty requires --apply")
    report = empty_report("apply" if args.apply else "scan")

    # collect candidate files
    files = []
    for source_name, roots in SCAN_SOURCES.items():
        for root in roots:
            if not root.exists():
                continue
            for pattern in ("*.xml", "*.java") if source_name == "test_java" else ("*.xml",):
                files.extend(root.rglob(pattern))
    files = sorted(set(files))
    if args.paths:
        allowed = [str(Path(p).absolute()) for p in args.paths]
        selected = []
        for file in files:
            resolved = str(file.absolute())
            if resolved in allowed or any(resolved.startswith(path.rstrip(os.sep) + os.sep) for path in allowed):
                selected.append(file)
        files = selected
    report["scan_count"] = len(files)
    workers = min(args.workers, max(1, len(files)))
    report["parallelism"] = {"python_processes": workers}
    analysis = analyze_files(files, workers)
    candidates = []
    for result in analysis:
        report["parse_failures"].extend(result["parse_failures"])
        report["unsupported_inline_xml"].extend(result["unsupported_inline_xml"])
        report["remaining_legacy_wrappers"].extend(result["remaining_legacy_wrappers"])
        report["no_strict_match"].extend(result["no_strict_match"])
        candidate = result["candidate"]
        if candidate is not None:
            candidates.append((Path(result["path"]), candidate["new_text"], candidate["node_count"],
                               candidate["block_counts"], []))
    report["node_migration_count"] = sum(c[2] for c in candidates if c[1] is not None)
    report["candidate_files"] = [str(c[0].relative_to(REPO_ROOT)) for c in candidates if c[1] is not None]
    report["candidate_files"].sort()
    report["no_strict_match"].sort(key=lambda item: (item.get("file", ""),
                                                     item.get("span", {}).get("start", -1),
                                                     item.get("reason", "")))
    for c in candidates:
        if c[1] is not None:
            for block_name, block_count in c[3].items():
                report["domain_block_counts"][block_name] = (
                    report["domain_block_counts"].get(block_name, 0) + block_count)

    if not args.apply:
        report["changed_file_count"] = len([c for c in candidates if c[1] is not None])
        write_report(report)
        print(f"scan complete: {len(files)} files, "
              f"{report['changed_file_count']} would change, {report['node_migration_count']} node rewrites")
        for c in candidates:
            if c[1] is None:
                print(f"  unsupported: {c[0].relative_to(REPO_ROOT)}: {c[3]}")
        return 0

    # No write is permitted if any preflight analysis failed. This includes malformed XML,
    # unsupported inline reconstruction, and any legacy wrapper left in an input target.
    preflight_errors = (report["parse_failures"] + report["unsupported_inline_xml"]
                        + report["remaining_legacy_wrappers"])
    if preflight_errors:
        write_report(report)
        print("ABORT: preflight analysis failed; no files written", file=sys.stderr)
        return 1

    # --apply: preflight
    to_write = [c for c in candidates if c[1] is not None]
    dirty = []
    if to_write:
        try:
            dirty_names = _git_dirty_paths()
        except RuntimeError as error:
            report["dirty"].append({"file": "<git-status>", "reason": str(error)})
            write_report(report)
            print("ABORT: unable to determine dirty targets; batch not written", file=sys.stderr)
            return 1
        for path, new_text, count, block_counts, fails in to_write:
            rel = _relative_target(path)
            if rel in dirty_names:
                dirty.append({"file": rel})
        # files not in the batch that are dirty are allowed (unrelated)
    if dirty and args.force_dirty:
        report["authorized_dirty"] = dirty
        dirty = []
    report["dirty"] = dirty
    if dirty:
        write_report(report)
        print(f"ABORT: {len(dirty)} dirty target file(s); batch not written", file=sys.stderr)
        return 1

    # verifier availability: build test classes first
    verifier_build = subprocess.run(["rtk", "mvn", "-q", "-o", "test-compile"],
                                    capture_output=True, text=True, cwd=REPO_ROOT)
    if verifier_build.returncode != 0:
        # fall back to online
        verifier_build = subprocess.run(["rtk", "mvn", "-q", "test-compile"],
                                        capture_output=True, text=True, cwd=REPO_ROOT)
    if verifier_build.returncode != 0:
        report["compile_failures"].append({"file": "<verifier>", "reason": "test-compile failed"})
        write_report(report)
        print("ABORT: Java verifier cannot build", file=sys.stderr)
        return 1

    # write with backup + verify each file; rollback on failure
    written = []
    for path, new_text, count, block_counts, fails in to_write:
        rel = str(path.relative_to(REPO_ROOT))
        backup = BEFORE_DIR / rel
        backup.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, backup)
        tmp = path.with_name(path.name + ".migrate.tmp")
        tmp.write_text(new_text, encoding="utf-8")
        os.replace(tmp, path)
        written.append(path)

    # Java IR verification for every rewritten XML file (relative paths so the verifier can
    # map each file to its target/quest-xml-migration/before/<rel> backup). Java test sources
    # are verified by test-compile + focused tests instead: their inline XML lives in text
    # blocks / concatenated literals and is not a standalone document for the file verifier.
    verify_files = [str(p.relative_to(REPO_ROOT)) for p in written if p.suffix == ".xml"]
    ok = True
    if verify_files:
        manifest = tempfile.NamedTemporaryFile("w", delete=False, suffix=".list")
        manifest.write("\n".join(verify_files) + "\n")
        manifest.close()
        output_file = str(REPORT_DIR / "verify.out")
        Path(output_file).unlink(missing_ok=True)
        verifier_args = ("--args-file " + manifest.name
                         + " --output-file " + output_file
                         + " --before-dir " + str(BEFORE_DIR)
                         + " --threads " + str(DEFAULT_WORKERS))
        result = subprocess.run(
            ["rtk", "mvn", "-q", "-o", "exec:java",
             "-Dexec.mainClass=com.aionemu.gameserver.questEngine.definition.QuestXmlMigrationVerifier",
             "-Dexec.classpathScope=test",
             "-Dexec.additionalClasspathElements=target/test-classes",
             "-Dexec.args=" + verifier_args],
            capture_output=True, text=True, cwd=REPO_ROOT)
        if result.returncode != 0 and not os.path.exists(output_file):
            # retry online once
            result = subprocess.run(
                ["rtk", "mvn", "-q", "exec:java",
                 "-Dexec.mainClass=com.aionemu.gameserver.questEngine.definition.QuestXmlMigrationVerifier",
                 "-Dexec.classpathScope=test",
                 "-Dexec.additionalClasspathElements=target/test-classes",
                 "-Dexec.args=" + verifier_args],
                capture_output=True, text=True, cwd=REPO_ROOT)
        expected = set(verify_files)
        seen = {}
        summary = None
        if os.path.exists(output_file):
            for raw_line in Path(output_file).read_text(encoding="utf-8").splitlines():
                line = raw_line.strip()
                if not line:
                    continue
                if line.startswith("SUMMARY "):
                    summary = line
                    continue
                parts = line.split(" ", 2)
                if len(parts) < 2 or parts[1] not in expected:
                    report["compile_failures"].append({"file": "<verifier>", "reason": "unexpected verifier result: " + line})
                    ok = False
                    continue
                seen[parts[1]] = seen.get(parts[1], 0) + 1
                if parts[0] == "MISMATCH":
                    report["ir_mismatches"].append({"file": parts[1], "first_diff": parts[2] if len(parts) > 2 else ""})
                    ok = False
                elif parts[0] == "PARSE_FAIL":
                    report["compile_failures"].append({"file": parts[1], "reason": parts[2] if len(parts) > 2 else ""})
                    ok = False
                elif parts[0] != "OK":
                    report["compile_failures"].append({"file": parts[1], "reason": "unknown verifier result: " + line})
                    ok = False
        else:
            report["compile_failures"].append({"file": "<verifier>",
                                               "reason": "verifier produced no output (mvn rc="
                                               + str(result.returncode) + ")"})
            ok = False
        missing = sorted(expected - set(seen))
        duplicate = sorted(path for path, count in seen.items() if count != 1)
        if missing:
            report["compile_failures"].append({"file": "<verifier>", "reason": "missing results: " + ", ".join(missing)})
            ok = False
        if duplicate:
            report["compile_failures"].append({"file": "<verifier>", "reason": "duplicate results: " + ", ".join(duplicate)})
            ok = False
        expected_summary = "SUMMARY mismatches=0 failures=0"
        if summary is None or not summary.startswith(expected_summary):
            report["compile_failures"].append({"file": "<verifier>", "reason": "invalid summary: " + str(summary)})
            ok = False
        if result.returncode != 0:
            report["compile_failures"].append({"file": "<verifier>", "reason": "verifier exit code " + str(result.returncode)})
            ok = False
        Path(manifest.name).unlink(missing_ok=True)

    if not ok:
        # rollback all written files
        for path in written:
            rel = str(path.relative_to(REPO_ROOT))
            backup = BEFORE_DIR / rel
            if backup.exists():
                shutil.copy2(backup, path)
        report["changed_file_count"] = len([c for c in candidates if c[1] is not None])
        write_report(report)
        print("ABORT: verification failed; all files rolled back", file=sys.stderr)
        return 1

    report["changed_file_count"] = len(written)
    report["remaining_legacy_wrappers"] = collect_legacy_locations(files, workers)
    write_report(report)
    print(f"apply complete: {len(written)} files migrated, {report['node_migration_count']} node rewrites")
    return 0


if __name__ == "__main__":
    sys.exit(main())
