#!/usr/bin/env python3
"""retail 十表 DOCTYPE 剥离批（2026-10-03，零行为变更）。

背景：真端十张表自带 `<!DOCTYPE strings [...]>` 内部实体子集（约 100+ 自名实体）。
实测语义（JDK Xerces，与运行时装载器同配置）：
- 5 个预定义实体（lt/gt/amp/quot/apos）不受自名声明影响，展开为标准字符；
- 非预定义实体（hellip）按声明文本展开为**字面量** `hellip`（如 `요정으로부터&hellip;` → `요정으로부터hellip`）。
⇒ 移除 DOCTYPE 后：预定义引用语义不变（XML 内建）；`&hellip;` 会成未定义实体炸解析，
   必须落盘为字面量 `hellip`（= 当前解析结果，逐字符不变）。

变换（逐文件最小字节改动）：
- 9 个 UTF-8 文件：删除 DOCTYPE 块（`<!DOCTYPE`…`]>` + 随行换行），`&hellip;` → `hellip`；
- npcfactions_quest.xml（唯一 UTF-16LE+BOM+CRLF，用户裁定转码）：UTF-16→UTF-8、
  CRLF→LF、声明 `encoding="UTF-16"`→`"UTF-8"`、删 DOCTYPE（该文件正文零实体引用）。

等价证据 = `xml_dom_probe dump` 变换前后逐元素比对（见本 topic dom-before/dom-after）。
幂等：无 DOCTYPE 时不做任何事。
"""
import hashlib
import pathlib
import re
import sys

REPO = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
RETAIL = REPO / 'src/main/resources/aion/data/static_data/quest/retail'

UTF8_FILES = ['Quest_CombineTask', 'Quest_SimpleCollectItem', 'Quest_SimpleHunt', 'Quest_SimpleItemPlay',
              'Quest_SimpleSerialHunt', 'Quest_SimpleTalk', 'Quest_SimpleUseItem', 'data_driven_quest', 'quest']
UTF16_FILE = 'npcfactions_quest'

DOCTYPE_BYTES = re.compile(rb'<!DOCTYPE[^\n]*\[.*?\]>\n', re.S)
DOCTYPE_TEXT = re.compile(r'<!DOCTYPE[^\n]*\[.*?\]>\r?\n', re.S)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def strip_utf8(name: str) -> None:
    path = RETAIL / f'{name}.xml'
    raw = path.read_bytes()
    before = sha256(raw)
    if b'<!DOCTYPE' not in raw:
        print(f'{name}.xml: no DOCTYPE (idempotent no-op) sha256={before}')
        return
    body = DOCTYPE_BYTES.sub(b'', raw, count=1)
    hellip = body.count(b'&hellip;')
    body = body.replace(b'&hellip;', b'hellip')
    path.write_bytes(body)
    print(f'{name}.xml: {len(raw)} -> {len(body)} B  hellip-refs={hellip}  '
          f'sha256 {before[:12]} -> {sha256(body)[:12]}')


def strip_utf16(name: str) -> None:
    path = RETAIL / f'{name}.xml'
    raw = path.read_bytes()
    before = sha256(raw)
    text = raw.decode('utf-16')  # BOM 自动识别
    if '<!DOCTYPE' not in text:
        print(f'{name}.xml: no DOCTYPE (idempotent no-op) sha256={before}')
        return
    body = DOCTYPE_TEXT.sub('', text, count=1)
    assert body.count('&hellip;') == 0, 'unexpected entity refs in npcfactions body'
    body = body.replace('\r\n', '\n')
    body = body.replace('encoding="UTF-16"', 'encoding="UTF-8"')
    out = body.encode('utf-8')
    path.write_bytes(out)
    print(f'{name}.xml: UTF-16LE(BOM)/CRLF -> UTF-8/LF  {len(raw)} -> {len(out)} B  '
          f'sha256 {before[:12]} -> {sha256(out)[:12]}')


def main() -> None:
    for name in UTF8_FILES:
        strip_utf8(name)
    strip_utf16(UTF16_FILE)
    # 负例：全部文件不得再有 DOCTYPE
    offenders = [p.name for p in RETAIL.glob('*.xml')
                 if b'<!DOCTYPE' in p.read_bytes() or '<!DOCTYPE'.encode('utf-16') in p.read_bytes()]
    if offenders:
        print(f'FAIL: DOCTYPE still present in {offenders}', file=sys.stderr)
        sys.exit(1)
    print('OK: no DOCTYPE remains under retail/')


if __name__ == '__main__':
    main()
