#!/usr/bin/env python3
"""M4-a：CombineTask 574 行的形状/可迁移性勘察。

对每个 CombineTask 任务比较三方：
- 真端 `Quest_CombineTask.xml`（task_npc / combineskill / combine_skillpoint / recipe_name / product / give_componentN）
- 生产 `quests/<id>.xml`（combine-skill / combine-skill-point / items / work-items / learn-recipe / 节点与路由形状）
- 本服索引（npc 名、item 名、recipe 模板）

输出：retail-combine-task-shapes.tsv + stdout 汇总。
"""
from __future__ import annotations

import pathlib
import re
import sys
from collections import Counter

REPO = pathlib.Path(__file__).resolve().parents[3]
RETAIL = REPO / "src/main/resources/aion/data/static_data/quest_retail/Quest_CombineTask.xml"
RETENTION = REPO / "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
ITEM_DIR = REPO / "src/main/resources/aion/data/static_data/items/item"
NPC_DIR = REPO / "src/main/resources/aion/data/static_data/npcs"
RECIPES = REPO / "src/main/resources/aion/data/static_data/recipe/recipe_templates.xml"
OUT = pathlib.Path(__file__).resolve().parent / "retail-combine-task-shapes.tsv"

SKILLS = {
    "weaponsmith": 40002, "armorsmith": 40003, "handiwork": 40008, "alchemy": 40007,
    "tailoring": 40004, "cooking": 40001, "menuisier": 40010,
    "gathering_b": 30002, "aerial_gathering": 30003,
}

ROW = re.compile(r"<id id=\"(\d+)\">(.*?)</id>", re.DOTALL)
FIELD = re.compile(r"<(\w+)>(.*?)</\1>", re.DOTALL)


def parse_retail():
    text = RETAIL.read_text(encoding="utf-8")
    rows = {}
    for quest_id, body in ROW.findall(text):
        fields = {name: value.strip() for name, value in FIELD.findall(body)}
        rows[int(quest_id)] = fields
    return rows


def factory_ids():
    ids = set()
    for line in RETENTION.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) >= 3 and parts[2] == "CombineTask":
            ids.add(int(parts[0]))
    return ids


def item_index():
    by_name = {}
    for path in sorted(ITEM_DIR.glob("*.xml")):
        text = path.read_text(encoding="utf-8", errors="replace")
        for tag in re.findall(r"<item_template\b[^>]*>", text):
            name = re.search(r'name_desc="([^"]*)"', tag)
            item_id = re.search(r'\bid="(\d+)"', tag)
            if name and item_id:
                by_name.setdefault(name.group(1).lower(), int(item_id.group(1)))
    return by_name


def npc_index():
    by_name = {}
    for path in sorted(NPC_DIR.glob("npc_template_*.xml")):
        text = path.read_text(encoding="utf-8", errors="replace")
        for tag in re.findall(r"<npc_template\b[^>]*>", text):
            name = re.search(r'name_desc="([^"]*)"', tag)
            npc_id = re.search(r'npc_id="(\d+)"', tag)
            if name and npc_id:
                by_name.setdefault(name.group(1).lower(), set()).add(int(npc_id.group(1)))
    return by_name


def recipe_index():
    text = RECIPES.read_text(encoding="utf-8", errors="replace")
    by_pair = {}
    for tag in re.findall(r"<recipe_template\b[^>]*>", text):
        fields = dict(re.findall(r'(\w+)="([^"]*)"', tag))
        try:
            recipe_id = int(fields["id"])
        except (KeyError, ValueError):
            continue
        skill = fields.get("skillid")
        product = fields.get("productid")
        if skill and product:
            by_pair.setdefault((int(skill), product.lower()), []).append(recipe_id)
    return by_pair


def xml_shape(quest_id):
    path = QUESTS / f"{quest_id}.xml"
    if not path.exists():
        return None
    text = path.read_text(encoding="utf-8")
    shape = {}
    meta = re.search(r"<metadata\b[^>]*>", text)
    shape["skill"] = re.search(r'combine-skill="(\d+)"', meta.group()) if meta else None
    shape["skill"] = int(shape["skill"].group(1)) if shape["skill"] else None
    point = re.search(r'combine-skill-point="(\d+)"', meta.group()) if meta else None
    shape["point"] = int(point.group(1)) if point else 0
    items_block = re.search(r"<items>(.*?)</items>", text, re.DOTALL)
    shape["items"] = [(int(i), int(c)) for i, c in re.findall(r'<item id="(\d+)" count="(\d+)"',
        items_block.group(1) if items_block else "")]
    shape["work_items"] = [(int(i), int(c)) for i, c in re.findall(r'<item id="(\d+)" count="(\d+)"',
        re.search(r"<work-items>(.*?)</work-items>", text, re.DOTALL).group(1) if "<work-items>" in text else "")]
    shape["recipes"] = sorted({int(r) for r in re.findall(r'<learn-recipe recipe-id="(\d+)"', text)})
    shape["nodes"] = re.findall(r'<node label="([^"]+)" status="([^"]+)">\s*<var name="var0" value="(\d+)"', text)
    shape["transitions"] = len(re.findall(r'<transition\b|<dialog\b|<npc-complete\b', text))
    return shape


def main() -> int:
    retail = parse_retail()
    family = factory_ids()
    items = item_index()
    npcs = npc_index()
    recipes = recipe_index()
    print(f"retail rows={len(retail)} family={len(family)} items={len(items)} npcs={len(npcs)} recipes={len(recipes)}")

    stats = Counter()
    rows = []
    for quest_id in sorted(family):
        row = retail.get(quest_id)
        if row is None:
            stats["no_retail_row"] += 1
            rows.append((quest_id, "NO_RETAIL_ROW", "", "", "", "", ""))
            continue
        npc_names = [n.strip() for n in row.get("task_npc", "").split(",") if n.strip()]
        npc_ok = all(len(npcs.get(n.lower(), ())) == 1 for n in npc_names) and bool(npc_names)
        stats["npc_unique" if npc_ok else "npc_unresolved"] += 1

        product = row.get("product", "").strip().split()
        product_id = items.get(product[0].lower()) if product else None
        product_count = int(product[1]) if len(product) > 1 else None

        components = []
        for index in range(1, 9):
            raw = row.get(f"give_component{index}")
            if not raw:
                continue
            parts = raw.split()
            item_id = items.get(parts[0].lower()) if parts else None
            components.append((item_id, int(parts[1]) if len(parts) > 1 else None))

        shape = xml_shape(quest_id)
        if shape is None:
            stats["xml_missing"] += 1
            continue
        stats["xml_present"] += 1

        skill = SKILLS.get(row.get("combineskill", "").strip())
        skill_ok = skill is not None and skill == shape["skill"]
        stats["skill_match" if skill_ok else "skill_mismatch"] += 1

        point = int(row.get("combine_skillpoint", "0").strip() or 0)
        point_ok = point == shape["point"]
        stats["point_match" if point_ok else "point_mismatch"] += 1

        item_ok = product_id is not None and shape["items"] == [(product_id, product_count)]
        stats["product_match" if item_ok else "product_mismatch"] += 1

        component_ok = components and shape["work_items"] == components
        stats["component_match" if component_ok else "component_mismatch"] += 1

        recipe_ids = recipes.get((skill, str(product_id)), []) if product_id and skill else []
        recipe_ok = bool(recipe_ids) and set(shape["recipes"]) == set(recipe_ids)
        stats["recipe_match" if recipe_ok else "recipe_unresolved"] += 1
        stats["nodes_" + "/".join(f"{label}:{status}:v{value}" for label, status, value in shape["nodes"])] += 1

        rows.append((quest_id, "OK" if all((npc_ok, skill_ok, point_ok, item_ok, component_ok, recipe_ok))
            else "PARTIAL", ",".join(npc_names), str(skill), str(point),
            f"{product_id}x{product_count}", str(recipe_ids[:1])))

    OUT.write_text("# CombineTask 574 行形状勘察（quest_id, verdict, npc, skill, point, product, recipe）\n"
        + "\n".join("\t".join(str(v) for v in row) for row in rows) + "\n", encoding="utf-8")
    print(f"wrote {OUT}")
    for key, count in sorted(stats.items()):
        if key.startswith("nodes_"):
            continue
    for key in sorted(stats):
        print(f"  {key:24s} {stats[key]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
