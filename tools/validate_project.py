#!/usr/bin/env python3
from __future__ import annotations
import json
import re
import sys
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
errors: list[str] = []
notes: list[str] = []

# JSON syntax
json_files = sorted(RES.rglob("*.json")) + [RES / "pack.mcmeta"]
for path in json_files:
    relative = path.relative_to(RES).as_posix()
    if relative.startswith(('data/', 'assets/')) and not re.fullmatch(r'[a-z0-9_./-]+', relative):
        errors.append(f"Invalid Minecraft resource path: {relative}")
    try:
        json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"JSON parse failed: {path.relative_to(ROOT)}: {exc}")
notes.append(f"JSON checked: {len(json_files)} files")

# Archaeology recipes must carry a resolvable loot table onto the placed block.
for material in ('sand', 'gravel'):
    name = 'suspicious_' + material
    recipe = json.loads((RES / f'data/creationcore/recipe/{name}.json').read_text())
    data = recipe.get('result', {}).get('components', {}).get('minecraft:block_entity_data', {})
    expected = 'creationcore:archaeology/crafted_' + name
    if data.get('id') != 'minecraft:brushable_block' or data.get('LootTable') != expected:
        errors.append(f'{name}: missing archaeology block entity loot table')
    loot_file = RES / f'data/creationcore/loot_table/archaeology/crafted_{name}.json'
    if not loot_file.exists():
        errors.append(f'{name}: archaeology loot table is missing')
    else:
        loot = json.loads(loot_file.read_text())
        if loot.get('type') != 'minecraft:archaeology' or loot['pools'][0]['rolls'] != 1:
            errors.append(f'{name}: archaeology must select one loot table per brush')

info_keys = ('void_bucket', 'bottled_nothing', 'blank_matter', 'base_matter',
             'creative_matter', 'creative_core', 'creative_crafting_table', 'creative_crafter', 'mine_craft')
for lang in ('zh_cn', 'en_us'):
    translations = json.loads((RES / f'assets/creationcore/lang/{lang}.json').read_text())
    for key in info_keys:
        description = translations.get('jei.creationcore.info.' + key, '')
        if not description or '\\n' in description:
            errors.append(f'{lang}/{key}: missing description or literal newline escapes')

# No stale namespace after the deliberate rename creativecore -> creationcore.
text_suffixes = {".java", ".json", ".toml", ".gradle", ".properties", ".md", ".mcmeta", ".yml", ".yaml"}
for path in ROOT.rglob("*"):
    if path.is_file() and path.suffix in text_suffixes:
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if "creativecore" in text:
            errors.append(f"Stale creativecore namespace/name in {path.relative_to(ROOT)}")

# Texture dimensions from the PNG IHDR (stdlib only, so CI needs no Python packages).
textures = sorted((RES / "assets/creationcore/textures").rglob("*.png"))
for path in textures:
    try:
        raw = path.read_bytes()
        if raw[:8] != b"\x89PNG\r\n\x1a\n" or raw[12:16] != b"IHDR":
            raise ValueError("not a PNG with an IHDR header")
        width, height = struct.unpack(">II", raw[16:24])
        expected_size = (32, 32) if path == RES / "assets/creationcore/textures/block/base_matter.png" else (16, 16)
        if 'gui/container' in path.as_posix():
            expected_size = (176, 166)
        if (width, height) != expected_size:
            errors.append(f"Unexpected texture size: {path.relative_to(ROOT)} -> {(width, height)}, expected {expected_size}")
    except Exception as exc:
        errors.append(f"Texture failed to inspect: {path.relative_to(ROOT)}: {exc}")
notes.append(f"Textures checked: {len(textures)} files")

# Resolve model texture references belonging to creationcore.
model_files = sorted((RES / "assets/creationcore/models").rglob("*.json"))
for model in model_files:
    data = json.loads(model.read_text(encoding="utf-8"))
    for ref in (data.get("textures") or {}).values():
        if not isinstance(ref, str) or ref.startswith("#") or not ref.startswith("creationcore:"):
            continue
        rel = ref.split(":", 1)[1]
        tex = RES / "assets/creationcore/textures" / f"{rel}.png"
        if not tex.exists():
            errors.append(f"Missing texture referenced by {model.relative_to(ROOT)}: {ref}")

# Resolve blockstate model references belonging to creationcore.
for state_file in sorted((RES / "assets/creationcore/blockstates").glob("*.json")):
    data = json.loads(state_file.read_text(encoding="utf-8"))
    variants = data.get("variants", {})
    for value in variants.values():
        values = value if isinstance(value, list) else [value]
        for entry in values:
            ref = entry.get("model") if isinstance(entry, dict) else None
            if isinstance(ref, str) and ref.startswith("creationcore:"):
                rel = ref.split(":", 1)[1]
                target = RES / "assets/creationcore/models" / f"{rel}.json"
                if not target.exists():
                    errors.append(f"Missing model referenced by {state_file.relative_to(ROOT)}: {ref}")

# Expected core files / recipes / compatibility tag.
expected = [
    RES / "data/creationcore/recipe/blank_matter.json",
    RES / "data/creationcore/recipe/mine_craft.json",
    RES / "data/creationcore/recipe/creative_crafting_table.json",
    RES / "data/creationcore/recipe/creative_crafter.json",
    RES / "data/creationcore/recipe/void_bottling.json",
    RES / "data/creationcore/tags/item/creative_core_containers.json",
    RES / "data/creationcore/tags/block/mine_craft_drop_fallback_blacklist.json",
    RES / "data/minecraft/tags/block/mineable/pickaxe.json",
    RES / "assets/creationcore/models/item/mine_craft.json",
    RES / "assets/creationcore/textures/item/mine_craft.png",
    RES / "creationcore.mixins.json",
    RES / "logo.png",
    ROOT / ".github/workflows/build.yml",
]
for path in expected:
    if not path.exists():
        errors.append(f"Missing expected file: {path.relative_to(ROOT)}")

# Make sure all 17 vanilla shulker boxes are present in the compatibility tag.
tag_path = RES / "data/creationcore/tags/item/creative_core_containers.json"
if tag_path.exists():
    vals = json.loads(tag_path.read_text(encoding="utf-8")).get("values", [])
    if len(vals) != 17 or len(set(vals)) != 17:
        errors.append(f"creative_core_containers should contain 17 unique vanilla shulker boxes, found {len(vals)}")

# Recipe sanity assertions for the v0.2 confirmed specification.
blank = json.loads((RES / "data/creationcore/recipe/blank_matter.json").read_text(encoding="utf-8"))
if blank.get("result", {}).get("id") != "creationcore:blank_matter" or blank.get("type") != "minecraft:crafting_shaped":
    errors.append("Blank Matter is not an ordinary crafting recipe")

if (RES / "data/creationcore/recipe/cow_spawn_egg.json").exists():
    errors.append("Retired cow spawn egg placeholder recipe must be removed")

stonecutting = list((RES / "data/creationcore/recipe/stonecutting").glob("*.json"))
if len(stonecutting) != 139:
    errors.append(f"Expected 139 biome spawn egg stonecutting mappings, found {len(stonecutting)}")
if len(list((RES / "data/creationcore/recipe").rglob("*.json"))) != 264:
    errors.append("Expected updated base catalog and ten optional integration recipes")
for name, amount in [('void_bucket', 1000), ('bottled_nothing', 250)]:
    filling = json.loads((RES / f'data/creationcore/recipe/create/filling/{name}.json').read_text())
    if filling.get('type') != 'create:filling' or filling['ingredients'][1] != {
            'type': 'neoforge:single', 'fluid': 'creationcore:void', 'amount': amount}:
        errors.append(f'Incorrect void filling recipe: {name}')
    if filling.get('neoforge:conditions') != [{'type': 'neoforge:mod_loaded', 'modid': 'create'}]:
        errors.append(f'Create filling recipe lacks optional dependency condition: {name}')

recipe_root = RES / 'data/creationcore/recipe'
for removed in ['dirt_path', 'farmland', 'vault_copper', 'trial_spawner_copper']:
    if (recipe_root / (removed + '.json')).exists():
        errors.append(f'Removed recipe still exists: {removed}')
if list(recipe_root.glob('infested_*_potion.json')):
    errors.append('Infestation potion recipes must be removed')
egg_recipes = list(recipe_root.glob('infested_*_egg.json'))
if len(egg_recipes) != 7:
    errors.append('Expected seven silverfish-based infested block recipes')
for path in egg_recipes:
    recipe = json.loads(path.read_text())
    if recipe['key'][recipe['pattern'][1][1]].get('item') != 'minecraft:silverfish_spawn_egg' or recipe['result']['count'] != 8:
        errors.append(f'Incorrect infested block recipe: {path.name}')
for target in ['vault', 'trial_spawner']:
    recipe = json.loads((recipe_root / ('ominous_' + target + '.json')).read_text())
    components = recipe['result']['components']
    name = components.get('minecraft:item_name')
    if not isinstance(name, str):
        errors.append(f'{target}: 1.21.1 item_name must be a JSON text string')
    elif json.loads(name).get('translate') != 'item.creationcore.ominous_' + target:
        errors.append(f'{target}: incorrect ominous translation')
    if components.get('minecraft:block_state', {}).get('ominous') != 'true':
        errors.append(f'{target}: missing ominous block state')

mine_recipe = json.loads((RES / "data/creationcore/recipe/mine_craft.json").read_text())
if [x.get('item') for x in mine_recipe['ingredients']].count('creationcore:creative_core') != 1:
    errors.append('Mine Craft must require one Creative Core')
disc_recipes = list((RES / 'data/creationcore/recipe').glob('copy_music_disc_*.json'))
if len(disc_recipes) != 19:
    errors.append('Expected 19 independent vanilla disc copy recipes')
for path in disc_recipes:
    recipe = json.loads(path.read_text())
    if recipe.get('group') != 'creationcore:disc_copy' or recipe['result']['count'] != 1:
        errors.append(f'Disc copy must output one copy and return the original: {path.name}')
    middle = recipe['key'][recipe['pattern'][1][1]]['item']
    if middle != recipe['result']['id']:
        errors.append(f'Disc input/output mismatch: {path.name}')

smith = json.loads((RES / "data/creationcore/recipe/creative_crafting_table.json").read_text(encoding="utf-8"))
if not (smith.get("template", {}).get("item") == "creationcore:creative_core"
        and smith.get("base", {}).get("item") == "minecraft:crafting_table"
        and smith.get("addition", {}).get("item") == "minecraft:netherite_ingot"):
    errors.append("Creative Crafting Table smithing recipe does not match the agreed three slots")



# Creative Crafter must survive regeneration with the correct station and output.
crafter_recipe = json.loads((RES / "data/creationcore/recipe/creative_crafter.json").read_text())
if not (crafter_recipe.get("type") == "minecraft:smithing_transform"
        and crafter_recipe.get("base", {}).get("item") == "minecraft:crafter"
        and crafter_recipe.get("template", {}).get("item") == "creationcore:creative_core"
        and crafter_recipe.get("addition", {}).get("item") == "minecraft:netherite_ingot"
        and crafter_recipe.get("result", {}).get("id") == "creationcore:creative_crafter"):
    errors.append("Creative Crafter smithing recipe is incorrect")

# Mine Craft v0.1 build-fix-6 resource/data assertions.
mine_model = RES / "assets/creationcore/models/item/mine_craft.json"
if mine_model.exists():
    model_data = json.loads(mine_model.read_text(encoding="utf-8"))
    if model_data.get("textures", {}).get("0") != "creationcore:item/mine_craft":
        errors.append("Mine Craft 3D model is not using the expected texture")
    if len(model_data.get("elements", [])) != 18:
        errors.append("Mine Craft 3D model does not contain the expected 18 elements")
    required_display = {"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "ground", "gui", "head", "fixed"}
    if not required_display.issubset(set(model_data.get("display", {}))):
        errors.append("Mine Craft 3D model is missing Blockbench display transforms")
    if model_data.get("overrides"):
        errors.append("Mine Craft single-form model must not contain custom_model_data mode overrides")

blacklist_tag = RES / "data/creationcore/tags/block/mine_craft_drop_fallback_blacklist.json"
if blacklist_tag.exists():
    vals = set(json.loads(blacklist_tag.read_text(encoding="utf-8")).get("values", []))
    required_blacklist = {"minecraft:dragon_egg", "minecraft:nether_portal", "minecraft:end_portal", "minecraft:end_gateway"}
    missing = sorted(required_blacklist - vals)
    if missing:
        errors.append(f"Mine Craft fallback blacklist is missing: {missing}")

for tag_name in ("sword", "sharp_weapon", "mining", "vanishing"):
    tag = RES / f"data/minecraft/tags/item/enchantable/{tag_name}.json"
    if not tag.exists():
        errors.append(f"Missing Mine Craft enchantment compatibility tag: {tag.relative_to(ROOT)}")
        continue
    values = json.loads(tag.read_text(encoding="utf-8")).get("values", [])
    if "creationcore:mine_craft" not in values:
        errors.append(f"Mine Craft missing from enchantable/{tag_name}")
for forbidden_tag in ("mining_loot", "durability"):
    tag = RES / f"data/minecraft/tags/item/enchantable/{forbidden_tag}.json"
    if tag.exists() and "creationcore:mine_craft" in json.loads(tag.read_text(encoding="utf-8")).get("values", []):
        errors.append(f"Mine Craft must not be added to enchantable/{forbidden_tag}")

pickaxe_mineable = RES / "data/minecraft/tags/block/mineable/pickaxe.json"
if pickaxe_mineable.exists():
    values = json.loads(pickaxe_mineable.read_text(encoding="utf-8")).get("values", [])
    if "creationcore:base_matter" not in values:
        errors.append("Base Matter is missing from minecraft:mineable/pickaxe")

mixin_file = RES / "creationcore.mixins.json"
if mixin_file.exists():
    mix = json.loads(mixin_file.read_text(encoding="utf-8"))
    required_mixins = {"BlockStateBaseMixin", "LightBlockMixin", "ServerPlayerGameModeMixin"}
    if not required_mixins.issubset(set(mix.get("mixins", []))):
        errors.append("Mine Craft server/common mixins are incomplete")
    if "client.ClientLevelMixin" not in mix.get("client", []):
        errors.append("Mine Craft Barrier/Light client marker mixin is missing")

logo = RES / "logo.png"
if logo.exists():
    raw = logo.read_bytes()
    if raw[:8] != b"\x89PNG\r\n\x1a\n" or raw[12:16] != b"IHDR":
        errors.append("logo.png is not a valid PNG")
    else:
        width, height = struct.unpack(">II", raw[16:24])
        if (width, height) != (128, 128):
            errors.append(f"logo.png should be 128x128, got {(width, height)}")



# build-fix-9 Mine Craft transaction + Base Matter generic pickaxe assertions.
core_events = ROOT / "src/main/java/dev/creationcore/event/CoreGameplayEvents.java"
server_gamemode_mixin = ROOT / "src/main/java/dev/creationcore/mixin/ServerPlayerGameModeMixin.java"
base_matter_block = ROOT / "src/main/java/dev/creationcore/block/BaseMatterBlock.java"
if core_events.exists():
    text = core_events.read_text(encoding="utf-8")
    for stale in ("BedBlock", "DoorBlock", "DoublePlantBlock", "MULTIPART_DROP_GUARD"):
        if stale in text:
            errors.append(f"Mine Craft duplicate-drop logic still contains hard-coded multipart marker: {stale}")
    for required in ("ACTIVE_MINE_CRAFT_BREAK", "RECENT_MINE_CRAFT_BREAKS", "suppressMineCraftTransactionDrop",
                     "resolveMineCraftDrops", "MINE_CRAFT_DROP_FALLBACK_BLACKLIST",
                     "VaultBlock.OMINOUS", "TrialSpawnerBlock.OMINOUS", "SculkShriekerBlock.CAN_SUMMON"):
        if required not in text:
            errors.append(f"Mine Craft generic break transaction is missing: {required}")
    if "player.preventsBlockDrops()" in text:
        errors.append("Mine Craft transaction uses Player#preventsBlockDrops, unavailable in the 1.21.1 mappings used by this project")
if server_gamemode_mixin.exists():
    text = server_gamemode_mixin.read_text(encoding="utf-8")
    if "@WrapMethod(method = \"destroyBlock\")" not in text or "enterMineCraftDestroyBlockCall(player, level, pos)" not in text or "finishMineCraftDestroyBlockCall" not in text:
        errors.append("ServerPlayerGameModeMixin is missing the wrapped Mine Craft destroyBlock transaction")
if base_matter_block.exists():
    text = base_matter_block.read_text(encoding="utf-8")
    if "ItemAbilities.PICKAXE_DIG" not in text or "player.getDestroySpeed(state)" not in text:
        errors.append("Base Matter is not using generic PICKAXE_DIG + normal player destroy speed acceleration")


# build-fix-14 item/model/classification assertions.
biome_eggs = {
    "cave_biome_spawn_egg": "洞穴群系生成蛋",
    "arid_biome_spawn_egg": "干旱群系生成蛋",
    "ocean_biome_spawn_egg": "海洋群系生成蛋",
    "plains_biome_spawn_egg": "平原群系生成蛋",
    "forest_biome_spawn_egg": "森林群系生成蛋",
    "mountain_biome_spawn_egg": "山地群系生成蛋",
    "wetland_biome_spawn_egg": "湿地群系生成蛋",
    "nether_biome_spawn_egg": "下界群系生成蛋",
    "end_biome_spawn_egg": "末地群系生成蛋",
}
zh_lang = json.loads((RES / "assets/creationcore/lang/zh_cn.json").read_text(encoding="utf-8"))
for item_id, expected_name in biome_eggs.items():
    model = RES / f"assets/creationcore/models/item/{item_id}.json"
    texture = RES / f"assets/creationcore/textures/item/{item_id}.png"
    if not model.exists():
        errors.append(f"Missing biome spawn-egg item model: {item_id}")
    if not texture.exists():
        errors.append(f"Missing biome spawn-egg item texture: {item_id}")
    if zh_lang.get(f"item.creationcore.{item_id}") != expected_name:
        errors.append(f"Biome spawn-egg Chinese name mismatch: {item_id}")

mod_items = ROOT / "src/main/java/dev/creationcore/registry/ModItems.java"
if mod_items.exists():
    mod_items_text = mod_items.read_text(encoding="utf-8")
    for item_id in biome_eggs:
        if f'"{item_id}"' not in mod_items_text:
            errors.append(f"Biome spawn-egg item is not registered: {item_id}")

creative_table_axe = RES / "data/minecraft/tags/block/mineable/axe.json"
if not creative_table_axe.exists():
    errors.append("Missing minecraft:mineable/axe tag for Creative Crafting Table")
else:
    values = json.loads(creative_table_axe.read_text(encoding="utf-8")).get("values", [])
    if "creationcore:creative_crafting_table" not in values:
        errors.append("Creative Crafting Table is missing from minecraft:mineable/axe")

mod_blocks = ROOT / "src/main/java/dev/creationcore/registry/ModBlocks.java"
if mod_blocks.exists() and ".strength(2.5F, 12.0F)" not in mod_blocks.read_text(encoding="utf-8"):
    errors.append("Creative Crafting Table hardness is not 2.5")
if mod_blocks.exists():
    block_text = mod_blocks.read_text(encoding="utf-8")
    if ".pushReaction(PushReaction.BLOCK)" not in block_text or ".lightLevel(state -> 6)" not in block_text:
        errors.append("Base Matter piston/light settings are incomplete")
if base_matter_block.exists() and "canBeReplaced(BlockState state, Fluid fluid)" not in base_matter_block.read_text(encoding="utf-8"):
    errors.append("Base Matter does not resist flowing fluid replacement")

mine_item_java = ROOT / "src/main/java/dev/creationcore/item/MineCraftItem.java"
if mine_item_java.exists():
    mine_item_text = mine_item_java.read_text(encoding="utf-8")
    if "extends SwordItem" not in mine_item_text:
        errors.append("Mine Craft is not implemented as a SwordItem")
    if "new Tool(List.of(), 1.0F, 0)" not in mine_item_text or "damageItem(" not in mine_item_text:
        errors.append("Mine Craft sword conversion does not preserve zero durability consumption")
    if "ItemAbilities.SWORD_DIG" not in mine_item_text or "ItemAbilities.SWORD_SWEEP" not in mine_item_text:
        errors.append("Mine Craft is missing sword item abilities")
    if "BuiltInRegistries.ITEM" not in mine_item_text or "anyOtherItemAccelerates" not in mine_item_text:
        errors.append("Mine Craft is not using generic registered-item acceleration detection")
    if "MINE_CRAFT_PICKAXE_BONUS" in mine_item_text:
        errors.append("Mine Craft still contains the retired hard-coded pickaxe bonus tag")

blockstate_mixin = ROOT / "src/main/java/dev/creationcore/mixin/BlockStateBaseMixin.java"
if blockstate_mixin.exists():
    mixin_text = blockstate_mixin.read_text(encoding="utf-8")
    if "NORMALIZED_HARDNESS = 50.0F" not in mixin_text or "hardness >= 0.0F && hardness <= NORMALIZED_HARDNESS" not in mixin_text:
        errors.append("Mine Craft hardness normalization is not the requested outside-[0,50] => 50 rule")
    if "INDESTRUCTIBLE_PROGRESS_DIVISOR" in mixin_text:
        errors.append("Mine Craft still contains the retired hardness -1-only special case")

swords_tag = RES / "data/minecraft/tags/item/swords.json"
if not swords_tag.exists() or "creationcore:mine_craft" not in json.loads(swords_tag.read_text(encoding="utf-8")).get("values", []):
    errors.append("Mine Craft is missing from minecraft:swords")

base_model = RES / "assets/creationcore/models/block/base_matter.json"
if base_model.exists():
    base_model_data = json.loads(base_model.read_text(encoding="utf-8"))
    faces = base_model_data.get("elements", [{}])[0].get("faces", {}) if base_model_data.get("elements") else {}
    expected_uv = {
        "north": [0, 0, 4, 4],
        "east": [0, 4, 4, 8],
        "south": [4, 0, 8, 4],
        "west": [4, 4, 8, 8],
        "up": [4, 12, 0, 8],
        "down": [12, 0, 8, 4],
    }
    for face, uv in expected_uv.items():
        if faces.get(face, {}).get("uv") != uv:
            errors.append(f"Base Matter 32x32 Blockbench UV mismatch on {face}")

# Obvious TODO/FIXME markers are useful to surface rather than silently ship.
markers = []
for path in (ROOT / "src/main/java").rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    for n, line in enumerate(text.splitlines(), 1):
        if "TODO" in line or "FIXME" in line:
            markers.append(f"{path.relative_to(ROOT)}:{n}: {line.strip()}")
if markers:
    notes.append("TODO/FIXME markers:\n  " + "\n  ".join(markers))

print("Creation Core v0.2 static resource validation")
for note in notes:
    print("[INFO]", note)
if errors:
    print(f"[FAIL] {len(errors)} problem(s):")
    for e in errors:
        print(" -", e)
    sys.exit(1)
print("[PASS] Resource/static checks passed.")
