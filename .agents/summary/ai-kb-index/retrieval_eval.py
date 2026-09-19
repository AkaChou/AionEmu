#!/usr/bin/env python3
"""Blind retrieval evaluation for the memory-bank Pattern index.

Query source: the original `.agents/summary/**` evidence documents that Pattern cards reference.
Each query is built from a summary document (title + first symptom-ish section) and expected to
rank one of the Patterns that reference that document. Trivially identifying tokens are stripped
before scoring: summary paths, owning topic directory, Pattern IDs and commit hashes.

Two calibration rules keep the metric honest:

- a document cited by several Patterns is scored against the whole expected set;
- documents without a symptom/现象 section (pure acceptance or measurement records) are reported
  separately as low-signal instead of being counted as misses.

Run:
    python3 -B .agents/summary/ai-kb-index/retrieval_eval.py
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
MEMORY_BANK = REPO_ROOT / ".agents/memory-bank"
SEARCH_TOOL = MEMORY_BANK / "search_memory_bank.py"

PATTERN_SECTION = re.compile(r"^## \[(?P<pid>[A-Z][A-Z0-9]+-\d{3})\]", re.MULTILINE)
SUMMARY_REF = re.compile(r"\.agents/summary/[A-Za-z0-9._/-]+\.md")
H1 = re.compile(r"^#\s+(?P<title>.+)$", re.MULTILINE)
HEADING = re.compile(r"^(?P<level>#{2,4})\s+(?P<title>.+)$", re.MULTILINE)
SYMPTOM_HEADING = re.compile(r"症状|现象|问题|Symptom|Bug|复现|背景|结论")
CODE_FENCE = re.compile(r"```.*?```", re.DOTALL)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Evaluate memory-bank Pattern retrieval.")
    parser.add_argument("--limit", type=int, default=5, help="ranking depth, default 5")
    parser.add_argument("--out-dir", type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument("--date", default="2026-09-19", help="report date stamp")
    parser.add_argument("--label", default="", help="optional suffix for the output file names")
    return parser.parse_args()


def evidence_owners() -> dict[Path, set[str]]:
    """Return {summary doc: {Pattern IDs citing it}} for every Pattern card."""
    owners: dict[Path, set[str]] = {}
    for card in sorted((MEMORY_BANK / "patterns").glob("*.md")):
        text = card.read_text(encoding="utf-8")
        heads = list(PATTERN_SECTION.finditer(text))
        for index, head in enumerate(heads):
            end = heads[index + 1].start() if index + 1 < len(heads) else len(text)
            section = text[head.start() : end]
            for ref in set(SUMMARY_REF.findall(section)):
                doc = REPO_ROOT / ref
                if doc.is_file() and doc.suffix == ".md":
                    owners.setdefault(doc, set()).add(head.group("pid"))
    return owners


def first_matching_section(body: str) -> str:
    """Return the text of the first symptom-like section, or the first paragraph."""
    heads = list(HEADING.finditer(body))
    for index, head in enumerate(heads):
        if SYMPTOM_HEADING.search(head.group("title")):
            end = heads[index + 1].start() if index + 1 < len(heads) else len(body)
            text = body[head.end() : end].strip()
            if text:
                return text
    for block in re.split(r"\n\s*\n", body):
        cleaned = block.strip()
        if cleaned and not cleaned.startswith("#"):
            return cleaned
    return body.strip()


def symptom_signal(doc: Path) -> bool:
    """True when the document actually describes a symptom rather than just recording a run."""
    text = CODE_FENCE.sub(" ", doc.read_text(encoding="utf-8"))
    for head in HEADING.finditer(text):
        if SYMPTOM_HEADING.search(head.group("title")):
            return True
    return False


def sanitize(value: str, doc: Path) -> str:
    """Strip identifying tokens so the query cannot trivially match the evidence path."""
    value = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", value)
    value = re.sub(r"\.agents/summary/[A-Za-z0-9._/-]+", " ", value)
    value = re.sub(rf"\b{re.escape(doc.parent.name)}\b", " ", value)
    value = re.sub(r"\b[A-Z][A-Z0-9]+-\d{3}\b", " ", value)
    value = re.sub(r"\b[0-9a-f]{7,40}\b", " ", value)
    value = re.sub(r"[`*_>#|]", " ", value)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def build_query(doc: Path) -> str:
    text = CODE_FENCE.sub(" ", doc.read_text(encoding="utf-8"))
    head = H1.search(text)
    title = head.group("title").strip() if head else doc.stem
    body = text[head.end() :] if head else text
    section = first_matching_section(body)
    return sanitize(f"{title}。{section}", doc)[:400]


def run_search(query: str, limit: int) -> list[dict]:
    proc = subprocess.run(
        [sys.executable, "-B", str(SEARCH_TOOL), query, "--json", "--limit", str(limit)],
        capture_output=True,
        text=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"search failed ({proc.returncode}): {proc.stderr.strip()}")
    return json.loads(proc.stdout)["matches"]


def render_report(date: str, results: list[dict], limit: int) -> str:
    scored = [item for item in results if item["signal"] == "high"]
    low_signal = [item for item in results if item["signal"] == "low"]
    total = len(scored)
    top1 = sum(1 for item in scored if item["rank"] == 1)
    top3 = sum(1 for item in scored if item["rank"] and item["rank"] <= 3)
    topn = sum(1 for item in scored if item["rank"])
    mrr = sum(1.0 / item["rank"] for item in scored if item["rank"]) / total if total else 0.0

    missed_patterns: dict[str, int] = {}
    for item in scored:
        if not item["rank"] or item["rank"] > 3:
            for pid in item["expected"]:
                missed_patterns[pid] = missed_patterns.get(pid, 0) + 1
    shared = [item for item in results if len(item["expected"]) > 1]

    lines = [
        f"# Memory Bank 检索盲检报告（{date}）",
        "",
        "## 方法",
        "",
        "- 查询来源：Pattern `evidence` 字段引用的 `.agents/summary/**` 原始证据文档，取标题 + 首个症状/结论段落（截断 400 字）。",
        "- 去标识处理：查询中移除 summary 路径、所属 topic 目录名、Pattern ID 与 commit hash，避免与卡片证据路径直接命中。",
        "- 打分：直接调用仓库现有 `search_memory_bank.py --json`，与 Agent 实际使用路径一致。",
        "- 期望集合：一份证据被多个 Pattern 引用时，命中其中任意一个都算成功。",
        "- 低信号过滤：没有「症状/现象/问题/Symptom」章节的文档（纯验收记录、量化记录）不计入命中率，单列。",
        "- 已知偏差：查询文本与元数据同源，命中率偏乐观；本报告用于发现「连原始证据都找不到」的卡片，而不是估计真实用户检索上限。",
        "",
        "## 结果",
        "",
        f"- 有效查询数：{total}（低信号文档 {len(low_signal)} 份已剔除）",
        f"- Top-1 命中：{top1}/{total}（{top1 / total:.0%}）" if total else "- 查询数为 0",
        f"- Top-3 命中：{top3}/{total}（{top3 / total:.0%}）" if total else "- ",
        f"- Top-{limit} 命中：{topn}/{total}（{topn / total:.0%}）" if total else "- ",
        f"- MRR：{mrr:.3f}" if total else "- ",
        "",
        "### Top-3 未命中的查询（需要补元数据的候选）",
        "",
    ]
    missed_queries = [item for item in scored if not item["rank"] or item["rank"] > 3]
    if missed_queries:
        for item in sorted(missed_queries, key=lambda i: i["rank"] or 99):
            ranked = ", ".join(f"{m['pattern_id']}({m['score']})" for m in item["top"][:3])
            lines.append(
                f"- 期望 `{', '.join(sorted(item['expected']))}`：查询「{item['query'][:60]}…」"
                f" → {ranked or '无匹配'}"
            )
    else:
        lines.append("- 无：所有有效查询都能在 Top-3 内找回对应 Pattern。")

    lines += ["", f"### 低信号文档（{len(low_signal)} 份，未计入命中率）", ""]
    for item in sorted(low_signal, key=lambda i: i["doc"]):
        lines.append(f"- `{item['doc']}`（期望 {', '.join(sorted(item['expected']))}）")

    lines += ["", "### 多 Pattern 共用证据", ""]
    if shared:
        for item in sorted(shared, key=lambda i: i["doc"]):
            hits = "、".join(sorted(item["expected"]))
            lines.append(f"- {hits} 共用 `{item['doc']}`：排名 {item['rank'] or 'miss'}")
    else:
        lines.append("- 无")

    lines += ["", "## 明细", "", "| 查询（截断） | 期望 | 排名 | 命中 |", "|---|---|---|---|"]
    for item in sorted(scored, key=lambda i: (i["rank"] or 99, i["expected"])):
        rank = item["rank"] or "miss"
        hit = item["hit"] or "-"
        lines.append(
            f"| {item['query'][:50]}… | `{', '.join(sorted(item['expected']))}` | {rank} | `{hit}` |"
        )
    return "\n".join(lines) + "\n"


def main() -> None:
    args = parse_args()
    owners = evidence_owners()
    results = []
    for doc, expected in sorted(owners.items(), key=lambda item: str(item[0])):
        query = build_query(doc)
        signal = "high" if symptom_signal(doc) else "low"
        if not query:
            results.append(
                {
                    "expected": sorted(expected),
                    "doc": str(doc.relative_to(REPO_ROOT)),
                    "query": "",
                    "signal": signal,
                    "rank": None,
                    "hit": None,
                    "top": [],
                }
            )
            continue
        matches = run_search(query, args.limit)
        rank = next(
            (i + 1 for i, item in enumerate(matches) if item["pattern_id"] in expected), None
        )
        results.append(
            {
                "expected": sorted(expected),
                "doc": str(doc.relative_to(REPO_ROOT)),
                "query": query,
                "signal": signal,
                "rank": rank,
                "hit": matches[rank - 1]["pattern_id"] if rank else None,
                "top": [
                    {"pattern_id": item["pattern_id"], "score": item["score"], "status": item["status"]}
                    for item in matches
                ],
            }
        )

    args.out_dir.mkdir(parents=True, exist_ok=True)
    suffix = f"-{args.label}" if args.label else ""
    json_path = args.out_dir / f"retrieval-eval-{args.date}{suffix}.json"
    md_path = args.out_dir / f"retrieval-eval-{args.date}{suffix}.zh-CN.md"
    json_path.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    md_path.write_text(render_report(args.date, results, args.limit), encoding="utf-8")
    print(f"RETRIEVAL_EVAL_OK QUERIES={len(results)} JSON={json_path} REPORT={md_path}")


if __name__ == "__main__":
    main()
