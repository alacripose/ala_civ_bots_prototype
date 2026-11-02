"""Wire place_block into the gate, registry, candidates, work and the ticket's close rule."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path, edits):
    file = ROOT / path
    text = file.read_text(encoding="utf-8")
    for index, (old, new) in enumerate(edits):
        count = text.count(old)
        if count != 1:
            raise SystemExit(f"{path}: anchor {index} occurs {count} times:\n{old[:200]}")
        text = text.replace(old, new, 1)
    file.write_text(text, encoding="utf-8")
    print(f"patched {path} ({len(edits)})")


# ------------------------------------------------------------------ gate
patch("src/main/java/dev/ensouled/core/Phase1GatePolicy.java", [
    (
        "\t/** Being up and about in the neighbourhood, chosen when the body is not tired. */\n"
        '\tpublic static final String SKILL_WANDER = "wander";',
        "\t/** Being up and about in the neighbourhood, chosen when the body is not tired. */\n"
        '\tpublic static final String SKILL_WANDER = "wander";\n'
        "\t/** Putting one block from the body's own inventory back into the world, inside home. */\n"
        '\tpublic static final String SKILL_PLACE = "place_block";',
    ),
    (
        '\t\treturn GateDecision.refuse("skill_not_permitted", "no pre-authorization for skill " + skill, hash);',
        '\t\tif (SKILL_PLACE.equals(skill)) {\n'
        "\t\t\tif (envelope.budgetCost() != 0) {\n"
        '\t\t\t\treturn GateDecision.refuse("bad_scope", "placing a block spends no harvest budget", hash);\n'
        "\t\t\t}\n"
        "\t\t\tif (view.blockAt(envelope.target()).isPresent()) {\n"
        '\t\t\t\treturn GateDecision.refuse("site_occupied", "a block already stands there", hash);\n'
        "\t\t\t}\n"
        "\t\t\tif (!view.canReach(envelope.target())) {\n"
        '\t\t\t\treturn GateDecision.refuse("not_reachable", "site is outside body reach", hash);\n'
        "\t\t\t}\n"
        "\t\t\tint siteDistance = (int) Math.round(homeAnchor.distanceTo(envelope.target()));\n"
        "\t\t\tif (siteDistance > homeRadius) {\n"
        '\t\t\t\treturn GateDecision.refuse("outside_home_radius", "site " + siteDistance + " blocks from home anchor", hash);\n'
        "\t\t\t}\n"
        "\t\t\treturn GateDecision.permit(hash);\n"
        "\t\t}\n"
        '\t\treturn GateDecision.refuse("skill_not_permitted", "no pre-authorization for skill " + skill, hash);',
    ),
])

# ------------------------------------------------------------------ registry
patch("src/main/java/dev/ensouled/core/SkillRegistry.java", [
    (
        "import dev.ensouled.skills.HarvestTreeRoutine;",
        "import dev.ensouled.skills.HarvestTreeRoutine;\nimport dev.ensouled.skills.PlaceBlockRoutine;",
    ),
    (
        "\t\tregistry.register(new SkillDefinition(\n"
        '\t\t\t\t\t\t"eat_if_hungry", 1,',
        "\t\tregistry.register(new SkillDefinition(\n"
        "\t\t\t\t\t\tPlaceBlockRoutine.ID, 1,\n"
        '\t\t\t\t\t\tList.of("holding_a_buildable_block", "site_in_reach", "site_empty"),\n'
        '\t\t\t\t\t\tList.of("block_placed", "inventory_delta_observed"),\n'
        '\t\t\t\t\t\tList.of("nothing_to_place", "no_site_in_reach", "site_not_reachable",\n'
        '\t\t\t\t\t\t\t\t"not_in_hotbar", "use_failed", "postcondition_failed",\n'
        '\t\t\t\t\t\t\t\t"no_inventory_delta", "path_invalid"),\n'
        '\t\t\t\t\t\t"build.place_one_block"),\n'
        "\t\t\t\tPlaceBlockRoutine::new);\n"
        "\t\tregistry.register(new SkillDefinition(\n"
        '\t\t\t\t\t\t"eat_if_hungry", 1,',
    ),
])

# ------------------------------------------------------------------ candidates
patch("src/main/java/dev/ensouled/core/CandidateGenerator.java", [
    (
        "\t\t// Being up and about is what a body does when it is not tired: the world is a place to walk",
        "\t\t// A build wish is served by putting material back, not by gathering more: the candidate is\n"
        "\t\t// the site, and the item is whatever the open ticket owes and the body actually carries.\n"
        "\t\tif (work(actor, Phase1GatePolicy.SKILL_PLACE) > 0) {\n"
        "\t\t\tOptional<Vec3i> site = dev.ensouled.skills.PlaceBlockRoutine.siteNear(view, body);\n"
        "\t\t\tOptional<String> placing = dev.ensouled.work.WorkLedger.itemForBuild(actor, view);\n"
        "\t\t\tif (site.isPresent() && placing.isPresent()) {\n"
        "\t\t\t\tout.add(new Candidate(\n"
        '\t\t\t\t\t\t"cand:build:" + site.get(),\n'
        "\t\t\t\t\t\tPhase1GatePolicy.SKILL_PLACE,\n"
        "\t\t\t\t\t\tsite.get(),\n"
        "\t\t\t\t\t\tplacing.get(),\n"
        "\t\t\t\t\t\t0,\n"
        "\t\t\t\t\t\twork(actor, Phase1GatePolicy.SKILL_PLACE),\n"
        "\t\t\t\t\t\t700,\n"
        "\t\t\t\t\t\tmind.ledger().stats(Phase1GatePolicy.SKILL_PLACE).habitStrength(),\n"
        "\t\t\t\t\t\t120,\n"
        "\t\t\t\t\t\t0,\n"
        "\t\t\t\t\t\t0,\n"
        "\t\t\t\t\t\t(int) Math.round(body.distanceTo(site.get())) * 40,\n"
        "\t\t\t\t\t\t0,\n"
        "\t\t\t\t\t\t1,\n"
        "\t\t\t\t\t\tnow));\n"
        "\t\t\t}\n"
        "\t\t}\n"
        "\t\t// Being up and about is what a body does when it is not tired: the world is a place to walk",
    ),
])

# ------------------------------------------------------------------ deliverable
patch("src/main/java/dev/ensouled/work/Deliverable.java", [
    (
        "\t/** The kind an utterance names, whether or not it says how many.",
        "\t/**\n"
        "\t * Whether the words ask for construction rather than gathering. A build wish spends stock\n"
        "\t * the being already carries, so it is not answered `already-held`: the count is what it must\n"
        "\t * put into the world, not what it must collect.\n"
        "\t */\n"
        "\tpublic static boolean builds(String text) {\n"
        "\t\tif (text == null) {\n"
        "\t\t\treturn false;\n"
        "\t\t}\n"
        "\t\tfor (String token : text.toLowerCase(Locale.ROOT).split(\"[^a-z0-9']+\")) {\n"
        "\t\t\tif (token.equals(\"build\") || token.equals(\"builds\") || token.equals(\"place\")\n"
        "\t\t\t\t\t|| token.equals(\"raise\") || token.equals(\"wall\")) {\n"
        "\t\t\t\treturn true;\n"
        "\t\t\t}\n"
        "\t\t}\n"
        "\t\treturn false;\n"
        "\t}\n\n"
        "\t/** The kind an utterance names, whether or not it says how many.",
    ),
])
print("ok")
