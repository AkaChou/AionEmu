#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
由门禁差异导出（-Dretail.metadata.diffOut=...）生成分歧登记表
src/test/resources/quest/retail-metadata-divergences.tsv。

每行 = (quest_id, axis, reason)。reason 是已定性的差异类：
  RETAIL_COND_PLACEMENT  真端 finished_quest_cond 在生产 XML 有 prerequisites /
                         start-conditions 两种等价表达；映射器按固定规则归属
                         （无后缀→prerequisites，带后缀或与他族共存→finished 条件），
                         两侧对玩家表达同一真端语义（完成后可接）。
  XML_ONLY_ASSET         真端 quest.xml 不携带该数据（bonuses/kills 来自客户端附带表），
                         且 typed 运行链路无消费方（grep 证据），映射为空行为等价。
  RETAIL_PRIORITY        其余逐任务口径分歧：真端表为权威（用户既定规则 §2.5），
                         生产 XML 为转换期方言或旧版资产（清单可枚举、可回溯）。
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
OUT = os.path.join(REPO, "src/test/resources/quest/retail-metadata-divergences.tsv")

REASONS = {
    "startConditions": "RETAIL_COND_PLACEMENT",
    "prerequisites": "RETAIL_COND_PLACEMENT",
    "bonuses": "XML_ONLY_ASSET",
    "kills": "XML_ONLY_ASSET",
}


def main(dump_path):
    rows = []
    seen = set()
    with open(dump_path, encoding="utf-8") as fh:
        for line in fh:
            if line.startswith("#"):
                continue
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 2 or parts[1] == "-":
                continue
            axis = parts[0]
            quest_id = parts[1]
            if not quest_id.isdigit():
                continue
            key = (quest_id, axis)
            if key in seen:
                continue
            seen.add(key)
            rows.append((quest_id, axis, REASONS.get(axis, "RETAIL_PRIORITY")))
    rows.sort(key=lambda r: (int(r[0]), r[1]))
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("# 真端元数据层已登记分歧（quest_id, axis, reason）；由 audit_generate_metadata_divergences.py 生成\n")
        fh.write("# 生成源：RetailMetadataEquivalenceGateTest -Dretail.metadata.diffOut 导出 + 人工定性\n")
        for quest_id, axis, reason in rows:
            fh.write(f"{quest_id}\t{axis}\t{reason}\n")
    from collections import Counter
    print(f"registered rows: {len(rows)}  quests: {len({r[0] for r in rows})}")
    for reason, n in Counter(r[2] for r in rows).most_common():
        print(f"  {reason}\t{n}")


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/retail-metadata-diffs6.tsv"))
