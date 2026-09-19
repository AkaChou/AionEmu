#!/usr/bin/env python3
"""Hold-out retrieval check for hand-written symptom queries.

Runs a TSV of `<expected Pattern ID(s)>\t<user-voice query>` against two Pattern sets:

- baseline: the current cards with every `keywords:` line stripped (previous behaviour);
- current: the cards as they are in the working tree.

The baseline copy is built in a temporary directory outside the repository, so no card is touched.

Run:
    python3 -B .agents/summary/ai-kb-index/holdout_eval.py
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
SEARCH_TOOL = REPO_ROOT / ".agents/memory-bank/search_memory_bank.py"
KEYWORDS_LINE = re.compile(r"^keywords:.*\n", re.MULTILINE)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Hold-out retrieval check.")
    parser.add_argument(
        "--cases",
        type=Path,
        default=Path(__file__).resolve().parent / "holdout-cases-2026-09-19.tsv",
    )
    parser.add_argument("--limit", type=int, default=5, help="ranking depth, default 5")
    parser.add_argument("--date", default="2026-09-19")
    parser.add_argument("--label", default="holdout")
    parser.add_argument("--out-dir", type=Path, default=Path(__file__).resolve().parent)
    return parser.parse_args()


def load_cases(path: Path) -> list[tuple[list[str], str]]:
    cases = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        expected, query = line.split("\t", 1)
        cases.append(([item.strip() for item in expected.split(",")], query.strip()))
    return cases


def build_baseline_root() -> tuple[Path, tempfile.TemporaryDirectory]:
    temp = tempfile.TemporaryDirectory(prefix="ai-kb-baseline-")
    source = REPO_ROOT / ".agents/memory-bank/patterns"
    target = Path(temp.name) / ".agents/memory-bank/patterns"
    target.mkdir(parents=True)
    for card in sorted(source.glob("*.md")):
        text = card.read_text(encoding="utf-8")
        (target / card.name).write_text(KEYWORDS_LINE.sub("", text), encoding="utf-8")
    return Path(temp.name), temp


def run_search(root: Path, query: str, limit: int) -> list[dict]:
    proc = subprocess.run(
        [
            sys.executable,
            "-B",
            str(SEARCH_TOOL),
            query,
            "--json",
            "--limit",
            str(limit),
            "--root",
            str(root),
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"search failed ({proc.returncode}): {proc.stderr.strip()}")
    return json.loads(proc.stdout)["matches"]


def evaluate(root: Path, cases: list[tuple[list[str], str]], limit: int) -> list[dict]:
    results = []
    for expected, query in cases:
        matches = run_search(root, query, limit)
        rank = next(
            (index + 1 for index, item in enumerate(matches) if item["pattern_id"] in expected), None
        )
        results.append(
            {
                "expected": expected,
                "query": query,
                "rank": rank,
                "hit": matches[rank - 1]["pattern_id"] if rank else None,
                "top": [
                    {"pattern_id": item["pattern_id"], "score": item["score"]} for item in matches
                ],
            }
        )
    return results


def metrics(results: list[dict]) -> dict[str, float]:
    total = len(results)
    return {
        "total": total,
        "top1": sum(1 for item in results if item["rank"] == 1),
        "top3": sum(1 for item in results if item["rank"] and item["rank"] <= 3),
        "top5": sum(1 for item in results if item["rank"]),
        "mrr": round(sum(1.0 / item["rank"] for item in results if item["rank"]) / total, 4)
        if total
        else 0.0,
    }


def render_report(date: str, before: list[dict], after: list[dict]) -> str:
    before_metrics, after_metrics = metrics(before), metrics(after)
    after_by_query = {item["query"]: item for item in after}
    lines = [
        f"# 手写留出集检索报告（{date}）",
        "",
        "## 方法",
        "",
        "- 查询来源：人工按用户/Z 端口吻改写的留出集（不复制卡片 `symptom` 原文），见 `holdout-cases-2026-09-19.tsv`。",
        "- 对照组：把工作树卡片中所有 `keywords:` 行剥离后复制到临时目录，用同一 `search_memory_bank.py` 重跑（即改造前行为）。",
        "- 局限：查询与 keywords 由同一作者编写，仍有人为偏向；本表只用于确认「别名字段确实被检索到且没有把别的卡片挤掉」。",
        "",
        "## 结果",
        "",
        "| 指标 | 改造前 | 改造后 |",
        "|---|---|---|",
        f"| 查询数 | {before_metrics['total']} | {after_metrics['total']} |",
        f"| Top-1 | {before_metrics['top1']} | {after_metrics['top1']} |",
        f"| Top-3 | {before_metrics['top3']} | {after_metrics['top3']} |",
        f"| Top-5 | {before_metrics['top5']} | {after_metrics['top5']} |",
        f"| MRR | {before_metrics['mrr']} | {after_metrics['mrr']} |",
        "",
        "## 逐条对比",
        "",
        "| 查询 | 期望 | 改造前排名 | 改造后排名 | 改造后命中 |",
        "|---|---|---|---|---|",
    ]
    for item in before:
        after_item = after_by_query[item["query"]]
        lines.append(
            f"| {item['query'][:44]}… | `{', '.join(item['expected'])}` | "
            f"{item['rank'] or 'miss'} | {after_item['rank'] or 'miss'} | "
            f"`{after_item['hit'] or '-'}` |"
        )

    regressed = [
        item
        for item in before
        if item["rank"] and (not after_by_query[item["query"]]["rank"]
                             or after_by_query[item["query"]]["rank"] > item["rank"])
    ]
    lines += ["", "## 回归检查", ""]
    if regressed:
        for item in regressed:
            after_rank = after_by_query[item["query"]]["rank"] or "miss"
            lines.append(f"- 「{item['query'][:60]}…」排名从 {item['rank']} 掉到 {after_rank}")
    else:
        lines.append("- 无：改造后没有任何查询排名下降。")
    return "\n".join(lines) + "\n"


def main() -> None:
    args = parse_args()
    cases = load_cases(args.cases)
    baseline_root, temp = build_baseline_root()
    try:
        before = evaluate(baseline_root, cases, args.limit)
    finally:
        temp.cleanup()
    after = evaluate(REPO_ROOT, cases, args.limit)

    args.out_dir.mkdir(parents=True, exist_ok=True)
    json_path = args.out_dir / f"retrieval-eval-{args.date}-{args.label}.json"
    md_path = args.out_dir / f"retrieval-eval-{args.date}-{args.label}.zh-CN.md"
    json_path.write_text(
        json.dumps({"before": before, "after": after}, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    md_path.write_text(render_report(args.date, before, after), encoding="utf-8")
    print(f"HOLDOUT_EVAL_OK CASES={len(cases)} JSON={json_path} REPORT={md_path}")


if __name__ == "__main__":
    main()
