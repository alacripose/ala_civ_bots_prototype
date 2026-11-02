"""Second half: build plans, placement evidence, and the reader's close rule."""
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


placed_methods = '''
\t/**
\t * Record one block that the body observed itself put into the world. The count is the evidence:
\t * a build commitment is satisfied by blocks standing in the world, never by the being saying it
\t * built them. Called by the routine after it has seen the block and the stack shrink.
\t */
\tpublic static void recordPlacement(String agentId, long tick, Vec3i at, String blockId) {
\t\tWorkLedger ledger = get();
\t\tif (ledger != null) {
\t\t\tledger.placed(agentId, tick, at, blockId);
\t\t}
\t}

\tprivate void placed(String agentId, long tick, Vec3i at, String blockId) {
\t\tOptional<Commitment> open = openCommitment(agentId);
\t\tif (open.isEmpty()) {
\t\t\treturn;
\t\t}
\t\tCommitment commitment = open.get();
\t\tPlan plan = planFor(commitment);
\t\tif (plan == null || !Phase1GatePolicy.SKILL_PLACE.equals(plan.step())) {
\t\t\treturn;
\t\t}
\t\tint count = placedCounts.getOrDefault(commitment.commitmentId(), 0) + 1;
\t\tplacedCounts.put(commitment.commitmentId(), count);
\t\trecord(commitment.name(), agentId, commitment, "build-placed",
\t\t\t\tMap.of("block", blockId == null ? "" : blockId, "at", String.valueOf(at),
\t\t\t\t\t\t"placed", count + "/" + commitment.deliverable().count()));
\t\tif (count >= commitment.deliverable().count()) {
\t\t\tcommitments.put(commitment.commitmentId(),
\t\t\t\t\tcommitment.closed(Commitment.State.SATISFIED, tick, "placed " + count));
\t\t\tcloseTicketOnPlacement(commitment, tick, count);
\t\t}
\t\tsave();
\t}

\t/** HELD, with the number of blocks seen standing, not the number the being claims. */
\tprivate void closeTicketOnPlacement(Commitment commitment, long tick, int count) {
\t\tfor (Ticket ticket : List.copyOf(tickets.values())) {
\t\t\tif (!ticket.commitmentId().equals(commitment.commitmentId()) || !ticket.open()) {
\t\t\t\tcontinue;
\t\t\t}
\t\t\tTicket closed = ticket.closed(Ticket.State.HELD, tick, "placed " + count,
\t\t\t\t\t"placed " + count + "/" + commitment.deliverable().count() + " at tick " + tick);
\t\t\ttickets.put(closed.ticketId(), closed);
\t\t\trecord(commitment.name(), commitment.agentId(), commitment, "ticket-closed",
\t\t\t\t\tMap.of("ticket", closed.ticketId(), "state", "HELD",
\t\t\t\t\t\t\t"cells", closed.usedCells() + "/" + closed.declaredCells(),
\t\t\t\t\t\t\t"witness", closed.witness()));
\t\t\tpublish(closed);
\t\t}
\t}

\t/** The item this body would place for the work it owes, if it carries one. */
\tpublic static Optional<String> itemForBuild(String agentId, WorldView view) {
\t\tWorkLedger ledger = get();
\t\tif (ledger == null || view == null) {
\t\t\treturn Optional.empty();
\t\t}
\t\tOptional<Commitment> open = ledger.openCommitment(agentId);
\t\tif (open.isEmpty()) {
\t\t\treturn Optional.empty();
\t\t}
\t\tPlan plan = ledger.planFor(open.get());
\t\tif (plan == null || !Phase1GatePolicy.SKILL_PLACE.equals(plan.step())) {
\t\t\treturn Optional.empty();
\t\t}
\t\tDeliverable.Kind kind = open.get().deliverable().kind();
\t\tfor (dev.ensouled.core.InventorySlot slot : view.inventory()) {
\t\t\tif (slot.count() > 0 && kind.matches(slot.itemId())) {
\t\t\t\treturn Optional.of(slot.itemId());
\t\t\t}
\t\t}
\t\treturn Optional.empty();
\t}
'''

patch("src/main/java/dev/ensouled/work/WorkLedger.java", [
    (
        "\tprivate final Map<String, Ticket> tickets = new LinkedHashMap<>();",
        "\tprivate final Map<String, Ticket> tickets = new LinkedHashMap<>();\n"
        "\t/** Blocks this being has been observed placing, per commitment: build work's own evidence. */\n"
        "\tprivate final Map<String, Integer> placedCounts = new LinkedHashMap<>();",
    ),
    (
        "\t\tDeliverable deliverable = parsed.get();\n\t\tif (deliverable.met(view)) {",
        "\t\tDeliverable deliverable = parsed.get();\n"
        "\t\t// A build wish is not answered \"already held\": the stock it names is what must be put\n"
        "\t\t// into the world, so holding it is the precondition, not the completion.\n"
        "\t\tboolean building = Deliverable.builds(text);\n"
        "\t\tif (!building && deliverable.met(view)) {",
    ),
    (
        '\t\tPlan plan = new Plan("plan:" + UUID.randomUUID(), commitment.commitmentId(),\n'
        '\t\t\t\tList.of(STEP_HARVEST), 0, tick, 0L, "");',
        '\t\tString step = building ? Phase1GatePolicy.SKILL_PLACE : STEP_HARVEST;\n'
        '\t\tPlan plan = new Plan("plan:" + UUID.randomUUID(), commitment.commitmentId(),\n'
        "\t\t\t\tList.of(step), 0, tick, 0L, \"\");",
    ),
    (
        "\t\t\t\tdev.ensouled.core.VerbLibrary.corpus().spokenVerb(STEP_HARVEST),\n"
        "\t\t\t\tdev.ensouled.core.VerbLibrary.corpus().basis(STEP_HARVEST), deliverable.count(),",
        "\t\t\t\tdev.ensouled.core.VerbLibrary.corpus().spokenVerb(step),\n"
        "\t\t\t\tdev.ensouled.core.VerbLibrary.corpus().basis(step), deliverable.count(),",
    ),
    (
        "\tprivate Ticket ticketFor(String commitmentId) {",
        placed_methods,
    ),
    (
        "import dev.ensouled.core.StreamNotices;",
        "import dev.ensouled.core.StreamNotices;\nimport dev.ensouled.core.Vec3i;",
    ),
    (
        '\t\t\troot.add("tickets", ticketRows);',
        '\t\t\troot.add("tickets", ticketRows);\n'
        '\t\t\tJsonObject buildRows = new JsonObject();\n'
        "\t\t\tfor (Map.Entry<String, Integer> row : placedCounts.entrySet()) {\n"
        "\t\t\t\tbuildRows.addProperty(row.getKey(), row.getValue());\n"
        "\t\t\t}\n"
        '\t\t\troot.add("builds", buildRows);',
    ),
    (
        "\t\t\t\t\ttickets.put(ticket.ticketId(), ticket);\n\t\t\t\t}\n\t\t\t}",
        "\t\t\t\t\ttickets.put(ticket.ticketId(), ticket);\n\t\t\t\t}\n\t\t\t}\n"
        '\t\t\tif (root.has("builds")) {\n'
        '\t\t\t\tfor (Map.Entry<String, com.google.gson.JsonElement> row\n'
        '\t\t\t\t\t\t: root.getAsJsonObject("builds").entrySet()) {\n'
        "\t\t\t\t\tplacedCounts.put(row.getKey(), row.getValue().getAsInt());\n"
        "\t\t\t\t}\n"
        "\t\t\t}",
    ),
])

# The reader accepts a placement witness as evidence of a held build ticket.
patch("tools/residents/src/axis.mjs", [
    (
        'if (state === "HELD" && !/^held \\d+\\/\\d+/.test(witness)) {',
        'if (state === "HELD" && !/^(held|placed) \\d+\\/\\d+/.test(witness)) {',
    ),
])
print("ok")
