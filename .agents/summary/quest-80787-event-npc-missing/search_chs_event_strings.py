"""在 5.8 中文客户端 L10N 串表里检索"活动名"候选（只读）。

数据源：patch/L10N/CHS/Data/data.pak（实为 ZIP，内含 Strings/client_strings_*.xml，UTF-16）。
用法: python3 search_chs_event_strings.py <关键词> [关键词...]
"""
import re
import sys
import zipfile

PAK = "patch/L10N/CHS/Data/data.pak"
TABLES = [f"Strings/client_strings_{n}.xml" for n in
          ("msg", "ui", "etc", "item", "item2", "item3", "npc", "quest", "skill",
           "dic_etc", "dic_item", "dic_monster", "dic_people", "dic_place", "level")]

ENTRY = re.compile(r"<string>\s*<id>(\d+)</id>\s*<name>([^<]*)</name>\s*<body>(.*?)</body>", re.S)

def main(keywords):
    z = zipfile.ZipFile(PAK)
    seen = 0
    for table in TABLES:
        try:
            txt = z.read(table).decode("utf-16")
        except KeyError:
            continue
        for sid, name, body in ENTRY.findall(txt):
            body = body.strip()
            for kw in keywords:
                if kw in body or kw.lower() in name.lower():
                    print(f"{table.split('_', 2)[-1][:-4]}\t{sid}\t{name}\t{body[:90]}")
                    seen += 1
                    break
    print(f"\n命中 {seen} 条", file=sys.stderr)

if __name__ == "__main__":
    main(sys.argv[1:])
