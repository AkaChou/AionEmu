#!/usr/bin/env python3
"""重建 `docs/QUEST_CATALOG.zh-CN.md`：全任务目录（id / 中英文名 / 等级 / 上限 / 道具物品 id / 限制 / 前置 / 接取 / 定义）。

数据来源
- 任务集与名称/限制/前置/接取方式：QuestWiki 索引（`<工作区>/AionEmu-QuestWiki/public/data/quests.index.json`，
  由迁移前的本目录 + 退役定义 XML + Aion 5.8 客户端字符串派生；其中等级已与仓库内真端表
  `quest/retail/quest.xml` 的 `minlevel_permitted` 全量交叉核对，6222/6222 一致）。
- 上限等级：真端表 `maxlevel_permitted`（0 = 无上限；998/999 为真端哨兵，按原值输出）。
- 道具物品 id：真端表物品列（工作/检查/收集/掉落/背包/奖励/可选奖励/职业）经仓库内物品模板
  `items/item/*.xml` 的 `name_desc → id` 索引解析（两通道：原名 → 去 `ITEM_` 前缀重查，与
  `NativeItemSymbols` 同规则）；`%` 前缀 = 随机奖励组，经 `quest/legacy/quest_random_rewards.xml`
  的 `name → id` 解析；未解符号原样输出并计入表头统计。
- 定义文件列：仓库内现行 `definitions/quests/<id>.xml` 存在 ⇒ 链接现行路径；否则（真端表驱动）⇒
  链接历史路径 `quest_definition/quests/<id>.xml`（见 git 历史），链接文字标注「已退役（真端表驱动）」。
- 目标（击杀/采集/收集/交互）列：真端优先——击杀取仓库内零售族表副本
  `quest/retail/Quest_SimpleHunt.xml`（monsterN+countN 组）、`Quest_SimpleSerialHunt.xml` 与
  `data_driven_quest.xml` 的 Hunt 进度（组内名字 `,`/空格分隔、尾数字为组计数），合并零售
  `quest.xml` 的 `drop_monster_*` 掉落怪列；真端无声明时回退客户端契约
  `definitions/quest_monster/quest_monster.csv`（击杀类 sourceType）。采集 = 客户端契约
  `gatherSource`（名字经真端 `<真端根>/Map/XML/Objects.xml` 的 harvest_source 表解析为对象 id）。
  收集 = 族表 `Quest_SimpleCollectItem.xml` 的 objectN + `data_driven_quest.xml` 的 CollectItem 进度。
  交互 = 族表 `Quest_SimpleTalk.xml`/`Quest_SimpleItemPlay.xml`/`Quest_SimpleSerialHunt.xml` 的 talk_npc*
  （中继目标 NPC）+ `data_driven_quest.xml` 的 Talk/TalkFOBJ/EnterArea 进度 + 客户端契约 goodsList/itemUseArea。
  名字→npc_id：仓库内 `npcs/npc_template_*.xml` 的 `name ∪ name_desc`（与 `NativeNpcNameResolver`
  同规则，一名多 id 时全部列出）；未解析输出 `?名字`。真端计数 `×n` 只随组输出；每分类最多
  `TARGET_REF_CAP` 个名字，超出记 `…+k`。

输出行格式（QuestWiki `scripts/sources.ts` 的 `loadCatalog` 兼容性约束）：
`| <id> | <等级> | <英文名> | <中文名> | <限制> | <前置> | <接取> | [<定义>](<路径>) | <上限等级> | <道具物品 id> | <目标> |`
- 定义列必须保持在第 7 格（解析器取 `cells[6]` 提取链接），新增列一律追加在其后；
- 单元格内不得出现裸 `|`（出现时替换为 `/`）。
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

HISTORICAL_PREFIX = "src/main/resources/aion/data/static_data/quest_definition/quests"
LIVE_PREFIX = "src/main/resources/aion/data/static_data/quest/definitions/quests"
ITEM_DIR = "src/main/resources/aion/data/static_data/items/item"
RANDOM_REWARD_FILE = "src/main/resources/aion/data/static_data/quest/legacy/quest_random_rewards.xml"
QUEST_TABLE = "src/main/resources/aion/data/static_data/quest/retail/quest.xml"
RETAIL_XML_DIR = "src/main/resources/aion/data/static_data/quest/retail"
NPC_DIR = "src/main/resources/aion/data/static_data/npcs"
CLIENT_MONSTER_FILE = "src/main/resources/aion/definitions/quest_monster/quest_monster.csv"
LINK_PREFIX = ".."

# 目标（击杀/采集/收集/交互）列：每个分类最多展示的目标名数量（超出记 …+k）——防止个别
# 任务的目标清单（客户端契约可达上百名）撑爆单元格。
TARGET_REF_CAP = 8
# 客户端契约 sourceType → 分类归属（击杀清单 / 交互清单）
CLIENT_KILL_TYPES = ("questItemDropMonster", "simpleQuest", "commonDrop", "dropMonster")
CLIENT_INTERACT_TYPES = ("goodsList", "itemUseArea")

CLASS_LABEL = {
    "fighter_selectable_item": "FIGHTER", "fighter_selectable_reward": "FIGHTER",
    "knight_selectable_item": "KNIGHT", "knight_selectable_reward": "KNIGHT",
    "ranger_selectable_item": "RANGER", "ranger_selectable_reward": "RANGER",
    "assassin_selectable_item": "ASSASSIN", "assassin_selectable_reward": "ASSASSIN",
    "wizard_selectable_item": "WIZARD", "wizard_selectable_reward": "WIZARD",
    "elementalist_selectable_item": "ELEMENTALIST", "elementalist_selectable_reward": "ELEMENTALIST",
    "priest_selectable_item": "PRIEST", "priest_selectable_reward": "PRIEST",
    "chanter_selectable_item": "CHANTER", "chanter_selectable_reward": "CHANTER",
    "gunner_selectable_item": "GUNSLINGER", "gunner_selectable_reward": "GUNSLINGER",
    "bard_selectable_item": "SONGWEAVER", "bard_selectable_reward": "SONGWEAVER",
    "rider_selectable_item": "AETHERTECH", "rider_selectable_reward": "AETHERTECH",
}


def find_repo(start: Path) -> Path:
    for parent in [start, *start.parents]:
        if (parent / "pom.xml").exists():
            return parent
    raise SystemExit("REPO_NOT_FOUND: 未找到含 pom.xml 的仓库根")


def find_wiki_root(repo: Path) -> Path:
    wiki = repo.parent / "AionEmu-QuestWiki"
    if (wiki / "public/data/quests.index.json").exists():
        return wiki
    raise SystemExit(f"WIKI_NOT_FOUND: {wiki}/public/data/quests.index.json 不存在")


def cell(text: object) -> str:
    """单元格清洗：裸 `|` 会破坏列切分，统一替换为 `/`。"""
    return str(text if text is not None else "").strip().replace("|", "/")


def build_item_index(repo: Path) -> dict[str, int]:
    """物品 name_desc → id（重复名保留首个，与 RetailItemNameIndex 同规则）。"""
    tpl = re.compile(r"<item_template\b[^>]*>")
    name = re.compile(r'name_desc="([^"]*)"')
    ident = re.compile(r'\bid="(\d+)"')
    byname: dict[str, int] = {}
    for shard in sorted((repo / ITEM_DIR).glob("*.xml")):
        text = shard.read_text(encoding="utf-8", errors="replace")
        for m in tpl.finditer(text):
            tag = m.group()
            n = name.search(tag)
            i = ident.search(tag)
            if n and i:
                byname.setdefault(n.group(1).lower(), int(i.group(1)))
    return byname


def build_random_groups(repo: Path) -> tuple[dict[str, int], bool]:
    """随机奖励组 `%Name` → id。真端表优先（<真端根>/Map/XML/quest_random_rewards.xml，UTF-16），
    仓库内 legacy 副本补齐；返回 (组表, 真端表是否可用)。"""
    groups: dict[str, int] = {}

    def load(path: Path, encoding: str) -> int:
        try:
            text = path.read_text(encoding=encoding, errors="replace")
        except OSError:
            return 0
        found = 0
        for block in re.findall(r"<quest_random_reward>(.*?)</quest_random_reward>", text, re.S):
            i = re.search(r"<id>(\d+)</id>", block)
            n = re.search(r"<name>(.*?)</name>", block)
            if i and n:
                key = n.group(1).strip()
                if key not in groups:
                    groups[key] = int(i.group(1))
                    found += 1
        return found

    retail_root = repo.parent / "58Server"
    retail_file = retail_root / "Map/XML/quest_random_rewards.xml"
    retail_ok = retail_file.exists()
    if retail_ok:
        load(retail_file, "utf-16")
    load(repo / RANDOM_REWARD_FILE, "utf-8")
    return groups, retail_ok


def build_npc_index(repo: Path) -> dict[str, list[int]]:
    """NPC 名 → id：`name ∪ name_desc` 双属性（与 NativeNpcNameResolver 同规则；
    真端任务表引用 name_desc 式全名，客户端模板 name 为短名）。一名多 id 时保留全部（消歧属裁决项）。"""
    byname: dict[str, set[int]] = {}
    for shard in sorted((repo / NPC_DIR).glob("npc_template_*.xml")):
        text = shard.read_text(encoding="utf-8", errors="replace")
        for m in re.finditer(r"<npc_template\b([^>]*)>", text):
            attrs = dict(re.findall(r'\b(npc_id|name|name_desc)="([^"]*)"', m.group(1)))
            nid = attrs.get("npc_id")
            if not nid:
                continue
            for key in ("name", "name_desc"):
                value = (attrs.get(key) or "").strip().lower()
                if value:
                    byname.setdefault(value, set()).add(int(nid))
    return {name: sorted(ids) for name, ids in byname.items()}


def build_object_index(repo: Path) -> tuple[dict[str, int], bool]:
    """采集源/对象名 → 对象 id：真端 `<真端根>/Map/XML/Objects.xml`（UTF-16）。
    仓库内无等价表（gatherable_templates 只存客户端显示名），根缺失时返回空表并在表头标注。"""
    path = repo.parent / "58Server/Map/XML/Objects.xml"
    if not path.exists():
        return {}, False
    raw = path.read_bytes()
    text = raw.decode("utf-16") if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else raw.decode("utf-8", "replace")
    byname: dict[str, int] = {}
    for block in re.findall(r"<object>(.*?)</object>", text, re.S):
        m_id = re.search(r"<id>(\d+)</id>", block)
        m_nm = re.search(r"<name>([^<]*)</name>", block)
        if m_id and m_nm:
            byname.setdefault(m_nm.group(1).strip().lower(), int(m_id.group(1)))
    return byname, True


def xml_root(text: str):
    """去 DOCTYPE（含实体定义）后解析；实体在正文中的引用保持 XML 内建五实体语义。"""
    import xml.etree.ElementTree as ET
    return ET.fromstring(re.sub(r"<!DOCTYPE.*?\]>", "", text, flags=re.S))


def names_with_count(part: str) -> tuple[list[str], int | None]:
    """拆真端目标片段 `名1, 名2 … [尾数字=组计数]` → (名字表, 计数)。"""
    tokens = [t for t in re.split(r"[,\s]+", part.strip()) if t]
    count = None
    if tokens and tokens[-1].isdigit():
        count = int(tokens[-1])
        tokens = tokens[:-1]
    return tokens, count


def load_family_targets(repo: Path) -> tuple[dict[int, list], dict[int, list], dict[int, list]]:
    """读仓库内零售族表副本 → (击杀, 收集, 交互) 三张 qid → [(名字表, 计数|None)]。

    击杀：SimpleHunt.monsterN（countN 为组计数）、SimpleSerialHunt.monster_*、data_driven Hunt；
    收集：SimpleCollectItem.objectN、data_driven CollectItem；
    交互：SimpleTalk/SimpleItemPlay/SimpleSerialHunt 的 talk_npc*（中继目标 NPC，如 1131 的
    Shugo_LF1a_01=799093）+ data_driven Talk/TalkFOBJ/EnterArea（谈话 NPC / FOBJ 对象 / 进入区域对象）。
    """
    kill: dict[int, list] = {}
    collect: dict[int, list] = {}
    interact: dict[int, list] = {}

    root = xml_root((repo / f"{RETAIL_XML_DIR}/Quest_SimpleHunt.xml").read_text(encoding="utf-8", errors="replace"))
    for row in root:
        qid = row.get("id")
        if not qid or not qid.isdigit():
            continue
        counts = {c.tag: (c.text or "").strip() for c in row if re.fullmatch(r"count\d*", c.tag)}
        for c in row:
            if re.fullmatch(r"monster\d*", c.tag):
                suffix = c.tag[len("monster"):]
                cnt = counts.get(f"count{suffix}")
                names = [t.strip() for t in re.split(r"[;,]", c.text or "") if t.strip()]
                if names:
                    kill.setdefault(int(qid), []).append((names, int(cnt) if cnt and cnt.isdigit() else None))

    for family_file in ("Quest_SimpleTalk.xml", "Quest_SimpleItemPlay.xml"):
        root = xml_root((repo / f"{RETAIL_XML_DIR}/{family_file}").read_text(encoding="utf-8", errors="replace"))
        for row in root:
            qid = row.get("id")
            if not qid or not qid.isdigit():
                continue
            for c in row:
                if re.fullmatch(r"talk_npc\d*", c.tag):
                    names = [t.strip() for t in re.split(r"[;,]", c.text or "") if t.strip()]
                    if names:
                        interact.setdefault(int(qid), []).append((names, None))

    root = xml_root((repo / f"{RETAIL_XML_DIR}/Quest_SimpleSerialHunt.xml").read_text(encoding="utf-8", errors="replace"))
    for row in root:
        qid = row.get("id")
        if not qid or not qid.isdigit():
            continue
        for c in row:
            if re.fullmatch(r"monster.*", c.tag):
                names = [t.strip() for t in re.split(r"[;,]", c.text or "") if t.strip()]
                if names:
                    kill.setdefault(int(qid), []).append((names, None))
            elif re.fullmatch(r"talk_npc\d*", c.tag):
                names = [t.strip() for t in re.split(r"[;,]", c.text or "") if t.strip()]
                if names:
                    interact.setdefault(int(qid), []).append((names, None))

    root = xml_root((repo / f"{RETAIL_XML_DIR}/Quest_SimpleCollectItem.xml").read_text(encoding="utf-8", errors="replace"))
    for row in root:
        qid = row.get("id")
        if not qid or not qid.isdigit():
            continue
        for c in row:
            if re.fullmatch(r"object\d*", c.tag):
                names = [t.strip() for t in re.split(r"[;,]", c.text or "") if t.strip()]
                if names:
                    collect.setdefault(int(qid), []).append((names, None))

    root = xml_root((repo / f"{RETAIL_XML_DIR}/data_driven_quest.xml").read_text(encoding="utf-8", errors="replace"))
    for row in root:
        qid = row.findtext("id")
        progress = row.find("progress_info")
        if not qid or not qid.strip().isdigit() or progress is None:
            continue
        for data in progress.iter("data"):
            category = (data.findtext("category_progress_") or "").strip().lower()
            for part in (data.findtext("value0_progress_") or "").strip().split(";"):
                if not part.strip():
                    continue
                names, count = names_with_count(part)
                if not names:
                    continue
                if category == "hunt":
                    kill.setdefault(int(qid), []).append((names, count))
                elif category == "collectitem":
                    collect.setdefault(int(qid), []).append((names, None))
                elif category in ("talk", "talkfobj", "enterarea"):
                    interact.setdefault(int(qid), []).append((names, None))
    return kill, collect, interact


def load_client_monster(repo: Path) -> dict[int, dict[str, list]]:
    """客户端任务目标契约 quest_monster.csv → qid → sourceType → [(名字表, None)]。

    第 7 字段起为名字列表（逗号分隔，未加引号，故跨字段）；仅作真端无声明时的回退通道。"""
    import csv
    out: dict[int, dict[str, list]] = {}
    path = repo / CLIENT_MONSTER_FILE
    with path.open(encoding="utf-8", errors="replace") as f:
        reader = csv.reader(f)
        next(reader, None)
        for row in reader:
            if len(row) < 7:
                continue
            names = [t for t in re.split(r"[,;\s]+", " ".join(row[6:])) if t]
            if names:
                out.setdefault(int(row[0]), {}).setdefault(row[3].strip(), []).append((names, None))
    return out


def merge_groups(*sources: list) -> list:
    """按名字集合去重（保序、真端在前）合并多路目标组。"""
    merged: list = []
    seen: set = set()
    for groups in sources:
        for names, count in groups:
            key = tuple(sorted(n.lower().rstrip(",") for n in names))
            if key in seen:
                continue
            seen.add(key)
            merged.append((names, count))
    return merged


def format_targets(groups: list, npc_index: dict[str, list[int]], object_index: dict[str, int],
                   unresolved: list[str]) -> str:
    """目标组 → `名(id)/名(id)×n、…`（每组名 '/' 相连、组间 '、'；每分类最多 TARGET_REF_CAP 个名）。"""
    pieces: list[str] = []
    shown = 0
    total = sum(len(names) for names, _ in groups)
    for names, count in groups:
        if shown >= TARGET_REF_CAP:
            break
        kept = names[:TARGET_REF_CAP - shown] if shown + len(names) > TARGET_REF_CAP else names
        shown += len(kept)
        parts = []
        for name in kept:
            key = name.strip().lower().rstrip(",")
            ids = npc_index.get(key)
            if ids is None:
                oid = object_index.get(key)
                ids = [oid] if oid is not None else None
            if ids is None:
                unresolved.append(name.strip())
                parts.append(f"?{name.strip()}")
            elif len(ids) == 1:
                parts.append(f"{name.strip()}({ids[0]})")
            else:
                parts.append(f"{name.strip()}({'/'.join(map(str, ids))})")
        text = "/".join(parts)
        if count is not None:
            text += f"×{count}"
        pieces.append(text)
    if shown < total:
        pieces.append(f"…+{total - shown}")
    return "、".join(pieces)


def parse_items(block: dict[str, list[str]], byname: dict[str, int], groups: dict[str, int],
                unresolved: list[str]):
    """把真端表物品列解析为分组 id 串。返回 [(label, [ 'id' | 'id×n' | '?name', ... ])]。"""
    def resolve(raw: str) -> tuple[str, str] | None:
        """返回 (类别, 文本)：类别 ∈ {item, group, unknown}。"""
        tokens = raw.split()
        if not tokens:
            return None
        name, count = tokens[0], (tokens[1] if len(tokens) > 1 else "1")
        suffix = f"×{count}" if count != "1" else ""
        if name.startswith("%"):
            gid = groups.get(name)
            if gid is None:
                unresolved.append(raw)
                return ("unknown", f"?{name}{suffix}")
            return ("group", f"{gid}{suffix}")
        item_id = byname.get(name.lower())
        if item_id is None and name.lower().startswith("item_"):
            item_id = byname.get(name.lower()[len("item_"):])
        if item_id is None:
            unresolved.append(raw)
            return ("unknown", f"?{name}{suffix}")
        return ("item", f"{item_id}{suffix}")

    def collect_tags(pattern: re.Pattern[str]) -> list[tuple[str, str]]:
        out = []
        for tag in sorted(block):
            if pattern.match(tag):
                for raw in block.get(tag, []):
                    value = resolve(raw)
                    if value:
                        out.append(value)
        return out

    def collect_class(suffix: str) -> list[str]:
        out = []
        for tag, label in CLASS_LABEL.items():
            if tag.endswith(suffix):
                values = [text for _, text in collect_tags(re.compile(re.escape(tag) + "$"))]
                if values:
                    out.append(f"{label}={'/'.join(values)}")
        return out

    raw_groups = [
        ("工作", collect_tags(re.compile(r"quest_work_item\d+$"))),
        ("检查", collect_tags(re.compile(r"check_item\d+_\d+$"))),
        ("收集", collect_tags(re.compile(r"collect_item\d+$"))),
        ("掉落", collect_tags(re.compile(r"drop_item_\d+$"))),
        ("背包", collect_tags(re.compile(r"inventory_item_name\d+$"))),
        ("奖励", collect_tags(re.compile(r"reward_item\d+_\d+$"))),
        ("可选奖励", collect_tags(re.compile(r"selectable_reward_item_?(ext_)?\d+(_\d+)?$"))),
    ]
    ordered: list[tuple[str, list[str]]] = []
    random_groups: list[str] = []
    unknown: list[str] = []
    for label, values in raw_groups:
        items = [text for kind, text in values if kind == "item"]
        random_groups.extend(text for kind, text in values if kind == "group")
        unknown.extend(text for kind, text in values if kind == "unknown")
        if items:
            ordered.append((label, items))
    for label, values in (("职业物品", collect_class("_selectable_item")),
                          ("职业奖励", collect_class("_selectable_reward"))):
        if values:
            ordered.append((label, values))
    if random_groups:
        ordered.append(("随机组", random_groups))
    if unknown:
        ordered.append(("未解", unknown))
    return ordered


def main() -> int:
    repo = find_repo(Path(__file__).resolve())
    wiki = find_wiki_root(repo)
    index = json.loads((wiki / "public/data/quests.index.json").read_text(encoding="utf-8"))
    byname = build_item_index(repo)
    groups, retail_groups_ok = build_random_groups(repo)
    npc_index = build_npc_index(repo)
    object_index, retail_objects_ok = build_object_index(repo)
    family_kill, family_collect, family_interact = load_family_targets(repo)
    client_monster = load_client_monster(repo)

    table_text = (repo / QUEST_TABLE).read_text(encoding="utf-8", errors="replace")
    table: dict[int, dict[str, list[str]]] = {}
    for block in re.findall(r"<quest>(.*?)</quest>", table_text, re.S):
        m = re.search(r"<id>(\d+)</id>", block)
        if not m:
            continue
        tags: dict[str, list[str]] = {}
        for tag, value in re.findall(r"<([a-z0-9_]+)>([^<]*)</\1>", block):
            tags.setdefault(tag, []).append(value.strip())
        table[int(m.group(1))] = tags

    rows: list[str] = []
    live = retired = 0
    unresolved: list[str] = []
    unresolved_targets: list[str] = []
    with_items = 0
    target_counts: dict[str, int] = {}
    with_targets = 0
    for entry in sorted(index, key=lambda e: e["id"]):
        qid = int(entry["id"])
        live_file = repo / f"{LIVE_PREFIX}/{qid}.xml"
        if live_file.exists():
            live += 1
            definition = f"[definitions/quests/{qid}.xml]({LINK_PREFIX}/{LIVE_PREFIX}/{qid}.xml)"
        else:
            retired += 1
            definition = (f"[已退役（真端表驱动；XML 见 git 历史）]"
                          f"({LINK_PREFIX}/{HISTORICAL_PREFIX}/{qid}.xml)")
        tags = table.get(qid, {})
        max_level = (tags.get("maxlevel_permitted") or ["0"])[0]
        max_cell = "无上限" if max_level == "0" else max_level
        item_groups = parse_items(tags, byname, groups, unresolved)
        if item_groups:
            with_items += 1
        item_cell = "；".join(f"{label}:{'、'.join(values)}" for label, values in item_groups)
        # 目标（击杀/采集/收集/交互）：真端族表/掉落列优先，客户端契约仅回退
        client_quest = client_monster.get(qid, {})
        drop_groups = [([t for t in re.split(r"[,;\s]+", value) if t], None)
                       for tag in sorted(tags) if re.fullmatch(r"drop_monster_\d+", tag)
                       for value in tags[tag]]
        kill_groups = merge_groups(family_kill.get(qid, []), drop_groups)
        if not kill_groups:
            kill_groups = merge_groups(*(client_quest.get(t, []) for t in CLIENT_KILL_TYPES))
        target_parts = []
        for label, grps in (("击杀", kill_groups),
                            ("采集", client_quest.get("gatherSource", [])),
                            ("收集", family_collect.get(qid, [])),
                            ("交互", merge_groups(family_interact.get(qid, []),
                                                *(client_quest.get(t, []) for t in CLIENT_INTERACT_TYPES)))):
            if not grps:
                continue
            text = format_targets(grps, npc_index, object_index, unresolved_targets)
            if text:
                target_parts.append(f"{label}:{text}")
                target_counts[label] = target_counts.get(label, 0) + 1
        if target_parts:
            with_targets += 1
        target_cell = "；".join(target_parts)
        rows.append("| {id} | {level} | {en} | {zh} | {limits} | {prereq} | {accept} | {defn} "
                    "| {max} | {items} | {targets} |".format(
                        id=qid,
                        level=cell(entry.get("level")),
                        en=cell(entry.get("nameEn")),
                        zh=cell(entry.get("nameZh")),
                        limits=cell(entry.get("limits")),
                        prereq=cell(entry.get("prereq")),
                        accept=cell(entry.get("accept")),
                        defn=definition,
                        max=max_cell,
                        items=cell(item_cell),
                        targets=cell(target_cell),
                    ))

    header = f"""# Aion 任务目录（中英对照）

> 生成日期：2026-10-05（重建；生成器 `.agents/summary/quest-catalog-rebuild/build_quest_catalog.py`）。
> 数据范围：客户端存在任务集（与 QuestWiki 索引一致，{len(rows)} 条）；真端表中无客户端数据的任务不收录。
> 中文名称：Aion 5.8 客户端字符串（与 display-name-id 精确匹配；经 QuestWiki 索引回填，不猜译）。
> 英文名称：迁移前任务定义 `metadata/@name`（经 QuestWiki 索引回填；真端表仅存韩文 dev_name）。
> 接取等级：真端表 `minlevel_permitted`（与索引全量一致）；存在有效 `maxlevel_permitted` 时输出区间，否则输出“起始等级+”。
> 上限等级：真端表 `maxlevel_permitted`；0 = 无上限；`998/999` 为真端哨兵值（按原值输出）。
> 道具物品 id：真端表物品列（工作/检查/收集/掉落/背包/奖励/可选奖励/职业）经物品模板 `name_desc → id` 解析
> （两通道：原名 → 去 `ITEM_` 前缀重查）；`%` 前缀 = 随机奖励组（<真端根>/Map/XML/quest_random_rewards.xml
> 优先，缺组回退仓库内 legacy 副本{'' if retail_groups_ok else '（真端根不可用）'}）；
> `×n` 为数量（n=1 省略）；`?名字` = 未解析符号。有物品声明的任务 {with_items} 条；未解析符号 {len(unresolved)} 处。
> 目标（击杀/采集/收集/交互）：真端优先——击杀取零售族表 Quest_SimpleHunt（monsterN+countN 组）/
> Quest_SimpleSerialHunt / data_driven_quest 的 Hunt 进度（尾数字为组计数），合并零售 quest.xml 的
> `drop_monster_*` 掉落怪列；真端无声明时回退客户端契约（definitions/quest_monster/quest_monster.csv）击杀类行。
> 采集 = 客户端契约 gatherSource（经真端 `<真端根>/Map/XML/Objects.xml` 的 harvest_source 表解析为对象 id{'' if retail_objects_ok else '；真端根不可用，未能解析'}）；
> 收集 = 族表 SimpleCollectItem.objectN + data_driven CollectItem；交互 = 族表 SimpleTalk/SimpleItemPlay/SimpleSerialHunt 的
> talk_npc*（中继目标 NPC）+ data_driven Talk/TalkFOBJ/EnterArea + 客户端契约 goodsList/itemUseArea。
> 名字→id：仓库内 npcs/npc_template_*.xml 的 `name ∪ name_desc`（一名多 id 时全部列出，`/` 分隔）；
> 未解析输出 `?名字`；每分类最多 {TARGET_REF_CAP} 个名，超出记 `…+k`（组内 `/` 相连、组间 `、`，`×n` 为组计数）。
> 有目标声明的任务 {with_targets} 条（击杀 {target_counts.get('击杀', 0)} / 采集 {target_counts.get('采集', 0)} / 收集 {target_counts.get('收集', 0)} / 交互 {target_counts.get('交互', 0)}）；未解析目标符号 {len(unresolved_targets)} 处。
> 限制条件 / 前置任务 / 接取方式：迁移前任务定义与真端表列（经 QuestWiki 索引回填）。
> 定义文件：现行任务 XML 直接链接；真端表驱动（已退役）的任务链接历史路径并标注，见 git 历史
> `git log --follow -- {HISTORICAL_PREFIX}/<id>.xml`。
> 现行定义 XML {live} 条；真端表驱动 {retired} 条。

| 任务 ID | 接取等级 | 英文名称 | 中文名称 | 限制条件 | 前置任务 | 接取方式 | 定义文件 | 上限等级 | 道具物品 id | 目标（击杀/采集/收集/交互） |
|---:|---:|---|---|---|---|---|---|---:|---|---|
"""

    out = repo / "docs/QUEST_CATALOG.zh-CN.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(header + "\n".join(rows) + "\n", encoding="utf-8")

    # 自检：按 QuestWiki loadCatalog 的解析规则逐行验证（定义列必须仍在第 7 格 cells[6]）
    catalog_row = re.compile(r"^\|\s*(\d+)\s*\|(.*)$")
    link = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
    parsed = 0
    for line in (header + "\n".join(rows)).splitlines():
        m = catalog_row.match(line)
        if not m:
            continue
        cells = m.group(2).split("|")
        if len(cells) < 8 or not link.search(cells[6]):
            print(f"PARSE_FAIL: {line[:120]}", file=sys.stderr)
            return 1
        parsed += 1
    if parsed != len(rows):
        print(f"ROW_COUNT_MISMATCH: parsed={parsed} rows={len(rows)}", file=sys.stderr)
        return 1
    print(f"QUEST_CATALOG_OK rows={len(rows)} live_link={live} retired_link={retired} "
          f"with_items={with_items} unresolved_item_symbols={len(unresolved)} "
          f"with_targets={with_targets} targets={target_counts} "
          f"unresolved_target_symbols={len(unresolved_targets)} path={out.relative_to(repo)}")
    if unresolved:
        print("UNRESOLVED_SAMPLE:", sorted(set(unresolved))[:6])
    if unresolved_targets:
        print("UNRESOLVED_TARGET_SAMPLE:", sorted(set(unresolved_targets))[:8])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
