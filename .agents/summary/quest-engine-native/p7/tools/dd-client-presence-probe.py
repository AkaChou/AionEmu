#!/usr/bin/env python3
"""§10.3-#5 前置裁定探针：DD 行的「真端表 vs 客户端三表」存在性 + 裁定分桶。

证据面（全部只读）：
  A. <真端根>/Map/XML/data_driven_quest.xml      真端 DD 表（行 = 任务 id / dev_name / category_acquire / Hunt 规格）
  B. <真端根>/Map/XML/quest.xml                  真端任务元数据
  C. <真端根>/Map/XML/challenge_task.xml         真端挑战任务表
  D. <客户端解包根>/quest.xml / data_driven_quest.xml / challenge_task.xml   客户端三表（玩家可渲染性判据）
  E. <仓库根>/src/main/resources/.../retail-xml-retention.tsv  owner 台账 + quests/*.xml 存在性

裁定（P7 前置，与计划 §10.3-#5 对齐）：
  CLIENT_PRESENT           客户端任一表有该 id ⇒ 玩家可渲染 ⇒ P7 必须实现（不得冻结）
  CLIENT_ABSENT            客户端三表皆无 ⇒ 类级冻结（不路由/不注册），理由 = 同版客户端无行

用法：
  python3 dd-client-presence-probe.py [--out <全量 TSV>] [--adjudicated-out <冻结 TSV>]
"""
import argparse
import pathlib
import re
import sys
from collections import Counter

HERE = pathlib.Path(__file__).resolve()


def find_repo(start: pathlib.Path) -> pathlib.Path:
    for candidate in [start, *start.parents]:
        if (candidate / "pom.xml").is_file():
            return candidate
    raise SystemExit("cannot locate <仓库根> (pom.xml) from " + str(start))


def find_host_dir(relative: str) -> pathlib.Path:
    """按 ENVIRONMENT.md 的同宿主约定解析外部根；支持 <workspace> 与本机 HOME 两种布局。"""
    candidates = [REPO.parent / relative, REPO.parent.parent / relative, pathlib.Path.home() / relative]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    raise SystemExit("cannot locate external root '" + relative + "' (tried: "
                     + ", ".join(str(c) for c in candidates) + ")")


REPO = find_repo(HERE)
RETAIL = find_host_dir("58Server")
CLIENT = find_host_dir("PycharmProjects/unpak/Quest_unpacked")
RETAIL_XML = RETAIL / "Map/XML"
RETENTION = REPO / "src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv"
DEFS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"

BLOCK = re.compile(r"<quest_data_driven>(.*?)</quest_data_driven>", re.S)
ID = re.compile(r"<id>\s*(\d+)\s*</id>")
FIELD = re.compile(r"<([a-z_0-9]+)>\s*([^<]*?)\s*</\1>")
QUEST_ID = re.compile(r"<id>\s*(\d+)\s*</id>")


def read_text(path: pathlib.Path) -> str:
    raw = path.read_bytes()
    text = raw.decode("utf-16") if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else raw.decode("utf-8", errors="replace")
    dtd = re.search(r"<!DOCTYPE.*?\]>", text, flags=re.S)
    if dtd:
        text = text[:dtd.start()] + text[dtd.end():]
    return text


DD_TABLE = REPO / "src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml"


COMMENT = re.compile(r"<!--.*?-->", re.S)


def dd_rows() -> dict:
    """真端 DD 表**活行**（仓内 vendor 副本，与 <真端根>/Map/XML/data_driven_quest.xml 同源）。

    真端表里有 32 个 `<quest_data_driven>` 块被 XML 注释掉（18 个 id 只存在于注释里），
    与生产装载器 `RetailDataDrivenTable` 的口径一致：注释行不是行。
    """
    text = COMMENT.sub("", read_text(DD_TABLE))
    rows = {}
    for body in BLOCK.findall(text):
        match = ID.search(body)
        if not match:
            continue
        fields = {}
        for key, value in FIELD.findall(body):
            fields.setdefault(key, []).append(value.strip())
        rows.setdefault(int(match.group(1)), fields)
    return rows


def commented_out_rows() -> dict:
    """只存在于 XML 注释里的 DD 行（真端自身不装载；含其字段，供证据面冻结）。"""
    raw = read_text(DD_TABLE)
    live = dd_rows()
    rows = {}
    for span in COMMENT.finditer(raw):
        for body in BLOCK.findall(span.group(0)):
            match = ID.search(body)
            if not match:
                continue
            quest_id = int(match.group(1))
            if quest_id in live:
                continue
            fields = {}
            for key, value in FIELD.findall(body):
                fields.setdefault(key, []).append(value.strip())
            rows.setdefault(quest_id, fields)
    return rows


def quest_ids(path: pathlib.Path) -> set:
    return {int(m) for m in QUEST_ID.findall(read_text(path))} if path.is_file() else set()


def retention() -> dict:
    owners = {}
    for line in RETENTION.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        cells = line.split("\t", -1)
        if cells[0].isdigit():
            owners[int(cells[0])] = (cells[1], cells[3])
    return owners


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=str(REPO / ".agents/summary/quest-engine-native/p7/dd-client-presence.tsv"))
    parser.add_argument("--adjudicated-out",
                        default=str(REPO / "src/test/resources/quest/retail-data-driven-client-absent.tsv"))
    args = parser.parse_args()

    dd = dd_rows()
    retail_quest = quest_ids(RETAIL_XML / "quest.xml")
    retail_challenge = quest_ids(RETAIL_XML / "challenge_task.xml")
    client_quest = quest_ids(CLIENT / "quest.xml")
    client_dd = quest_ids(CLIENT / "data_driven_quest.xml")
    client_challenge = quest_ids(CLIENT / "challenge_task.xml")
    owners = retention()

    lines = ["# §10.3-#5 逐行证据：真端 DD 表 × 真端 quest/challenge × 客户端三表 × owner 台账",
             "\t".join(["quest_id", "dev_name", "acquire_category", "hunt_first", "retail_quest_xml",
                        "retail_challenge", "client_quest_xml", "client_dd_table", "client_challenge",
                        "retention_owner", "xml_in_repo", "verdict"])]
    absent = ["# §10.3-#5 裁定冻结：客户端三表皆无的 DD 活行（CLIENT_ABSENT_LIVE ⇒ P7 不得路由/注册）"
              " + 真端表内注释禁用的行（COMMENTED_OUT ⇒ 装载器不含；逐行证据见 p7/dd-client-presence.tsv）",
              "\t".join(["quest_id", "dev_name", "acquire_category", "hunt_first", "retention_owner",
                         "xml_in_repo", "verdict"])]
    dead_rows = commented_out_rows()
    verdicts = Counter()
    for quest_id in sorted(dd):
        fields = dd[quest_id]
        dev_name = (fields.get("dev_name") or ["-"])[0]
        acquire = (fields.get("category_acquire_") or ["-"])[0]
        hunt = (fields.get("value0_progress_") or ["-"])[0]
        in_client = quest_id in client_quest or quest_id in client_dd or quest_id in client_challenge
        verdict = "CLIENT_PRESENT" if in_client else "CLIENT_ABSENT_LIVE"
        owner = owners.get(quest_id, ("ABSENT", ""))[0]
        xml_in_repo = (DEFS / f"{quest_id}.xml").is_file()
        verdicts[verdict] += 1
        lines.append("\t".join([
            str(quest_id), dev_name, acquire, hunt,
            str(quest_id in retail_quest), str(quest_id in retail_challenge),
            str(quest_id in client_quest), str(quest_id in client_dd), str(quest_id in client_challenge),
            owner, str(xml_in_repo), verdict,
        ]))
        if verdict == "CLIENT_ABSENT_LIVE":
            absent.append("\t".join([str(quest_id), dev_name, acquire, hunt, owner, str(xml_in_repo), verdict]))

    for quest_id in sorted(dead_rows):
        fields = dead_rows[quest_id]
        dev_name = (fields.get("dev_name") or ["-"])[0]
        acquire = (fields.get("category_acquire_") or ["-"])[0]
        hunt = (fields.get("value0_progress_") or ["-"])[0]
        owner = owners.get(quest_id, ("ABSENT", ""))[0]
        xml_in_repo = (DEFS / f"{quest_id}.xml").is_file()
        absent.append("\t".join([str(quest_id), dev_name, acquire, hunt, owner, str(xml_in_repo),
                                  "COMMENTED_OUT"]))

    pathlib.Path(args.out).write_text("\n".join(lines) + "\n", encoding="utf-8")
    pathlib.Path(args.adjudicated_out).write_text("\n".join(absent) + "\n", encoding="utf-8")

    print("DD_ROWS", len(dd))
    print("VERDICTS", dict(verdicts))
    print("CLIENT_ABSENT_DD_TABLE", sum(1 for i in dd if i not in client_quest and i not in client_dd and i not in client_challenge))
    print("CLIENT_ABSENT_WITH_RETAIL_QUEST_XML",
          sum(1 for i in dd if i not in client_quest and i not in client_dd and i not in client_challenge and i in retail_quest))
    print("CLIENT_ABSENT_RETIRED",
          sum(1 for i in dd if i not in client_quest and i not in client_dd and i not in client_challenge
              and owners.get(i, ("ABSENT", ""))[0] == "RETAIL_TABLE"))
    buckets = Counter()
    for i in dd:
        if i in client_quest or i in client_dd or i in client_challenge:
            continue
        name = (dd[i].get("dev_name") or [""])[0]
        for token in ("[주간]", "[이벤트]", "_challengetask_", "fortress", "world_", "test_"):
            if token in name:
                buckets[token] += 1
                break
        else:
            buckets["(other)"] += 1
    print("ABSENT_DEV_NAME_BUCKETS", dict(buckets))
    dead = set(dead_rows)
    print("COMMENTED_OUT_IDS", len(dead), "（真端表注释禁用；生产装载器不含）")
    print("COMMENTED_OUT_SAMPLE", sorted(dead)[:10])
    print("FIXTURE_ROWS", len(absent) - 2, "= CLIENT_ABSENT_LIVE",
          sum(1 for line in absent[2:] if line.endswith("CLIENT_ABSENT_LIVE")),
          "+ COMMENTED_OUT", sum(1 for line in absent[2:] if line.endswith("COMMENTED_OUT")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
