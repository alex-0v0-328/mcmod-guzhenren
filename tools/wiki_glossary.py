"""Chinese-English glossary page of the wiki, generated from both mods' zh_cn and en_us lang files.

Writes or checks <wiki>/中英对照表.md: one `中文 | English | key` table per section of SECTIONS, in that order, with no
opening text: the page starts at its first heading.
A selector is a lang key (taken alone) or a key ending in `.` (every key under it); entries with a blank side or
a `%` placeholder are sentences or templates, not terms, and are left out. Inside one selector the keys follow
ORDER when their last segment is listed there, then key order.

Everything from the `## 设定词` heading on is hand-written and kept as it is; the script owns the text above it.

    python3 -B tools/wiki_glossary.py            check: exit 1 and print the stale lines when the page is out of date
    python3 -B tools/wiki_glossary.py --write    regenerate the generated part
"""
import argparse
import difflib
import json
import sys
from pathlib import Path

import project_runtime as env


PAGE = '中英对照表.md'
MANUAL_HEADING = '## 设定词'
LANG_DIR = 'src/generated/resources/assets/{mod}/lang'
MANUAL_STUB = (f'{MANUAL_HEADING}\n\n语言文件里没有的设定词写在这里，脚本不改本节。'
               '炼蛊的两张设定词表见 [[炼蛊与蛊方]]。\n')

SECTIONS = (
    ('修炼', 'guzhenren', (
        'guzhenren.enum.aperture.title.', 'guzhenren.enum.aperture.rank.', 'guzhenren.enum.aperture.stage.',
        'guzhenren.enum.aperture.talent.', 'guzhenren.display.talent_grade.', 'guzhenren.enum.aperture.status.',
        'guzhenren.display.aperture_1', 'guzhenren.display.aperture_2')),
    ('流派', 'guzhenren', (
        'guzhenren.enum.path.path.', 'guzhenren.enum.path.attainment.', 'guzhenren.enum.path.carrier.',
        'guzhenren.enum.path.effect_category.', 'guzhenren.enum.strength.strength_path_branch.')),
    ('体质与种族', 'guzhenren', (
        'guzhenren.enum.body.physique.', 'guzhenren.enum.body.extreme_physique.', 'guzhenren.enum.body.race.',
        'guzhenren.enum.body.skin.', 'guzhenren.enum.body.tendon.', 'guzhenren.enum.body.bone.',
        'guzhenren.enum.body.flesh.', 'guzhenren.enum.body.shield.')),
    ('魂魄与念头', 'guzhenren', (
        'guzhenren.enum.soul.tier.', 'guzhenren.enum.wisdom.type.', 'guzhenren.enum.wisdom.tag.',
        'guzhenren.enum.wisdom.brilliance.')),
    ('力道', 'guzhenren', (
        'guzhenren.enum.strength.beast_family.', 'guzhenren.enum.strength.beast_strength.',
        'guzhenren.enum.strength.human_strength.')),
    ('蛊', 'guzhenren', (
        'guzhenren.display.gu', 'guzhenren.display.gu_material', 'guzhenren.menu.vital',
        'guzhenren.enum.gu.gu_type.', 'guzhenren.killing_move.km_mortal_little_light_amplify')),
    ('蛊虫与蛊材', 'guzhenren', ('item.guzhenren.', 'block.guzhenren.')),
    ('效果', 'guzhenren', ('effect.guzhenren.', 'guzhenren.path_effect.')),
    ('界面', 'guzhenren', (
        'guzhenren.screen.tab.', 'guzhenren.screen.label.', 'guzhenren.screen.button.', 'guzhenren.screen.info.',
        'guzhenren.screen.impact', 'guzhenren.screen.nourish', 'guzhenren.screen.nourish_second',
        'guzhenren.screen.nourish_stop', 'guzhenren.screen.pick.', 'guzhenren.menu.aperture_storage',
        'guzhenren.menu.refinement', 'guzhenren.menu.refinement.recipes', 'guzhenren.menu.refinement.craft',
        'itemGroup.guzhenren.', 'key.')),
    ('世界', 'guworld', ('dimension.guworld.',)),
)

ORDER = (
    'none', 'mortal', 'gu_master', 'gu_immortal',
    'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine',
    'init', 'middle', 'upper', 'peak',
    'ten_extremes', 'first', 'second', 'third', 'fourth',
    'ten', 'hundred', 'thousand', 'ten_thousand', 'hundred_thousand', 'million', 'ten_million', 'hundred_million',
    'ordinary', 'quasi_master', 'master', 'quasi_grandmaster', 'grandmaster', 'quasi_great_grandmaster',
    'great_grandmaster', 'quasi_supreme_grandmaster', 'supreme_grandmaster',
    'decent', 'distinctive', 'outstanding', 'unrivaled',
    'jin', 'ten_jin', 'jun', 'ten_jun', 'hundred_jun',
    'normal', 'dead',
)
RANK = {word: index for index, word in enumerate(ORDER)}


def load_lang(root, mod):
    folder = Path(root) / LANG_DIR.format(mod=mod)
    return tuple(json.loads((folder / f'{name}.json').read_text(encoding='utf-8')) for name in ('zh_cn', 'en_us'))


def select(selector, zh, en):
    keys = [k for k in zh if k.startswith(selector)] if selector.endswith('.') else [k for k in zh if k == selector]
    if not keys:
        raise SystemExit(f'wiki_glossary: selector {selector} matches no lang key')
    keys.sort(key=lambda k: (RANK.get(k.rsplit('.', 1)[-1], len(ORDER)), k))
    rows = []
    for key in keys:
        chinese, english = zh[key].strip(), en.get(key, '').strip()
        if chinese and english and '%' not in chinese and '%' not in english:
            rows.append((chinese, english, key))
    return rows


def render(langs):
    """The generated part of the page: one table per section, ending in a blank line."""
    lines = []
    seen = set()
    for title, mod, selectors in SECTIONS:
        zh, en = langs[mod]
        lines += [f'## {title}', '', '| 中文 | English | key |', '|---|---|---|']
        for selector in selectors:
            for chinese, english, key in select(selector, zh, en):
                if (mod, key) in seen:
                    raise SystemExit(f'wiki_glossary: {key} is selected twice')
                seen.add((mod, key))
                lines.append(f'| {chinese} | {english} | `{key}` |')
        lines.append('')
    return '\n'.join(lines) + '\n'


def split(text):
    """(generated part, hand-written part); the hand-written part starts at MANUAL_HEADING."""
    marker = f'\n{MANUAL_HEADING}\n'
    at = text.find(marker)
    if at < 0:
        return text, ''
    return text[:at + 1], text[at + 1:]


def main(argv=None, roots=None, wiki=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--write', action='store_true', help='regenerate the page instead of checking it')
    args = parser.parse_args(argv)
    roots = roots or {'guzhenren': env.REPOSITORY_ROOT, 'guworld': env.GU_WORLD_ROOT}
    page = Path(wiki or env.WIKI_ROOT) / PAGE
    generated = render({mod: load_lang(root, mod) for mod, root in roots.items()})
    current = page.read_text(encoding='utf-8') if page.is_file() else ''
    head, manual = split(current)
    if args.write:
        page.write_text(generated + (manual or MANUAL_STUB), encoding='utf-8')
        print(f'wiki_glossary: wrote {page}')
        return 0
    if head == generated:
        print(f'wiki_glossary: {page} is up to date')
        return 0
    diff = difflib.unified_diff(head.splitlines(), generated.splitlines(), 'page', 'lang files', lineterm='', n=0)
    print('\n'.join(diff), file=sys.stderr)
    print(f'wiki_glossary: {page} is out of date; run tools/wiki_glossary.py --write', file=sys.stderr)
    return 1


if __name__ == '__main__':
    sys.exit(main())
