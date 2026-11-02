"""The civilian docs' rule (GRILLING-C11 Q572): skill is a view over completed work, not a stored
scalar. This patch makes the *verb* the learned unit and the practice log the record."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path, edits):
    file = ROOT / path
    text = file.read_text(encoding="utf-8")
    for index, (old, new) in enumerate(edits):
        count = text.count(old)
        if count != 1:
            raise SystemExit(f"{path}: anchor {index} occurs {count} times:\n{old[:160]}")
        text = text.replace(old, new, 1)
    file.write_text(text, encoding="utf-8")
    print(f"patched {path} ({len(edits)})")


# Vec3i has above() but no below(): a site's support is one y down.
patch("src/main/java/dev/ensouled/skills/PlaceBlockRoutine.java", [
    ("\t\tVec3i support = site.below();",
     "\t\tVec3i support = new Vec3i(site.x(), site.y() - 1, site.z());"),
    ("\t\t\t\t\tif (view.blockAt(candidate.below()).isEmpty()) {",
     "\t\t\t\t\tif (view.blockAt(new Vec3i(candidate.x(), candidate.y() - 1, candidate.z())).isEmpty()) {"),
])

# MindLedger: the practice log is the record; the counters are a view over it, keyed by the verb.
patch("src/main/java/dev/ensouled/core/MindLedger.java", [
    (
        "\tprivate final Map<String, SkillStats> stats = new LinkedHashMap<>();",
        "\t/** Every verified practice of a skill, in order: the record a skill is a view over. */\n"
        "\tprivate final java.util.List<Practice> practice = new java.util.ArrayList<>();\n"
        "\t/** The view, memoised by verb: one verb is one learned skill however many routines speak it. */\n"
        "\tprivate final Map<String, SkillStats> stats = new LinkedHashMap<>();",
    ),
    (
        "\tpublic SkillStats stats(String skillId) {\n"
        "\t\treturn stats.computeIfAbsent(skillId, ignored -> new SkillStats());\n"
        "\t}",
        "\t/** One verified practice: which routine was used, which corpus verb it speaks. */\n"
        "\tpublic record Practice(long tick, String skillId, String verb, boolean success,\n"
        "\t\t\tString evidence) {\n"
        "\t}\n\n"
        "\t/**\n"
        "\t * The verb is the learned unit (Q572): two routines that speak the same attested verb build\n"
        "\t * one skill, and a routine the corpus gives no verb for is learned under its own id and\n"
        "\t * reported as unattested rather than given an invented name.\n"
        "\t */\n"
        "\tpublic static String learnedKey(String skillId) {\n"
        "\t\tString verb = VerbLibrary.corpus().spokenVerb(skillId);\n"
        "\t\treturn verb == null || verb.isBlank() ? String.valueOf(skillId) : verb;\n"
        "\t}\n\n"
        "\tpublic SkillStats stats(String skillId) {\n"
        "\t\treturn stats.computeIfAbsent(learnedKey(skillId), ignored -> new SkillStats());\n"
        "\t}\n\n"
        "\t/** Record one verified outcome. The counter moves only because a practice row exists. */\n"
        "\tpublic Practice practiced(String skillId, long tick, boolean success, String evidence) {\n"
        "\t\tPractice row = new Practice(tick, skillId, learnedKey(skillId), success,\n"
        "\t\t\t\tevidence == null ? \"\" : evidence);\n"
        "\t\tpractice.add(row);\n"
        "\t\tif (success) {\n"
        "\t\t\tstats(skillId).recordSuccess();\n"
        "\t\t} else {\n"
        "\t\t\tstats(skillId).recordFailure();\n"
        "\t\t}\n"
        "\t\treturn row;\n"
        "\t}\n\n"
        "\tpublic java.util.List<Practice> practice() {\n"
        "\t\treturn java.util.List.copyOf(practice);\n"
        "\t}\n\n"
        "\t/** Replay the record, not the counters: a restart learns what the being actually did. */\n"
        "\tpublic void restorePractice(java.util.List<Practice> rows) {\n"
        "\t\tfor (Practice row : rows) {\n"
        "\t\t\tpractice.add(row);\n"
        "\t\t\tif (row.success()) {\n"
        "\t\t\t\tstats(row.skillId()).recordSuccess();\n"
        "\t\t\t} else {\n"
        "\t\t\t\tstats(row.skillId()).recordFailure();\n"
        "\t\t\t}\n"
        "\t\t}\n"
        "\t}",
    ),
    (
        "\tpublic Map<String, SkillStats> statsBySkill() {\n"
        "\t\treturn Collections.unmodifiableMap(stats);\n"
        "\t}",
        "\t/** The learned skills, keyed by the verb the corpus attests: the civilian-doc view. */\n"
        "\tpublic Map<String, SkillStats> statsBySkill() {\n"
        "\t\treturn Collections.unmodifiableMap(stats);\n"
        "\t}",
    ),
])

# Call sites: a practice row, not a counter bump.
patch("src/main/java/dev/ensouled/core/AgentMind.java", [
    ("\t\t\t\tledger.stats(candidate.skillId()).recordFailure();",
     "\t\t\t\tledger.practiced(candidate.skillId(), tick, false, \"gate:\" + candidate.candidateId());"),
    ("\t\tledger.stats(intention.skillId()).recordSuccess();",
     "\t\tledger.practiced(intention.skillId(), tick, true, \"intent:\" + intention.id());"),
    ("\t\tledger.stats(intention.skillId()).recordFailure();",
     "\t\tledger.practiced(intention.skillId(), tick, false, \"intent:\" + intention.id());"),
])

# Persistence: save the practice rows beside the counters, and prefer them on restore.
patch("src/main/java/dev/ensouled/core/MindPersistence.java", [
    (
        "\t\tList<StateNode> statNodes = new ArrayList<>();",
        "\t\tList<StateNode> practiceNodes = new ArrayList<>();\n"
        "\t\tfor (MindLedger.Practice p : ledger.practice()) {\n"
        "\t\t\tpracticeNodes.add(new StateNode().putLong(\"t\", p.tick())\n"
        "\t\t\t\t\t.putString(\"skill\", p.skillId()).putString(\"verb\", p.verb())\n"
        "\t\t\t\t\t.putString(\"ok\", Boolean.toString(p.success()))\n"
        "\t\t\t\t\t.putString(\"e\", p.evidence()));\n"
        "\t\t}\n"
        "\t\troot.putNodeList(\"practice\", practiceNodes);\n\n"
        "\t\tList<StateNode> statNodes = new ArrayList<>();",
    ),
    (
        "\t\tfor (StateNode n : root.nodes(\"stats\")) {\n"
        "\t\t\tledger.restoreStats(n.string(\"skill\", \"\"), n.integer(\"successes\", 0), n.integer(\"failures\", 0));\n"
        "\t\t}",
        "\t\tList<MindLedger.Practice> practiced = new ArrayList<>();\n"
        "\t\tfor (StateNode n : root.nodes(\"practice\")) {\n"
        "\t\t\tpracticed.add(new MindLedger.Practice(n.lng(\"t\", 0L), n.string(\"skill\", \"\"),\n"
        "\t\t\t\t\tn.string(\"verb\", \"\"), Boolean.parseBoolean(n.string(\"ok\", \"false\")),\n"
        "\t\t\t\t\tn.string(\"e\", \"\")));\n"
        "\t\t}\n"
        "\t\tif (!practiced.isEmpty()) {\n"
        "\t\t\tledger.restorePractice(practiced);\n"
        "\t\t} else {\n"
        "\t\t\t// Saves written before the practice log carry counters only; import them once.\n"
        "\t\t\tfor (StateNode n : root.nodes(\"stats\")) {\n"
        "\t\t\t\tledger.restoreStats(n.string(\"skill\", \"\"), n.integer(\"successes\", 0),\n"
        "\t\t\t\t\t\tn.integer(\"failures\", 0));\n"
        "\t\t\t}\n"
        "\t\t}",
    ),
])
print("ok")
