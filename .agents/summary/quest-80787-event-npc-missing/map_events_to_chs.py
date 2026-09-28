"""把 events_config.xml 的活动名映射到 5.8 中文客户端串表里的中文名候选（只读）。

做法：活动名 → 一组客户端内部事件 token（KEY 片段）→ 在该 token 命中的串里挑"标题型"中文串。
"""
import re
import sys
import zipfile
from collections import defaultdict

PAK = "patch/L10N/CHS/Data/data.pak"
TABLES = [f"Strings/client_strings_{n}.xml" for n in
          ("msg", "ui", "etc", "item", "item2", "item3", "npc", "quest", "skill",
           "dic_etc", "dic_item", "dic_monster", "dic_people", "dic_place", "level")]
ENTRY = re.compile(r"<string>\s*<id>(\d+)</id>\s*<name>([^<]*)</name>\s*<body>(.*?)</body>", re.S)

# 活动名 -> 客户端 KEY token（正则，大小写不敏感）
TOKENS = {
    "Novice Event": r"NOVICE|NEWBIE|INITIATION",
    "Mega Kinah Event": r"MEGAKINAH|MEGA_KINAH|KINAH100|MILLION",
    "Guardian General Event": r"GUARDIANGENERAL|GUARDIAN_GENERAL|GUARDGENERAL",
    "Sweet Fruits": r"SWEETFRUIT|SWEET_FRUIT|FRUIT",
    "Summer Block Party Event": r"BLOCKPARTY|BLOCK_PARTY|SUMMER",
    "Creativity Event": r"CREATIV",
    "Mooncake Event": r"MOONCAKE|MOON_CAKE",
    "Toy Festival": r"TOYFEST|TOY_FEST|TOYFESTIVAL|TONIRON",
    "Legendary Symphony Event": r"SYMPHONY",
    "Ardent Heat": r"ARDENT|HEAT",
    "Wishing Fountain Event": r"WISHING|FOUNTAIN",
    "Ascension Energy": r"ASCENSION",
    "Black Star Event": r"BLACKSTAR|BLACK_STAR",
    "Holy Water Event": r"HOLYWATER|HOLY_WATER",
    "9th Anniversary Event": r"9TH|ANNIVERSARY",
    "Pumpkin Cookie Event": r"PUMPKINCOOKIE|COOKIE",
    "Pumpkin Kings Haunt": r"PUMPKINKING|PUMPKIN_KING|HALLOWEEN",
    "Daru Days": r"\bDARU\b|DARUDAY|DARUDAY",
    "Celebrate Solorius Event": r"SOLORIUS|CHRISTMAS|XMAS",
    "Snowy Freezy": r"SNOWY|FREEZY",
    "Code Red: Atreia": r"CODERED|CODE_RED",
    "Curse Of Aion Event": r"CURSE",
    "Doppelgangers": r"DOPPEL",
    "Enemy Land Event": r"ENEMYLAND|ENEMY_LAND",
    "Golden Feather Event": r"GOLDENFEATHER|GOLDEN_FEATHER",
    "IDEvent S3 Event": r"IDEVENT",
    "Museum": r"MUSEUM",
    "Shop Event": r"SHOPEVENT|SHOP_EVENT",
    "Shugo Ranger Event": r"SHUGORANGER|SHUGO_RANGER|RANGER",
    "Wings With Stars Event": r"WINGSWITHSTARS|STARWING|WINGSTAR",
    "Miniring Event": r"MINIRING|MINIRIM|MINIRING",
    "Secrets Of The Ancients": r"SECRET.*ANCIENT|ANCIENT",
    "New Year Event": r"NEWYEAR|NEW_YEAR",
    "Dreaming Nether Weapon Event": r"NETHER|DREAMING",
    "Fluffy Snow Event": r"FLUFFY|SNOWFLAKE",
    "Gift Giver Event": r"GIFTGIVER|GIFT_GIVER",
    "The Alchemist Event": r"ALCHEMIST",
    "Valentine's Day Event": r"VALENTINE",
    "Joker Wild Event": r"JOKER",
    "White Day Inventory": r"WHITEDAY|WHITE_DAY",
    "Rainbow Snake Festival": r"RAINBOWSNAKE|SNAKE",
    "Flower Day Event": r"FLOWERDAY|FLOWER_DAY",
    "Pink Petal Event": r"PINKPETAL|PINK_PETAL|PETAL",
    "All Dolled Up": r"DOLL|MATRYOSHKA|NESTINGDOLL",
    "Event Maid Promotion": r"\bMAID\b|MAIDCAFE|MAID_CAFE",
    "Patron Mark Inventory": r"PATRON",
    "Captain Harlock Event": r"HARLOCK",
    "The Lost Words": r"LOSTWORD|LOST_WORD|WORD_EVENT",
    "Thecla Royan": r"THECLA",
    "October Fest": r"OCTOBERFEST|OKTOBERFEST|OCTOBER_FEST",
}

def load_index():
    z = zipfile.ZipFile(PAK)
    entries = []
    for table in TABLES:
        try:
            txt = z.read(table).decode("utf-16")
        except KeyError:
            continue
        for sid, name, body in ENTRY.findall(txt):
            entries.append((table.split("client_strings_")[-1][:-4], sid, name, body.strip()))
    return entries

def main(argv):
    entries = load_index()
    wanted = argv or list(TOKENS)
    for event in wanted:
        pat = re.compile(TOKENS[event], re.I)
        matched = [e for e in entries if pat.search(e[2])]
        # 标题型：body 短、含"活动/节/庆典/祭"，或 key 带 EVENT 且 body < 60
        titled = [e for e in matched if len(e[3]) <= 60 and re.search(r"活动|节|庆典|祭|纪念", e[3])]
        print(f"### {event}  (命中 key {len(matched)}，标题型 {len(titled)})")
        for table, sid, name, body in titled[:14]:
            print(f"   [{table}] {name} = {body}")
        print()

if __name__ == "__main__":
    main(sys.argv[1:])
