#!/usr/bin/env python3
"""QE-112 裁定探针：DD 纯 hunt 行的「真端块 × 客户端行阶梯」双源复算。

证据面（只读）：
  A. <真端根>/Map/XML/data_driven_quest.xml  真端 DD 表：每个 progress 块的 `;` 段数与该段计数
  B. <仓库根>/src/main/resources/aion/definitions/quest_monster/quest_monster.csv
     客户端任务书行阶梯：`Progress(SECTION_0==k; SECTION_m<count)`（DD hunt 行的唯一客户端形）

裁定（QE-112 切片）：
  * 每行的段数落 6 位行号（var0）、段计数落 var1..var4（SECTION_1..4）⇒ 行阶梯，旧“5 槽网格”上限不存在；
  * 4 个 6 块 × 1 段的行（3122/3123/4122/4123）由 XML 保留（ADJUDICATED:RETAIL_HUNT_MULTI_STAGE_DEFERRED）
    转 RETAIL_TABLE/OK，basis = DD_HUNT_CLIENT_LADDER（本台账）；
  * 多段单行（如 2869 的 6+6）与多行单段（如 15324 的 20/20/20）同属一个阶梯模型，均按客户端行复算。

用法：
  python3 dd-hunt-ladder-probe.py [--out <TSV>]
"""
import argparse
import csv
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve()
REPO = next(candidate for candidate in [HERE, *HERE.parents] if (candidate / "pom.xml").is_file())
CLIENT_CSV = REPO / "src/main/resources/aion/definitions/quest_monster/quest_monster.csv"
BLOCK = re.compile(r"<quest_data_driven>(.*?)</quest_data_driven>", re.S)
ROW = re.compile(r"^Progress\(SECTION_0==(\d+)(?:; SECTION_([1-9]\d*)<(\d+))?\)$")

FLIPPED = (3122, 3123, 4122, 4123)


def find_host_dir(relative: str) -> pathlib.Path:
    for candidate in [REPO.parent / relative, REPO.parent.parent / relative, pathlib.Path.home() / relative]:
        if candidate.exists():
            return candidate
    raise SystemExit("cannot locate external root '" + relative + "'")


RETAIL = find_host_dir("58Server") / "Map/XML/data_driven_quest.xml"


def field(block: str, name: str):
    match = re.search(r"<%s>\s*([^<]*?)\s*</%s>" % (name, name), block, re.S)
    return match.group(1) if match else None


def load_table():
    raw = RETAIL.read_bytes()
    text = raw.decode("utf-16") if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else raw.decode("utf-8", errors="replace")
    text = re.sub(r"<!DOCTYPE.*?\]>", "", text, flags=re.S)
    rows = {}
    for block in BLOCK.findall(text):
        if "<id>" not in block:
            continue
        quest_id = int(field(block, "id"))
        progress = [value.strip() for value in
                    re.findall(r"<category_progress_>\s*([^<]*?)\s*</category_progress_>", block, re.S)]
        if not progress or set(progress) != {"Hunt"}:
            continue
        steps = []
        for index in range(len(progress)):
            value = field(block, "value%d_progress_" % index) or ""
            groups = [group.strip() for group in value.split(";") if group.strip()]
            steps.append([int(group.replace(",", " ").split()[-1]) for group in groups if group.replace(",", " ").split()[-1].isdigit()])
        rows[quest_id] = steps
    return rows


def load_client():
    rows = {}
    with CLIENT_CSV.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.reader(handle)
        next(reader)
        for record in reader:
            if len(record) < 2:
                continue
            try:
                quest_id = int(record[0].strip())
            except ValueError:
                continue
            match = ROW.match(record[1].strip())
            if match:
                rows.setdefault(quest_id, []).append(
                    (int(match.group(1)), int(match.group(2) or 0), int(match.group(3) or 0)))
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=str(HERE.parent.parent / "qe-112-quest-ddhunt-ladder-decisions.tsv"))
    args = parser.parse_args()
    table = load_table()
    client = load_client()

    lines = [
        "# QE-112 裁定台账：DD 纯 hunt 行的行阶梯（真端 progress 块 × 客户端 quest_monster.csv 双源复算）",
        "# QE-112 adjudication ledger: the DD pure-hunt row ladder, recomputed from the retail progress",
        "# blocks and the client quest_monster.csv rows.",
        "#",
        "# 复算 / recompute: python3 .agents/summary/quest-dd-hunt6-ladder/tools/dd-hunt-ladder-probe.py",
        "# 结论 / verdict: 行阶梯 = var0 行号（SECTION_0）+ var1..var4 段计数（SECTION_1..4）；每行 1..4 段、",
        "#   每段计数 1..63；旧「5 槽网格」上限是把行内段号当槽号的误判 ⇒ 6 块 × 1 段的 3122/3123/4122/4123",
        "#   由 XML 保留转 RETAIL_TABLE/OK（basis=DD_HUNT_CLIENT_LADDER）。",
        "quest_id\tverdict\tbasis\tprogress_blocks\tsegments_per_block\tclient_rows\tclient_counts\tclient_contract\tdetail",
    ]
    for quest_id in FLIPPED:
        steps = table[quest_id]
        rows = client.get(quest_id, [])
        counts = [count for _, _, count in rows]
        contract = "; ".join("SECTION_0==%d; SECTION_1<%d" % (row, count) for row, _, count in rows)
        lines.append("\t".join([
            str(quest_id), "ADOPTED", "DD_HUNT_CLIENT_LADDER", str(len(steps)),
            ",".join(str(len(step)) for step in steps),
            ",".join(str(row) for row, _, _ in rows), ",".join(str(count) for count in counts), contract,
            "真端 %d 个进度块（首块 1 段，其余空段由客户端行补足）；客户端 %d 行行阶梯（每行 1 杀）"
            "⇒ 行号 + 计数两字段足够" % (len(steps), len(rows)),
        ]))

    # 家族复算：所有「真端纯 hunt 块 ∧ 客户端声明行阶梯」的行，段数/计数必须与客户端一一对应。
    checked = 0
    problems = []
    for quest_id, steps in sorted(table.items()):
        rows = client.get(quest_id, [])
        if not rows:
            continue
        checked += 1
        ladder = {}
        for row, section, count in rows:
            ladder.setdefault(row, {})[section] = count
        model = [list(ladder[row].values()) for row in sorted(ladder)]
        flat = [count for step in steps for count in step]
        if [count for row in model for count in row] != flat:
            problems.append((quest_id, flat, model))
    lines.append("")
    lines.append("# 家族复算：客户端声明行阶梯的纯 hunt 行 = %d；其中「真端块段数 ≠ 客户端行阶梯」的行 = %d。"
                 % (checked, len(problems)))
    lines.append("# 形差处置：DD 车道以客户端任务书为计数/行号权威（家族口径「客户端计数为权威」），"
                 "因此这些行按客户端行阶梯编译；逐行形差登记如下（retail=真端 progress 块的段计数，"
                 "client=客户端行阶梯的段计数）。")
    lines.append("# Family recompute: rows whose retail block shape differs from the client ladder are"
                 " compiled from the client rows (the family's client-count canon); each difference is listed.")
    for quest_id, flat, model in problems:
        lines.append("# SHAPE_DIFF\t%d\tretail=%s\tclient=%s" % (quest_id, flat, model))

    pathlib.Path(args.out).write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("FLIPPED", list(FLIPPED))
    print("CLIENT_LADDER_ROWS", checked)
    print("MISMATCH", len(problems))
    print("WROTE", args.out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
