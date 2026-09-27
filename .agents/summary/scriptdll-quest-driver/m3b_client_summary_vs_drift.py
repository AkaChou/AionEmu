#!/usr/bin/env python3
"""交叉表：真端合成 vs 历史 XML 的漂移分类 × 客户端 QUEST_Q<id>.html 的 quest_summary 行数。

用途（M3-b 口径证据）：回答"XML 的 REWARD 节点 var0=1 到底是任务书第几行"。
`<steps>` 里每个 `<step>` 是客户端任务书的一行；`REWARD` 态的 var0 是"当前进行到第几行"的投影。
"""
import collections
import pathlib
import re
import sys

DIALOGS = pathlib.Path("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs")
REGISTRY = pathlib.Path("src/test/resources/quest/retail-simple-talk-drift.tsv")

STEP_RE = re.compile(r"<step>")
SUMMARY_RE = re.compile(
    r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', re.DOTALL)


def summary_steps(quest_id):
    path = DIALOGS / f"QUEST_Q{quest_id}.html"
    if not path.exists():
        return None
    text = path.read_text(encoding="utf-8", errors="replace")
    match = SUMMARY_RE.search(text)
    if match is None:
        return "no-summary-page"
    return len(STEP_RE.findall(match.group(1)))


def main():
    rows = []
    for line in REGISTRY.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        quest_id, classification = line.split("\t")
        rows.append((int(quest_id), classification))
    cross = collections.Counter()
    examples = collections.defaultdict(list)
    for quest_id, classification in rows:
        steps = summary_steps(quest_id)
        bucket = "missing-html" if steps is None else str(steps)
        cross[(classification, bucket)] += 1
        if len(examples[(classification, bucket)]) < 4:
            examples[(classification, bucket)].append(quest_id)
    for (classification, bucket), count in sorted(cross.items()):
        print(f"{classification:38s} steps={bucket:12s} {count:5d}  e.g. {examples[(classification, bucket)]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
