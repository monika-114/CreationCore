"""Generate the v0.2 recipe data from the confirmed table.

Keeps the 88 spawn-egg entries and their 139 stonecutting mappings consistent.
"""
from pathlib import Path
from collections import Counter
import json
import re
import argparse
import shutil
import tempfile
from docx import Document

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--check', action='store_true', help='Verify reproducibility without changing resources')
args = parser.parse_args()
stage = tempfile.TemporaryDirectory(prefix='creationcore-recipes-')
RECIPES = Path(stage.name) / 'recipe'
TAGS = Path(stage.name) / 'tags'
RECIPES.mkdir()
TAGS.mkdir()

def write(name, data):
    assert re.fullmatch(r'[a-z0-9_./-]+', name), name
    target = RECIPES / (name + '.json')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')

def item(id):
    if isinstance(id, (list, tuple)):
        return [item(i) for i in id]
    if isinstance(id, dict):
        return id
    if id.startswith('#'):
        return {'tag': id[1:]}
    return {'item': id if ':' in id else 'minecraft:' + id}

HIGH = {
    'minecraft:copper_horse_armor', 'minecraft:nautilus_spawn_egg',
    'minecraft:sulfur_cube_spawn_egg', 'minecraft:copper_golem_spawn_egg',
    'minecraft:camel_husk_spawn_egg', 'minecraft:parched_spawn_egg',
    'minecraft:zombie_nautilus_spawn_egg', 'minecraft:happy_ghast_spawn_egg',
    'minecraft:creaking_spawn_egg', 'minecraft:dried_ghast',
    'minecraft:copper_grate', 'minecraft:exposed_copper_grate',
    'minecraft:weathered_copper_grate', 'minecraft:oxidized_copper_grate',
    'minecraft:waxed_copper_grate', 'minecraft:waxed_exposed_copper_grate',
    'minecraft:waxed_weathered_copper_grate', 'minecraft:waxed_oxidized_copper_grate',
    'minecraft:closed_eyeblossom', 'minecraft:open_eyeblossom', 'minecraft:golden_dandelion',
}
# Copper grates and eyeblossoms are vanilla in later 1.21 releases, so each
# optional result/ingredient is guarded when a corresponding recipe is emitted.
def condition(data, ids):
    # Result checks also cover vanilla IDs that were introduced after 1.21.1,
    # including spawn eggs omitted from the document's asterisk notation.
    output = data.get('result', {})
    result_id = output.get('id') if isinstance(output, dict) else None
    ids = sorted(set(x for x in ids if x in HIGH) |
                 ({result_id} if isinstance(result_id, str) and result_id.startswith('minecraft:') else set()))
    if ids:
        data['neoforge:conditions'] = [{'type': 'neoforge:item_exists', 'item': x} for x in ids]
    return data

def ids_in(value):
    if isinstance(value, str):
        return {value if ':' in value else 'minecraft:'+value} if not value.startswith('#') else set()
    if isinstance(value, list):
        return set().union(*(ids_in(v) for v in value))
    if isinstance(value, dict):
        return set().union(*(ids_in(v) for v in value.values()))
    return set()

def result(out, count=1, components=None):
    obj = {'id': out if ':' in out else 'minecraft:' + out, 'count': count}
    if components:
        obj['components'] = components
    return obj

def shape(name, grid, out, count=1, exclusive=True, components=None, extra_conditions=(), group=None):
    assert len(grid) in (1,2,3)
    assert all(len(row) == len(grid[0]) for row in grid)
    symbols = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    distinct = list({repr(c):c for row in grid for c in row if c is not None}.values())
    assert len(distinct) <= len(symbols)
    syms = {repr(c): symbols[n] for n,c in enumerate(distinct)}
    pat = [''.join(' ' if c is None else syms[repr(c)] for c in row) for row in grid]
    key = {syms[repr(c)]: item(c) for c in distinct}
    data = {'type': 'creationcore:creative_crafting' if exclusive else 'minecraft:crafting_shaped',
            'category': 'misc', 'pattern': pat, 'key': key, 'result': result(out,count,components)}
    if group:
        data['group'] = group
    write(name, condition(data, ids_in(grid)|ids_in(out)|set(extra_conditions)))

def shapeless(name, inputs, out, count=1, exclusive=True, components=None, extra_conditions=()):
    data={'type':'creationcore:creative_crafting_shapeless' if exclusive else 'minecraft:crafting_shapeless',
          'category':'misc', 'ingredients':[item(x) for x in inputs],
          'result':result(out,count,components)}
    write(name,condition(data,ids_in(inputs)|ids_in(out)|set(extra_conditions)))

def full(center, edge):
    return [[edge,edge,edge],[edge,center,edge],[edge,edge,edge]]

def one(id):
    return 'creationcore:'+id

def potion(potion_id):
    return {'type':'neoforge:components', 'items':'minecraft:potion',
            'components':{'minecraft:potion_contents':{'potion':'minecraft:'+potion_id}},
            'strict':False}

BIOMES = {
    '洞穴':'cave','干旱':'arid','海洋':'ocean','平原':'plains','森林':'forest',
    '山地':'mountain','湿地':'wetland','下界':'nether','末地':'end'
}
def biome(id):
    return one(BIOMES[id]+'_biome_spawn_egg')

COPPER_BLOCKS = [f'minecraft:{prefix}copper_block' for prefix in ('','exposed_','weathered_','oxidized_','waxed_','waxed_exposed_','waxed_weathered_','waxed_oxidized_')]
# Unoxidized is named copper_block; oxidized variants omit "_block" in the registry.
COPPER_BLOCKS = ['minecraft:copper_block','minecraft:exposed_copper','minecraft:weathered_copper','minecraft:oxidized_copper',
                 'minecraft:waxed_copper_block','minecraft:waxed_exposed_copper','minecraft:waxed_weathered_copper','minecraft:waxed_oxidized_copper']
COPPER_GRATES = ['minecraft:copper_grate','minecraft:exposed_copper_grate','minecraft:weathered_copper_grate','minecraft:oxidized_copper_grate',
                 'minecraft:waxed_copper_grate','minecraft:waxed_exposed_copper_grate','minecraft:waxed_weathered_copper_grate','minecraft:waxed_oxidized_copper_grate']
SOILS=['dirt','coarse_dirt','grass_block','podzol','mycelium','rooted_dirt']

# Mod progression and utility items.
shape('blank_matter', [
    ['tinted_glass','tinted_glass','tinted_glass'],
    ['stick',one('bottled_nothing'),'stick'],
    ['tinted_glass','tinted_glass','tinted_glass']],one('blank_matter'),exclusive=False)
shapeless('mine_craft',['netherite_pickaxe','netherite_sword','netherite_axe','netherite_shovel',one('creative_core')],one('mine_craft'))
write('creative_crafting_table',{
    'type':'minecraft:smithing_transform',
    'template':item(one('creative_core')),
    'base':item('crafting_table'),
    'addition':item('netherite_ingot'),
    'result':result(one('creative_crafting_table'))
})
write('creative_crafter', {
    'type': 'minecraft:smithing_transform',
    'template': item(one('creative_core')),
    'base': item('crafter'),
    'addition': item('netherite_ingot'),
    'result': result(one('creative_crafter'))
})
shape('void_bottling',[
    [None,'glass',None],['glass',one('void_bucket'),'glass'],[None,'glass',None]],
    one('bottled_nothing'),4,exclusive=False)

BN=one('bottled_nothing')
for base in ('sand', 'gravel'):
    shape('suspicious_'+base,[[None,base,None],[base,'decorated_pot',base],[None,base,None]],'suspicious_'+base,4,
          components={'minecraft:block_entity_data':{'id':'minecraft:brushable_block',
                      'LootTable':'creationcore:archaeology/crafted_suspicious_'+base}})
shape('barrier',full(one('blank_matter'),'bedrock'),'barrier',8)
shape('light_level_1',full(one('blank_matter'),'torch'),'light',8,components={'minecraft:block_state':{'level':'1'}})
shapeless('debug_stick',['stick','enchanted_book'],'debug_stick')
shape('reinforced_deepslate',[['deepslate']*3]*3,'reinforced_deepslate')
shape('budding_amethyst',[[None,'amethyst_cluster',None],['amethyst_cluster','amethyst_block','amethyst_cluster'],[None,'amethyst_cluster',None]],'budding_amethyst')
shape('end_portal_frame',[['end_stone',None,'end_stone'],['ender_eye','bedrock','ender_eye'],['end_stone']*3],'end_portal_frame')
for target, base in [
 ('infested_stone','stone'),('infested_cobblestone','cobblestone'),
 ('infested_stone_bricks','stone_bricks'),('infested_mossy_stone_bricks','mossy_stone_bricks'),
 ('infested_cracked_stone_bricks','cracked_stone_bricks'),
 ('infested_chiseled_stone_bricks','chiseled_stone_bricks'),('infested_deepslate','deepslate')]:
    shape(target+'_egg',full('silverfish_spawn_egg',base),target,8)
shape('spawner',full('nether_star','iron_bars'),'spawner')
shape('tall_grass',[[ 'bone_meal'],['short_grass']],'tall_grass')
shape('large_fern',[[ 'bone_meal'],['fern']],'large_fern')
for target,center in [('trial_spawner','nether_star'),('vault','enchanted_golden_apple')]:
    shape(target+'_grate',full(center,COPPER_GRATES),target,extra_conditions=('minecraft:copper_grate',))
    shapeless('ominous_'+target,['ominous_bottle',target],target,
              components={'minecraft:block_state':{'ominous':'true'},
                          'minecraft:item_name':json.dumps({'translate':'item.creationcore.ominous_'+target})})

# Nine biome spawn eggs. Alternates in each slot are intentionally independent.
for id, surround in {
    'cave':'deepslate','arid':['sand','red_sand'],'ocean':'prismarine',
    'plains':'grass_block','forest':'#minecraft:logs','wetland':'mud',
    'nether':'netherrack','end':'end_stone',
}.items():
    shape(id+'_biome_spawn_egg',full('egg',surround),one(id+'_biome_spawn_egg'))
shape('mountain_biome_spawn_egg',[['snow_block']*3,['stone','egg','stone'],['stone']*3],biome('山地'))

# Every stonecutter mapping in the uploaded 88-entry table.
doc=Document(ROOT/'docs/recipe_spec.docx')
egg_table=next(t for t in doc.tables if len(t.rows)==89)
map_counts=Counter()
special=[]
for row in egg_table.rows[1:]:
    label, description = (c.text.strip() for c in row.cells)
    match=re.search(r'minecraft:([a-z0-9_]+)',label)
    assert match, label
    output='minecraft:'+match.group(1)
    if description.startswith('切石材料：'):
        for source in re.search(r'切石材料：(.*?)；\s*1\s*→\s*1',description).group(1).split('，'):
            source=source.strip()
            assert source in BIOMES,(label,source)
            recipe={'type':'minecraft:stonecutting','ingredient':item(biome(source)),
                    'result':{'id':output,'count':1}}
            write('stonecutting/'+BIOMES[source]+'_'+match.group(1),condition(recipe,[output]))
            map_counts[source]+=1
    else:
        special.append((output,description))
assert len(egg_table.rows)==89

# Spawn eggs that use bespoke crafting rather than biome stonecutting.
shapeless('mule_spawn_egg',['donkey_spawn_egg','horse_spawn_egg'],'mule_spawn_egg',2)
shapeless('tadpole_spawn_egg',['tadpole_bucket'],'tadpole_spawn_egg')
shapeless('turtle_spawn_egg',['turtle_egg'],'turtle_spawn_egg')
shapeless('mooshroom_spawn_egg',['cow_spawn_egg','brown_mushroom','red_mushroom'],'mooshroom_spawn_egg')
shapeless('sniffer_spawn_egg',['sniffer_egg'],'sniffer_spawn_egg')
PUMPKINS=['minecraft:pumpkin','minecraft:jack_o_lantern']
shape('copper_golem_spawn_egg',[[PUMPKINS],[COPPER_BLOCKS]],'copper_golem_spawn_egg')
shape('iron_golem_spawn_egg',[[PUMPKINS]*3,['iron_block']*3,[None,'iron_block',None]],'iron_golem_spawn_egg')
shape('snow_golem_spawn_egg',[[PUMPKINS],['snow_block'],['snow_block']],'snow_golem_spawn_egg')
shape('trader_llama_spawn_egg',[['blue_carpet',None],['llama_spawn_egg','lead']],'trader_llama_spawn_egg')
shapeless('wandering_trader_spawn_egg',['villager_spawn_egg',potion('invisibility')],'wandering_trader_spawn_egg')
shapeless('bogged_spawn_egg',['red_mushroom','skeleton_spawn_egg','brown_mushroom'],'bogged_spawn_egg')
for name,base,edge in [
    ('camel_husk_spawn_egg','camel_spawn_egg','rotten_flesh'),
    ('drowned_spawn_egg','zombie_spawn_egg','kelp'),
    ('husk_spawn_egg','zombie_spawn_egg','sand'),
    ('parched_spawn_egg','skeleton_spawn_egg','sand'),
    ('stray_spawn_egg','skeleton_spawn_egg','snow_block'),
    ('zombie_horse_spawn_egg','horse_spawn_egg','rotten_flesh'),
    ('zombie_nautilus_spawn_egg','nautilus_spawn_egg','rotten_flesh'),
    ('zombie_villager_spawn_egg','villager_spawn_egg','rotten_flesh'),
    ('zoglin_spawn_egg','hoglin_spawn_egg','rotten_flesh')]:
    shape(name,full(base,edge),name)
shape('skeleton_horse_spawn_egg',[
    ['bone','lightning_rod','bone'],
    ['bone','horse_spawn_egg','bone'],
    ['bone','bone','bone']],'skeleton_horse_spawn_egg')
shape('wither_spawn_egg',[
    ['wither_skeleton_skull']*3,
    [['soul_sand','soul_soil']]*3,
    [None,['soul_sand','soul_soil'],None]],'wither_spawn_egg')
shapeless('creaking_spawn_egg',['creaking_heart'],'creaking_spawn_egg')
shape('elder_guardian_spawn_egg',full('guardian_spawn_egg','gold_block'),'elder_guardian_spawn_egg')
shape('warden_spawn_egg',[
    ['sculk']*3,['sculk',biome('洞穴'),'sculk'],['sculk','sculk_catalyst','sculk']],'warden_spawn_egg')
shapeless('happy_ghast_spawn_egg',['dried_ghast'],'happy_ghast_spawn_egg')
shapeless('piglin_brute_spawn_egg',['piglin_spawn_egg','golden_axe'],'piglin_brute_spawn_egg')
shape('ender_dragon_spawn_egg',[
    ['end_crystal','dragon_breath','end_crystal'],
    ['elytra',biome('末地'),'dragon_head'],
    ['end_crystal','dragon_breath','end_crystal']],'ender_dragon_spawn_egg')
assert len(special)==28, (len(special), special)

# Standard crafting additions.
shape('calcite',[['dripstone_block','tuff'],['tuff','dripstone_block']],'calcite',2,exclusive=False)
for material,output in [('iron_ingot','iron_horse_armor'),('gold_ingot','golden_horse_armor'),
                        ('diamond','diamond_horse_armor'),('copper_ingot','copper_horse_armor')]:
    shape(output,[[material,None,material],[material]*3,[material,None,material]],output,exclusive=False)
shape('soul_soil',[['soul_sand','dirt'],['dirt','soul_sand']],'soul_soil',2,exclusive=False)
shape('dead_bush',[['stick',None,None],['stick']*3,[None,'stick',None]],'dead_bush',exclusive=False)
shape('spore_blossom',[['moss_block']*3,[None,'#creationcore:recipe_flowers',None]],'spore_blossom',exclusive=False)
flowers=['dandelion','poppy','blue_orchid','allium','azure_bluet','red_tulip','orange_tulip','white_tulip','pink_tulip',
         'oxeye_daisy','cornflower','lily_of_the_valley','wither_rose','torchflower','sunflower','lilac','rose_bush','peony','pitcher_plant']
optional=['closed_eyeblossom','open_eyeblossom','golden_dandelion']
(TAGS/'recipe_flowers.json').write_text(json.dumps({'replace':False,
  'values':['minecraft:'+x for x in flowers]+[{'id':'minecraft:'+x,'required':False} for x in optional]},
  ensure_ascii=False,indent=2)+'\n')
shape('cobweb',[['string']*3]*3,'cobweb',exclusive=False)

# Fixed output per disc makes the copied disc identical in type to the input.
discs=['13','cat','blocks','chirp','far','mall','mellohi','stal','strad','ward','11','wait',
       'pigstep','otherside','5','relic','precipice','creator','creator_music_box']
for disc in discs:
    id='minecraft:music_disc_'+disc
    shape('copy_music_disc_'+disc,[
        ['iron_ingot',one('blank_matter'),'iron_ingot'],
        ['iron_ingot',id,'iron_ingot'],
        ['iron_ingot']*3],id,1,group='creationcore:disc_copy')
shape('elytra',[
    ['phantom_membrane']*3,
    ['phantom_membrane',one('creative_matter'),'phantom_membrane'],
    ['phantom_membrane',None,'phantom_membrane']],'elytra')
HEADS=['skeleton_skull','wither_skeleton_skull','zombie_head','player_head','creeper_head','piglin_head']
shape('dragon_head',[[HEADS,one('creative_matter')]],'dragon_head')
shape('dragon_egg',[
    ['crying_obsidian']*3,
    ['crying_obsidian','sniffer_egg','crying_obsidian'],
    ['crying_obsidian',one('creative_matter'),'crying_obsidian']],'dragon_egg')
shapeless('gilded_blackstone',['gold_nugget']*3+['blackstone']*6,'gilded_blackstone')
shape('heart_of_the_sea',full(one('base_matter'),'prismarine'),'heart_of_the_sea')
shape('enchanted_golden_apple',full('apple','gold_block'),'enchanted_golden_apple')

# Mineable ore recipes, eight arbitrary-position ingredients plus the correct output.
ore_table=next(t for t in doc.tables if len(t.rows)==18)
MATERIAL={'铁锭':'iron_ingot','煤炭':'coal','金锭':'gold_ingot','钻石':'diamond',
          '红石':'redstone','青金石':'lapis_lazuli','铜锭':'copper_ingot',
          '石头':'stone','深板岩':'deepslate','金粒':'gold_nugget',
          '下界岩':'netherrack','石英':'quartz','基底物质':one('base_matter')}
ORE_IDS={'铁矿石':'iron_ore','煤矿石':'coal_ore','金矿石':'gold_ore','钻石矿石':'diamond_ore',
 '红石矿石':'redstone_ore','青金石矿石':'lapis_ore','铜矿石':'copper_ore',
 '深层铁矿石':'deepslate_iron_ore','深层煤矿石':'deepslate_coal_ore',
 '深层金矿石':'deepslate_gold_ore','深层钻石矿石':'deepslate_diamond_ore',
 '深层红石矿石':'deepslate_redstone_ore','深层青金石矿石':'deepslate_lapis_ore',
 '深层铜矿石':'deepslate_copper_ore','下界金矿石':'nether_gold_ore',
 '下界石英矿石':'nether_quartz_ore'}
for row in ore_table.rows[1:]:
    label,description=(c.text.strip() for c in row.cells)
    raw=description.split('→')[0].replace('；','，').strip('， ')
    ingredients=[MATERIAL[x.strip()] for x in raw.split('，') if x.strip()]
    assert len(ingredients)==9,(label,ingredients)
    if label=='远古残骸':
        shape('ancient_debris',[ingredients[:3],ingredients[3:6],ingredients[6:]],'ancient_debris')
    else:
        shapeless(ORE_IDS[label],ingredients,ORE_IDS[label])

# Optional integrations are kept in the same reproducible catalog.
def optional_shape(name, grid, output, mod, mechanical=False):
    symbols = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    distinct = list(dict.fromkeys(c for row in grid for c in row if c is not None))
    keys = {c: symbols[i] for i,c in enumerate(distinct)}
    data = {'type': 'create:mechanical_crafting' if mechanical else 'creationcore:creative_crafting',
            'pattern': [''.join(keys.get(c, ' ') for c in row) for row in grid],
            'key': {keys[c]: item(c) for c in distinct}, 'result': result(output),
            'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': mod},
                                   {'type': 'neoforge:item_exists', 'item': output}]}
    if mechanical:
        data['accept_mirrored'] = True
        data['show_notification'] = False
    write(name, data)

for target, owner in [('backpack', 'sophisticatedbackpacks'), ('storage', 'sophisticatedstorage')]:
    optional_shape('compat/' + target + '_creative_crafting_upgrade',
                   [['string', owner + ':crafting_upgrade', 'string'],
                    [None, one('creative_core'), None], ['string', None, 'string']],
                   one(target + '_creative_crafting_upgrade'), owner)
optional_shape('compat/ae2/creative_energy_cell', full(one('creative_core'), 'ae2:dense_energy_cell'),
               'ae2:creative_energy_cell', 'ae2')
write('compat/ae2/creative_item_cell', {
    'type': 'creationcore:creative_crafting_shapeless',
    'ingredients': [item(one('creative_core')), item('ae2:item_cell_housing')],
    'result': result('ae2:creative_storage_cell'),
    'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'ae2'},
                           {'type': 'neoforge:item_exists', 'item': 'ae2:creative_storage_cell'}]})
C = one('creative_core')
optional_shape('compat/create/handheld_worldshaper',
               [[None,C,None,None,None,None], [C,'create:extendo_grip',C,C,C,C],
                [None,C,'create:brass_hand',None,None,None]], 'create:handheld_worldshaper', 'create', True)
optional_shape('compat/create/creative_motor',
               [['create:brass_casing']*3, ['create:hand_crank', C, 'create:shaft'],
                ['create:brass_casing']*3], 'create:creative_motor', 'create', True)
optional_shape('compat/create/creative_fluid_tank', [['create:copper_sheet'],[C],['create:copper_sheet']],
               'create:creative_fluid_tank', 'create', True)
optional_shape('compat/create/creative_blaze_cake', [[C],['create:blaze_cake_base']],
               'create:creative_blaze_cake', 'create', True)

for name, container, amount in [('void_bucket', 'bucket', 1000), ('bottled_nothing', 'glass_bottle', 250)]:
    write('create/filling/' + name, {
        'type': 'create:filling',
        'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'create'}],
        'ingredients': [{'item': 'minecraft:' + container},
                        {'type': 'neoforge:single', 'fluid': 'creationcore:void', 'amount': amount}],
        'results': [{'id': 'creationcore:' + name}],
    })

print('recipes:',len(list(RECIPES.rglob('*.json'))),'stonecutting mappings:',sum(map_counts.values()),
      'biomes:',dict(map_counts),'special eggs:',len(special))

actual_recipes = ROOT / 'src/main/resources/data/creationcore/recipe'
actual_tags = ROOT / 'src/main/resources/data/creationcore/tags/item'
if args.check:
    expected = {p.relative_to(RECIPES): json.loads(p.read_text()) for p in RECIPES.rglob('*.json')}
    actual = {p.relative_to(actual_recipes): json.loads(p.read_text()) for p in actual_recipes.rglob('*.json')}
    changed = sorted(str(p) for p in expected.keys() | actual.keys() if expected.get(p) != actual.get(p))
    for p in TAGS.glob('*.json'):
        target = actual_tags / p.name
        if not target.exists() or json.loads(p.read_text()) != json.loads(target.read_text()):
            changed.append('tags/' + p.name)
    if changed:
        raise SystemExit('Recipe regeneration differs: ' + ', '.join(changed))
    print('PASS: generated recipes/tags exactly match the published catalog; no files changed.')
else:
    # Generate and validate the entire new catalog before replacing any existing recipe files.
    if actual_recipes.exists():
        shutil.rmtree(actual_recipes)
    shutil.copytree(RECIPES, actual_recipes)
    actual_tags.mkdir(parents=True, exist_ok=True)
    for p in TAGS.glob('*.json'):
        shutil.copy2(p, actual_tags / p.name)
stage.cleanup()
