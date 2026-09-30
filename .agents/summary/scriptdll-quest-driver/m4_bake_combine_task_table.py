#!/usr/bin/env python3
"""M4-a：真端 `Quest_CombineTask.xml`（UTF-16 + DTD）转 UTF-8 入仓。

源：<真端根>/Map/XML/Quest_CombineTask.xml
目标：src/main/resources/aion/data/static_data/quest_retail/Quest_CombineTask.xml
与 M3 的 SimpleTalk/SimpleHunt 表处理方式一致：只做编码转换，不改内容。
"""
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

import os
import pathlib
import sys

SRC = pathlib.Path(f"{REPO.parent / '58Server'}/Map/XML/Quest_CombineTask.xml")
DST = pathlib.Path(
    "src/main/resources/aion/data/static_data/quest_retail/Quest_CombineTask.xml")


def main() -> int:
    raw = SRC.read_bytes()
    text = raw.decode("utf-16")
    # 统一换行为 LF，去掉 BOM；内容（含 DTD 实体块）保持原样。
    text = text.lstrip("\ufeff").replace("\r\n", "\n")
    text = text.replace('encoding="UTF-16"', 'encoding="UTF-8"', 1)
    DST.write_text(text, encoding="utf-8")
    rows = text.count("<id id=")
    print(f"wrote {DST} rows={rows} bytes={DST.stat().st_size}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
