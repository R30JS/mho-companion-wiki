#!/usr/bin/env python3
"""Validate the bundled offline JSON assets for MHO Companion Wiki.

Run: python3 tools/validate_assets.py
"""
import json
import sys
from pathlib import Path

ASSETS = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets"

MODULES = {
    "version.json": "ModuleVersions",
    "codes.json": "CodesData",
    "builds.json": "BuildsData",
    "characters.json": "CharactersData",
    "monsters.json": "MonstersData",
    "guides.json": "GuidesData",
}

ELEMENTS = {"FIRE", "WATER", "THUNDER", "ICE", "DRAGON", "NONE"}
ELEMENTS_WITH_CONTENT = ELEMENTS - {"NONE"}
WEAPON_TYPES = {
    "HEAVY_BOWGUN", "LONG_SWORD", "DUAL_BLADES", "BOW",
    "GREAT_SWORD", "CHARGE_BLADE", "HAMMER", "LANCE",
}
ROLES = {"ASSAULT", "DISRUPTOR", "SUPPORT"}
ATTACK_TYPES = {"SEVER", "BLUNT", "RANGED"}

EXPECTED_COUNTS = {
    "builds": 40,      # 5 elements x 8 weapon types
    "adventurers": 14,
    "monsters": 7,
    "codes": 4,
    "guide sections": 6,
    "ailments": 8,
    "recipes": 6,
}

errors = []
warnings = []


def fail(msg):
    errors.append(msg)


def load(name):
    path = ASSETS / name
    if not path.exists():
        fail(f"missing asset {name}")
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        fail(f"{name}: invalid JSON ({exc})")
        return None


def require_keys(obj, keys, where):
    for key in keys:
        if key not in obj:
            fail(f"{where}: missing required key '{key}'")


def validate():
    version = load("version.json") or {}
    require_keys(version, ["remoteBaseUrl", "codes_v", "builds_v", "characters_v", "monsters_v", "guides_v"], "version.json")

    codes = load("codes.json") or {}
    builds = load("builds.json") or {}
    characters = load("characters.json") or {}
    monsters = load("monsters.json") or {}
    guides = load("guides.json") or {}

    # --- codes ---
    code_entries = codes.get("codes", [])
    seen_codes = set()
    for entry in code_entries:
        require_keys(entry, ["id", "code", "status"], f"codes/{entry.get('id')}")
        if entry["code"] in seen_codes:
            fail(f"duplicate code '{entry['code']}'")
        seen_codes.add(entry["code"])

    # --- builds ---
    element_entries = builds.get("elements", [])
    build_ids = set()
    expected_build_ids = set()
    for elem in element_entries:
        element_name = elem.get("element")
        if element_name not in ELEMENTS:
            fail(f"builds: unknown element '{element_name}'")
        if element_name == "NONE":
            continue
        for weapon in elem.get("weapons", []):
            require_keys(weapon, ["id", "weaponType", "name"], f"builds/{element_name}")
            wt = weapon.get("weaponType")
            if wt not in WEAPON_TYPES:
                fail(f"builds/{weapon.get('id')}: unknown weaponType '{wt}'")
            expected_id = f"{element_name.lower()}-{ {'HEAVY_BOWGUN':'hbg','LONG_SWORD':'ls','DUAL_BLADES':'db','BOW':'bow','GREAT_SWORD':'gs','CHARGE_BLADE':'cb','HAMMER':'hammer','LANCE':'lance'}[wt] }"
            expected_build_ids.add(expected_id)
            if weapon["id"] != expected_id:
                fail(f"builds/{weapon['id']}: id should be '{expected_id}'")
            if weapon["id"] in build_ids:
                fail(f"duplicate build id '{weapon['id']}'")
            build_ids.add(weapon["id"])
            at = {"HEAVY_BOWGUN": "RANGED", "LONG_SWORD": "SEVER", "DUAL_BLADES": "SEVER",
                  "BOW": "RANGED", "GREAT_SWORD": "SEVER", "CHARGE_BLADE": "SEVER",
                  "HAMMER": "BLUNT", "LANCE": "SEVER"}[wt]
            # AttackType consistency is implicit via weaponType model.

    if len(element_entries) != len(ELEMENTS_WITH_CONTENT):
        fail(f"builds: expected {len(ELEMENTS_WITH_CONTENT)} elements, found {len(element_entries)}")
    if len(build_ids) != EXPECTED_COUNTS["builds"]:
        fail(f"builds: expected {EXPECTED_COUNTS['builds']} weapons, found {len(build_ids)}")
    missing = expected_build_ids - build_ids
    if missing:
        fail(f"builds: missing expected ids {sorted(missing)}")

    # --- adventurers ---
    adventurers = characters.get("characters", [])
    seen_adv = set()
    for adv in adventurers:
        require_keys(adv, ["id", "name", "role", "element", "weaponType"], f"characters/{adv.get('id')}")
        if adv["element"] not in ELEMENTS:
            fail(f"characters/{adv['id']}: unknown element '{adv['element']}'")
        if adv["role"] not in ROLES:
            fail(f"characters/{adv['id']}: unknown role '{adv['role']}'")
        if adv["weaponType"] not in WEAPON_TYPES:
            fail(f"characters/{adv['id']}: unknown weaponType '{adv['weaponType']}'")
        if adv["id"] in seen_adv:
            fail(f"duplicate adventurer id '{adv['id']}'")
        seen_adv.add(adv["id"])
    if len(adventurers) != EXPECTED_COUNTS["adventurers"]:
        fail(f"characters: expected {EXPECTED_COUNTS['adventurers']} adventurers, found {len(adventurers)}")

    # --- monsters ---
    monster_entries = monsters.get("monsters", [])
    seen_mon = set()
    for mon in monster_entries:
        require_keys(mon, ["id", "name", "weakness"], f"monsters/{mon.get('id')}")
        wk = mon.get("weakness", {})
        for side in ("primary", "secondary"):
            if wk.get(side) not in ELEMENTS:
                fail(f"monsters/{mon['id']}: unknown weakness.{side} '{wk.get(side)}'")
        if mon["id"] in seen_mon:
            fail(f"duplicate monster id '{mon['id']}'")
        seen_mon.add(mon["id"])
        for zone in mon.get("hitzones", []):
            require_keys(zone, ["part", "sever", "blunt", "ranged"], f"monsters/{mon['id']} hitzone")
            for rating in ("sever", "blunt", "ranged"):
                if zone.get(rating, 0) not in (0, 1, 2, 3):
                    fail(f"monsters/{mon['id']} hitzone {zone.get('part')}: {rating}={zone.get(rating)} out of range")
    if len(monster_entries) != EXPECTED_COUNTS["monsters"]:
        fail(f"monsters: expected {EXPECTED_COUNTS['monsters']} monsters, found {len(monster_entries)}")

    # --- guides ---
    sections = guides.get("sections", [])
    if len(sections) != EXPECTED_COUNTS["guide sections"]:
        fail(f"guides: expected {EXPECTED_COUNTS['guide sections']} sections, found {len(sections)}")
    for section in sections:
        require_keys(section, ["id", "title"], f"guides/section/{section.get('id')}")
        for item in section.get("items", []):
            require_keys(item, ["heading", "body"], f"guides/section/{section.get('id')} item")

    ailments = guides.get("ailments", [])
    if len(ailments) != EXPECTED_COUNTS["ailments"]:
        fail(f"guides: expected {EXPECTED_COUNTS['ailments']} ailments, found {len(ailments)}")
    for ailment in ailments:
        require_keys(ailment, ["id", "name", "effect", "cures"], f"guides/ailment/{ailment.get('id')}")

    recipes = guides.get("emergencyRecipes", [])
    if len(recipes) != EXPECTED_COUNTS["recipes"]:
        fail(f"guides: expected {EXPECTED_COUNTS['recipes']} recipes, found {len(recipes)}")
    for recipe in recipes:
        require_keys(recipe, ["name", "effect"], f"guides/recipe/{recipe.get('name')}")

    # --- versions consistency ---
    if not version.get("remoteBaseUrl"):
        warnings.append("remoteBaseUrl is empty; remote sync is not configured (bundled data only)")

    if errors:
        print(f"VALIDATION FAILED ({len(errors)} error(s)):")
        for err in errors:
            print(f"  ! {err}")
        return False

    print("VALIDATION OK")
    print(f"  modules: {', '.join(MODULES)}")
    print(f"  builds: {len(build_ids)}, adventurers: {len(adventurers)}, monsters: {len(monster_entries)}")
    print(f"  codes: {len(code_entries)}, guide sections: {len(sections)}, ailments: {len(ailments)}, recipes: {len(recipes)}")
    if warnings:
        print("  warnings:")
        for w in warnings:
            print(f"    - {w}")
    return True


if __name__ == "__main__":
    sys.exit(0 if validate() else 1)
