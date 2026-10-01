#!/usr/bin/env python3
"""P0b: 真端表入仓工具（幂等）。

1. 对 4 张新表做 byte 级一致拷贝（UTF-16 原样，不改编码/不改语义），
   拷贝后 sha256 必须与源相等，否则拒绝写出。
2. 生成/更新 table-source-provenance.tsv（全部 12 张入仓真端表：
   8 张已转换表 + 4 张新 byte 级表），路径只写名称引用（<真端根>），
   不写开发者机器路径。
3. 重跑幂等：输出与上次相同即无操作。
"""
import hashlib
import pathlib
import shutil
import subprocess
import sys

SRC = pathlib.Path('/Users/mc/IdeaProjects/58Server/Map/XML')
REPO = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
RETAIL = REPO / 'src/main/resources/aion/data/static_data/quest/retail'
OUT = RETAIL / 'table-source-provenance.tsv'

NEW_TABLES = [
    # (source, repo_name, rows, note)
    ('HtmlPages.xml', 'HtmlPages.xml', 5904, 'P0b: 页注册表（计划 §4.6.1）'),
    ('challenge_task.xml', 'challenge_task.xml', 123, 'P0b: 挑战任务（P0a 决策参与）'),
    ('quest_random_rewards.xml', 'quest_random_rewards.xml', 817, 'P0b: 随机奖励（legacy 副本陈旧，P0a 决策重入仓）'),
    ('npcfactions_quest.xml', 'npcfactions_quest.xml', 436, 'P0b: 阵营任务（P7 前置）'),
]

# 8 张已入仓表（P0a 已证 token 语义等价；source_sha256 取自 P0a table-source-inventory.tsv）
EXISTING = [
    ('quest.xml', 'quest.xml', 10035, '850b7afc85e60ad01d55ddc9e8a73e2077b985fb405455510a655988d068132c'),
    ('data_driven_quest.xml', 'data_driven_quest.xml', 2492, '624195b8afed12d648ff3896f502daa040d02826d492509873da4e2b8592b609'),
    ('Quest_SimpleTalk.xml', 'Quest_SimpleTalk.xml', 3152, '7cd509d7b5e4dfea0867c90abde1e5bcd83806c7d14174c9a53791294dc93f5f'),
    ('Quest_SimpleHunt.xml', 'Quest_SimpleHunt.xml', 1863, 'c6fd9828d7c6f75cb4bcd6cdcced60268affc5f6d96d4dfc1f8aaa36a2af9571'),
    ('Quest_CombineTask.xml', 'Quest_CombineTask.xml', 574, 'f4af3e797c4984e495409a2ae130ef959ad3fa37d43eb62d41b62847b31608c3'),
    ('Quest_SimpleCollectItem.xml', 'Quest_SimpleCollectItem.xml', 262, '5612a374b5b7901c72fae7cbc7570f337aa0e7dd8d54c2eed3bc29c9b8d800b5'),
    ('Quest_SimpleUseItem.xml', 'Quest_SimpleUseItem.xml', 160, '8b4192812fdcb1a41016007249218964a2553e432d3a04f075ae6c382ca70a21'),
    ('Quest_SimpleItemPlay.xml', 'Quest_SimpleItemPlay.xml', 43, 'def4640956ad5d9a671b0a857448392c8f1fb2d70898db43460316505aee19bb'),
    ('Quest_SimpleSerialHunt.xml', 'Quest_SimpleSerialHunt.xml', 16, 'd4cb9a033b36bf972da27e62d05a03029a0bb7295cc17051e073f6f8f1487e44'),
]


def sha256_file(p):
    h = hashlib.sha256()
    with open(p, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def git_sha_of(path):
    """入仓文件的 git blob sha（HEAD 版本，用于幂等对照输出）。"""
    rel = path.relative_to(REPO)
    r = subprocess.run(['git', '-C', str(REPO), 'rev-parse', f'HEAD:{rel}'], capture_output=True, text=True)
    return r.stdout.strip() if r.returncode == 0 else '(new)'


def main():
    for src_name, repo_name, rows, note in NEW_TABLES:
        src = SRC / src_name
        dst = RETAIL / repo_name
        src_sha = sha256_file(src)
        tmp = dst.with_suffix('.xml.p0b-tmp')
        shutil.copyfile(src, tmp)
        if sha256_file(tmp) != src_sha:
            tmp.unlink()
            print(f'FAIL byte-identity {src_name}', file=sys.stderr)
            sys.exit(1)
        if dst.exists() and sha256_file(dst) == src_sha:
            tmp.unlink()
            print(f'unchanged {repo_name} sha256={src_sha}')
        else:
            tmp.replace(dst)
            print(f'ingested {repo_name} rows={rows} sha256={src_sha} note={note}')

    lines = ['table\tsource_ref\tsource_sha256\trepo_path\trepo_sha256\tencoding\ttransformation\trows']
    for src_name, repo_name, rows, _ in NEW_TABLES:
        s_sha = sha256_file(SRC / src_name)
        r_sha = sha256_file(RETAIL / repo_name)
        assert s_sha == r_sha, repo_name
        lines.append(f'{src_name}\t<真端根>/Map/XML/{src_name}\t{s_sha}\tretail/{repo_name}\t{r_sha}\t'
                     f'UTF-16LE(BOM)\tBYTE_IDENTICAL\t{rows}')
    for src_name, repo_name, rows, s_sha in EXISTING:
        r_sha = sha256_file(RETAIL / repo_name)
        lines.append(f'{src_name}\t<真端根>/Map/XML/{src_name}\t{s_sha}\tretail/{repo_name}\t{r_sha}\t'
                     f'UTF-8\tCONVERTED_UTF8_WHITESPACE_NORMALIZED(P0a token-semantic-equal)\t{rows}')
    OUT.write_text('\n'.join(lines) + '\n')
    print(f'provenance manifest: {OUT.relative_to(REPO)} rows={len(lines) - 1}')
    for src_name, repo_name, *_ in NEW_TABLES:
        print(f'git-state {repo_name}: HEAD={git_sha_of(RETAIL / repo_name)}')


if __name__ == '__main__':
    main()
