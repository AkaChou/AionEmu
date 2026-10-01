#!/usr/bin/env python3
"""聚焦套件红类清单与逐类差集（P4 收口）。

用法：
  python3 red_class_delta.py classes <mvn.log> <out.tsv>
  python3 red_class_delta.py delta   <base.log> <base.tsv> <new.log> <new.tsv> <out.tsv>

解析口径：surefire 每类一行 `Tests run: T, Failures: F, Errors: E, Skipped: S ... -- in <class>`；
红类 = F+E > 0。差集只列出三元组（tests/failures/errors）不同的类，NEW/REMOVED 用字面量标记。
"""
import re
import sys

LINE = re.compile(
    r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+).*? -- in ([\w.$]+)")
TOTAL = re.compile(r"^\[(?:INFO|ERROR)\] Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)$")


def parse(path):
    classes, total = {}, None
    with open(path, encoding="utf-8", errors="replace") as handle:
        for raw in handle:
            line = raw.rstrip("\n")
            match = LINE.search(line)
            if match:
                tests, failures, errors, skipped, name = (
                    int(match.group(1)), int(match.group(2)), int(match.group(3)),
                    int(match.group(4)), match.group(5))
                classes[name] = (tests, failures, errors, skipped)
                continue
            match = TOTAL.match(line)
            if match:
                total = tuple(int(match.group(i)) for i in range(1, 5))
    return classes, total


def write_classes(log, out):
    classes, total = parse(log)
    red = {k: v for k, v in classes.items() if v[1] + v[2] > 0}
    with open(out, "w", encoding="utf-8") as handle:
        handle.write("# P4 聚焦套件红类清单（mvn -o test '-Dtest=*Quest*Test,*Retail*Test'）\n")
        handle.write("# 日志：%s\n" % log)
        handle.write("class\ttests\tfailures\terrors\tskipped\n")
        for name in sorted(red):
            tests, failures, errors, skipped = red[name]
            handle.write("%s\t%d\t%d\t%d\t%d\n" % (name, tests, failures, errors, skipped))
    print("total=%s classes=%d red=%d out=%s" % (total, len(classes), len(red), out))
    return red


def write_delta(base_log, base_tsv, new_log, new_tsv, out):
    base, base_total = parse(base_log)
    new, new_total = parse(new_log)
    names = sorted(set(base) | set(new))
    missing = {"<absent>": None}
    rows = []
    for name in names:
        left = base.get(name)
        right = new.get(name)
        if left == right:
            continue
        parts = []
        if left is None:
            parts.append("NEW")
        if right is None:
            parts.append("REMOVED")
        if left and right:
            for index, label in ((1, "F"), (2, "E"), (0, "T")):
                diff = right[index] - left[index]
                if diff:
                    parts.append("%s%+d" % (label, diff))
        def fmt(value):
            if value is None:
                return "<absent>\t<absent>\t<absent>"
            return "%d\t%d\t%d" % (value[0], value[1], value[2])
        rows.append("%s\t%s\t%s\t%s" % (name, fmt(left), fmt(right), ";".join(parts)))
    with open(out, "w", encoding="utf-8") as handle:
        handle.write("# P4 收口：聚焦套件逐类 tests/failures/errors 三元组差集（基线 = 上一批日志）\n")
        handle.write("# 命令：mvn -o test -Dtest=*Quest*Test,*Retail*Test -DfailIfNoTests=false\n")
        handle.write("# 日志：%s（基线） / %s（本批）\n" % (base_log, new_log))
        handle.write("# 总量：基线 %s / 本批 %s\n" % (base_total, new_total))
        handle.write("class\tbase_tests\tbase_failures\tbase_errors\tnew_tests\tnew_failures\tnew_errors\tdelta\n")
        for row in rows:
            handle.write(row + "\n")
    base_red = {k for k, v in base.items() if v[1] + v[2] > 0}
    new_red = {k for k, v in new.items() if v[1] + v[2] > 0}
    print("base_total=%s new_total=%s" % (base_total, new_total))
    print("red ADDED=%s REMOVED=%s" % (sorted(new_red - base_red), sorted(base_red - new_red)))
    print("changed triplets=%d out=%s" % (len(rows), out))


if __name__ == "__main__":
    if sys.argv[1] == "classes":
        write_classes(sys.argv[2], sys.argv[3])
    else:
        write_delta(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6])
