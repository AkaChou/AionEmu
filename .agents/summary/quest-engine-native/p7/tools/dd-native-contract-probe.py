#!/usr/bin/env python3
"""P7 步 1 探针：DD（data_driven_quest）原生 handler 契约逐行复算。

证据面（全部只读、仓内可复算，不读外部根）：
  A. <仓库根>/src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml
     真端 DD 表（仓内 vendor 副本；注释块不是行）
  B. <仓库根>/src/main/resources/.../retail/retail-xml-retention.tsv   owner 台账
  C. <仓库根>/src/test/resources/quest/retail-data-driven-client-absent.tsv
     客户端三表皆无的孤行 + 真端表注释禁用行（一次性客户端证据快照，P7 前置裁定）

产出：
  1. 切换集 = 真端 DD 活行 ∧ owner RETAIL_TABLE（客户端可渲染 ∧ 玩家可见）
  2. 接取轴直方图（真端 `category_acquire_`）
  3. 进度形直方图（`category_progress_` 步序列）与每类步数
  4. 步列词汇表（每类实际出现的 `valueN_progress_` 列号，含出现次数）= step 2 的动作/效果实现清单
  5. 6 位布局不变量（步号 <= 63、每步子计数 <= 4 组、计数 <= 63，80817 为裁定例外）
  6. 逐行矩阵 TSV + 规范形 SHA-256（门禁冻结摘要）

用法：
  python3 dd-native-contract-probe.py [--out <逐行矩阵 TSV>] [--summary-json <path>]
"""

import argparse
import collections
import hashlib
import json
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve()

# 真端 8 类 progress handler（p7-prereqs/dd-dispatcher-and-handlers.md §2）。
RETAIL_PROGRESS_CATEGORIES = (
    "hunt", "collectitem", "pvp", "talk", "enterarea", "itemplay", "enterworld", "talkfobj")
# 真端接取 kind（QuestProgressExtraInfo_Talk.cpp：ItemPlay=3 / Talk=4 / EnterWorld=7 / LevelUp=8 /
# LevelUpLogIn=10）+ DD 表实际出现的 EnterArea（区域任务结束）与 none（链式自动接取）。
RETAIL_ACQUIRE_KINDS = (
    "talk", "itemplay", "levelup", "enterworld", "leveluplogin", "enterarea", "none")
# 6 位布局：bit0-5 = 步号，之后每 6 位一个子计数（真端 FUN_180c46020 算术）。
SIX_BIT_STEP_MASK = 0x3F
# 裁定例外：80817（客户端两表 + 真端 quest.xml 都有行）计数 100 > 63，按真端原样复刻（含第 64 杀回绕）。
ADJUDICATED_OVER_SIX_BIT = {80817}


def find_repo(start: pathlib.Path) -> pathlib.Path:
    for candidate in [start, *start.parents]:
        if (candidate / "pom.xml").is_file():
            return candidate
    raise SystemExit("cannot locate <仓库根> (pom.xml) from " + str(start))


REPO = find_repo(HERE)
DD_TABLE = REPO / "src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml"
RETENTION = REPO / "src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv"
ABSENT_FIXTURE = REPO / "src/test/resources/quest/retail-data-driven-client-absent.tsv"

BLOCK = re.compile(r"<quest_data_driven>(.*?)</quest_data_driven>", re.S)
DATA = re.compile(r"<data>(.*?)</data>", re.S)
COMMENT = re.compile(r"<!--.*?-->", re.S)
DTD = re.compile(r"<!DOCTYPE.*?\]>", re.S)
CATEGORY = re.compile(r"<category_progress_>\s*([^<]*?)\s*</category_progress_>")
COLUMN = re.compile(r"<(value\d+_progress_)>\s*([^<]*?)\s*</\1>")


def strip_prologue(text: str) -> str:
    match = DTD.search(text)
    if match:
        text = text[:match.start()] + text[match.end():]
    return COMMENT.sub("", text)


def field(body: str, name: str):
    match = re.search(r"<%s>\s*([^<]*?)\s*</%s>" % (name, name), body, re.S)
    return match.group(1).strip() if match else None


def dd_rows() -> dict:
    """真端 DD 活行（注释块已剔）。 / Live retail DD rows (comment blocks are not rows)."""
    text = strip_prologue(DD_TABLE.read_text("utf-8"))
    rows = {}
    for body in BLOCK.findall(text):
        quest_id = int(field(body, "id"))
        steps = []
        for data in DATA.findall(body):
            category = (CATEGORY.search(data).group(1) or "").strip().lower()
            columns = {}
            for name, value in COLUMN.findall(data):
                if value.strip():
                    columns[int(re.findall(r"\d+", name)[0])] = value.strip()
            steps.append((category, columns))
        rows[quest_id] = dict(
            quest_id=quest_id,
            acquire=(field(body, "category_acquire_") or "").strip().lower(),
            acquire_param=field(body, "value0_acquire_"),
            reward_npc=field(body, "reward_npc_name"),
            steps=steps,
        )
    return rows


def retention_owners() -> dict:
    owners = {}
    for line in RETENTION.read_text("utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        cells = line.split("\t")
        owners.setdefault(int(cells[0]), cells[1])
    return owners


def frozen_buckets() -> dict:
    buckets = collections.defaultdict(set)
    for line in ABSENT_FIXTURE.read_text("utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        cells = line.split("\t")
        if cells[0] == "quest_id":
            continue
        buckets[cells[6]].add(int(cells[0]))
    return buckets


def counter_groups(steps) -> list:
    """每步的子计数组数（hunt/collectitem/pvp = 分号段数，其余类别 = 1）。"""
    out = []
    for category, columns in steps:
        value = columns.get(0, "")
        if category in ("hunt", "collectitem", "pvp"):
            segments = [segment.strip() for segment in value.split(";") if segment.strip()]
            out.append(max(1, len(segments)))
        else:
            out.append(1)
    return out


def targets(steps) -> list:
    """每步的目标计数（hunt/collectitem 取段尾计数，pvp 取段值）。"""
    out = []
    for category, columns in steps:
        value = columns.get(0, "")
        if category not in ("hunt", "collectitem", "pvp"):
            out.append([1])
            continue
        numbers = []
        for segment in value.split(";"):
            segment = segment.strip()
            if not segment:
                continue
            if category == "pvp":
                numbers.append(int(segment) if segment.isdigit() else None)
            else:
                split = segment.rfind(" ")
                tail = segment[split + 1:].strip() if split > 0 else ""
                numbers.append(int(tail) if tail.isdigit() else None)
        out.append(numbers)
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=str(HERE.parent.parent / "dd-native-shape-matrix.tsv"))
    parser.add_argument("--summary-json")
    args = parser.parse_args()

    rows = dd_rows()
    owners = retention_owners()
    buckets = frozen_buckets()
    absent = buckets["CLIENT_ABSENT_LIVE"]
    commented = buckets["COMMENTED_OUT"]

    switch = sorted(quest for quest in rows if owners.get(quest) == "RETAIL_TABLE")
    assert not (set(switch) & absent), "切换集不得含客户端不可渲染的孤行"
    assert not (set(switch) & commented), "切换集不得含注释禁用行"

    acquire_histogram = collections.Counter(rows[quest]["acquire"] for quest in switch)
    steps_histogram = collections.Counter(len(rows[quest]["steps"]) for quest in switch)
    category_steps = collections.Counter()
    shape_histogram = collections.Counter()
    column_histogram = collections.defaultdict(collections.Counter)
    wide_steps = []
    over_six_bit = set()
    for quest_id in switch:
        row = rows[quest_id]
        shape_histogram[tuple(category for category, _ in row["steps"])] += 1
        for index, (category, columns) in enumerate(row["steps"]):
            assert category in RETAIL_PROGRESS_CATEGORIES, (quest_id, category)
            assert 0 in columns, "步必须声明 value0_progress_：%s#%d" % (quest_id, index)
            category_steps[category] += 1
            for number in columns:
                column_histogram[category][number] += 1
        for index, groups in enumerate(counter_groups(row["steps"])):
            if groups > 4:
                wide_steps.append((quest_id, index, groups))
        for targets_of_step in targets(row["steps"]):
            for target in targets_of_step:
                if target is not None and target > SIX_BIT_STEP_MASK:
                    over_six_bit.add(quest_id)

    canonical = []
    for quest_id in switch:
        row = rows[quest_id]
        columns = sorted({column for _, cols in row["steps"] for column in cols})
        canonical.append("%d\t%s\t%s\t%s\t%s" % (
            quest_id,
            row["acquire"],
            ";".join(category for category, _ in row["steps"]) or "-",
            ",".join(str(column) for column in columns),
            row["reward_npc"] or "-"))
    digest = hashlib.sha256(("\n".join(canonical) + "\n").encode("utf-8")).hexdigest()

    out = pathlib.Path(args.out)
    with out.open("w", encoding="utf-8") as handle:
        handle.write("# P7 步 1：DD 原生 handler 契约逐行矩阵（真端表 + owner 台账 + 客户端孤行快照复算）\n")
        handle.write("quest_id\tacquire\tstep_categories\tvalue_columns\treward_npc\n")
        handle.write("\n".join(canonical))
        handle.write("\n")

    summary = {
        "dd_live_rows": len(rows),
        "switch_rows": len(switch),
        "buckets": {
            "client_absent_live": len(absent),
            "commented_out": len(commented),
            "retail_table": len(switch),
            "xml_retention": sum(1 for quest in rows if owners.get(quest) == "XML_RETENTION"),
        },
        "acquire_histogram": dict(sorted(acquire_histogram.items())),
        "steps_per_row_histogram": dict(sorted(steps_histogram.items())),
        "category_steps": dict(sorted(category_steps.items())),
        "category_rows": dict(sorted(collections.Counter(
            category
            for quest in switch
            for category in {category for category, _ in rows[quest]["steps"]}
        ).items())),
        "distinct_shapes": len(shape_histogram),
        "column_histogram": {category: dict(sorted(histogram.items()))
                             for category, histogram in sorted(column_histogram.items())},
        "steps_with_more_than_four_counters": wide_steps,
        "counts_over_six_bit": sorted(over_six_bit),
        "adjudicated_over_six_bit": sorted(ADJUDICATED_OVER_SIX_BIT),
        "canonical_sha256": digest,
        "matrix_out": str(out.relative_to(REPO)) if str(out).startswith(str(REPO)) else str(out),
    }
    if args.summary_json:
        pathlib.Path(args.summary_json).write_text(
            json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
