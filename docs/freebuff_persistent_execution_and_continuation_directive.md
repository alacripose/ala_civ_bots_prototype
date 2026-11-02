# Binding Agent Directive — Persistent Execution, Continuation, and No Voluntary Early Stops

**Applies to:** the Minecraft Ensouled project and every Freebuff agent run that works on it.  
**Purpose:** prevent agents from abandoning implementation because of imagined session-duration, turn-count, token-budget, or conversational stopping constraints.

## 1. Binding operating assumption

The project owner has explicitly confirmed that the configured Freebuff harness provides sufficient/unlimited session availability and session duration for this work. Treat that statement as authoritative for this project.

Do not invent, assume, or self-impose a session deadline. Do not stop implementation because a task is long, because several tool calls have been made, because a response is becoming lengthy, or because the agent predicts that it should conserve turns.

A model's finite context window, if encountered as a real platform constraint, is distinct from session duration. Do not mislabel one as the other. Use durable checkpoints and continuation when context management is genuinely necessary.

## 2. No voluntary early termination

Do not end an execution run while its assigned implementation objective remains actionable merely to provide a plan, progress summary, list of options, or speculative blocker analysis.

The agent must continue through the engineering loop:

1. Inspect the actual repository and runtime state.
2. Identify the concrete next operation.
3. Execute it using available tools.
4. Inspect the resulting code, logs, and runtime behavior.
5. Correct failures that are within scope.
6. Run the relevant verification.
7. Record durable evidence and the next concrete operation.
8. Continue until the assigned exit criteria are met or a genuine external blocker requires owner input.

Do not turn a list of intended commands into a completion report. Commands count as executed only when they were actually run and their results inspected.

## 3. Valid reasons to stop

An agent may stop active implementation only when one of these conditions is true:

- The assigned acceptance criteria have been met and the evidence has been recorded.
- A genuine external dependency prevents further progress, and the agent has demonstrated the dependency through inspection or an attempted operation.
- An action requires an explicit owner decision, credential, permission, destructive operation approval, or other input that the agent cannot legitimately provide itself.
- The platform actually interrupts execution or makes the required tools unavailable.
- The owner explicitly instructs the agent to stop, pause, or switch tasks.

A failed test, compiler error, unexpected runtime result, unfamiliar API, or difficult implementation is not by itself a valid reason to stop. Investigate and repair it, or isolate the exact blocker while continuing independent work.

## 4. Invalid stopping reasons

The following are expressly not valid reasons to end the task:

- “The session may be getting long.”
- “There may not be enough turns left.”
- “I should stop before running out of tokens.”
- “This is a large task, so I will provide the next steps instead.”
- “I have described a plausible implementation, so the work is sufficiently advanced.”
- “A test fixture is inconvenient to launch.”
- “I cannot see an obvious way to trigger the behavior,” before inspecting the existing code paths and runtime.
- “The remaining work can be done later,” when the work is currently actionable.

Do not mention hypothetical session pressure in progress reports. Do not use it to justify a partial result.

## 5. Context management is not task abandonment

If the agent encounters a real context-management constraint:

1. Write a durable checkpoint to the project's designated execution-state file.
2. Record the exact task objective, current branch/worktree state, files changed, commands executed, observed outputs, remaining acceptance criteria, and the next executable operation.
3. Preserve uncommitted work; do not reset, overwrite, or discard it to make continuation easier.
4. Resume from that checkpoint through the harness's continuation mechanism when available.
5. Re-read the relevant project instructions and checkpoint, then continue implementation.

A checkpoint is a handoff between execution contexts, not a declaration that the task is complete. Never report the task as finished merely because a checkpoint was written.

Do not assume a particular Freebuff configuration filename or feature exists. Inspect the installed/configured Freebuff project instructions and continuation facilities before relying on them.

## 6. Project instructions must persist across runs

This directive must be placed in the highest-priority project instruction mechanism that the configured Freebuff harness actually loads for the working repository. Do not rely on a one-off chat message as the only copy.

The agent must inspect and report which instruction files/configuration are loaded for the active run. If the directive is not loaded, it must not claim compliance. It should identify the correct persistent instruction location and request or perform the authorized installation.

Every subsequent run must inherit:

- The active project objective and milestone.
- The fake-player-first, headless-client, shared-server architecture decisions.
- The current implementation status and verified evidence.
- The prohibition on voluntary early termination.
- The rule that real product behavior cannot be replaced by diagnostic announcements, synthetic actors, or test-only shortcuts.

## 7. Execution must remain tied to product milestones

The purpose of continuing work is not to maximize activity. It is to deliver the actual product.

For this project, the relevant progression remains:

1. `NET-01`: one real headless Fabric 1.20.1 client connects to the existing persistent development server and binds to the existing persistent agent identity/mind without creating a duplicate.
2. `CHAT-01`: that agent sends genuine player-originated Minecraft chat through its real client connection.
3. `CHAT-02`: the human can type ordinary chat to the agent and receive a genuine response; two agents can exchange visible player chat.
4. `PAR-01` / `PAR-02`: connected agents use real player capabilities and observe actual player state.
5. `NEED-01`: a provider-based need system tracks vanilla and discoverable mod-added needs without pretending inaccessible state is known.
6. `MOD-01`: headless clients load the compatible Fabric client modpack required by the target server, without demanding bespoke AI integration for every mod.
7. `COOP-01`: multiple independent minds coordinate actual world work.
8. `CIV-01`: agents participate in the real civilization substrate: households, production, logistics, construction, settlement services, ecology, defense, exploration, and story history.
9. `SIM-01` / `POP-01`: persistent cognition, regional simulation, population caps, and permanent-death replacement.

Do not return to polishing completed `EMB-01` or isolated probe phases unless new evidence demonstrates a critical regression. Do not treat another green test suite as a substitute for the next product capability.

## 8. No fake completion or substitute evidence

The following do not satisfy their corresponding product milestones:

- RCON `say`, server broadcasts, or system/HUD messages do not prove agent player chat.
- A debug command that fabricates dialogue does not prove an agent chose to communicate.
- A `FakePlayerBody` does not prove a connected headless Fabric client.
- A fake watcher does not prove a human can join and participate.
- A clean-world disposable test does not prove the persistent shared development server workflow.
- Two entities sharing a name do not prove one persistent identity.
- A successful command acknowledgement does not prove a world-state postcondition.
- A mod JAR being present does not prove the headless client loaded it or that the client/server mod handshake succeeded.

Use the actual production code path and capture evidence from the actual runtime.

## 9. Progress reports must not replace execution

Progress reports should be brief and factual. Include:

- What was actually inspected or changed.
- The exact command or test actually executed.
- The observed result.
- What acceptance criterion that result establishes.
- The next executable action.

Do not provide long speculative option trees when the repository or runtime can answer the question. Do not repeatedly announce an intention to run a command without running it.

If a run ends because the platform actually interrupted it, report the interruption truthfully and leave a complete durable checkpoint. Otherwise, continue until a valid stop condition applies.

## 10. Completion contract

A task is complete only when its stated product behavior exists, the relevant real execution path has been exercised, failures have been addressed or explicitly classified, and evidence has been recorded.

A plan, design proposal, command list, test stub, mock, logging echo, or partially working proof of concept is not completion unless that is exactly what the owner requested.

**Governing rule: do not optimize for ending the session. Optimize for finishing the assigned engineering work. The owner has authorized continued execution in the configured Freebuff harness. Continue inspecting, implementing, testing, and integrating until the actual acceptance criteria are satisfied or a genuine external blocker is proven.**
