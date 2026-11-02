# Addendum E — Player-Visible Agent Chat, Communication Transport, and Social Observability

**Target runtime:** Minecraft Java Edition 1.20.1 / Fabric  
**Status:** Binding architecture amendment  
**Related architecture:** `minecraft_ensouled_system1_cua_architecture_v3.md`; Addenda C and D; `minecraft_persistent_interactive_dev_server_test_harness_directive.md`  
**Primary requirement:** The human operator must be able to see agents actually communicate with one another in the ordinary Minecraft chat interface while connected to the same persistent development server.

---

## E.1. Problem statement

The current implementation has a small internal `MessageBus` with typed request/accept/decline exchanges, but there is no adequate player-facing way to observe those exchanges in-game. As a result, the operator cannot readily distinguish agents that are communicating from agents that are merely completing scripted or independently selected actions. A test report or server log is not an adequate substitute for visible social behavior.

This is an architectural gap, not a cosmetic UI request.

The system must implement a real communication path that:

1. Allows one agent to form an utterance from its own intention, knowledge, and current interaction context.
2. Delivers that utterance to the intended agent(s).
3. Allows receiving agents to perceive and respond to the utterance through their own cognition.
4. Presents the exchange in the normal Minecraft chat window so a connected human can watch it happen.
5. Records enough provenance to trace a visible line back to the originating agent, speech act, and resulting response.

The chat window is the first user-facing social interface. Do not substitute a debug-only fake player, a synthetic watcher, a server log, or a decorative transcript that is disconnected from agent decisions.

## E.2. Binding design decision: one communication event, multiple views

The system must have one authoritative communication record per utterance. Its transport, delivery, rendering, and audit views must all derive from that same record.

```text
Agent A cognition
    │
    ├── forms a speech intention / speech act
    │
    ▼
CommunicationEnvelope (stable message ID + provenance + content)
    │
    ├── validates sender identity, recipient/scope, rate, expiry
    │
    ▼
ChatTransport
    │
    ├── delivers to addressed agent inbox(es)
    ├── publishes through the shared server's chat path
    ├── makes the same utterance visible to the human operator
    └── records delivery / read / response evidence
             │
             ▼
       Agent B perception
             │
             ▼
       Agent B interpretation and independent decision
             │
             ▼
       Agent B's own reply or action
```

The internal `MessageBus` may remain as the delivery mechanism for agent inboxes, but it must not become a hidden alternate communication system that bypasses chat. The server-visible chat utterance and the recipient's received communication must be linked by the same message ID and content hash.

A line written to chat without being delivered to the recipient is only a display event, not successful agent communication. Conversely, a private in-memory delivery with no visible presentation is not sufficient for the operator-facing development workflow.

## E.3. Player-facing presentation: use ordinary Minecraft chat

Agent speech must appear in the standard Minecraft chat interface used by the human player. Do not require a custom client GUI or a separate dashboard just to verify that agents are talking.

Default development presentation should make the speaker and audience clear. Example:

```text
[Aster] Does anyone have bread to spare? I need one for the trip.
[Rowan → Aster] I have one. I'll bring it over.
[Aster → Rowan] Thank you. I'll wait by the west chest.
[Rowan] I've placed the bread in the chest.
```

The exact display styling is configurable, but speaker identity must never be ambiguous. Agent messages must be visibly distinguishable from human chat and from server/system notices. Do not impersonate a human player or render an agent utterance as though it were written by the operator.

### E.3.1. Development default: visible agent conversation

On the local development server, agent-to-agent conversation should be visible to the connected operator by default, including direct exchanges. The operator is actively developing and inspecting the simulation, so a default that hides most agent dialogue defeats the requirement.

The development presentation may mark the scope:

- `[World]` — visible public agent speech.
- `[Local]` — speech whose modeled audience is within a configured proximity range.
- `[Direct A → B]` — addressed to one or more specific agents.
- `[System]` — transport, refusal, delivery, or diagnostic notices; never presented as an agent's own words.

For the initial implementation, it is acceptable to render all agent speech to the operator's ordinary chat stream even when its modeled delivery scope is direct or local, provided the line is clearly labelled and this is the configured development-observation policy. This must not silently change which agents actually receive the message.

### E.3.2. Chat is not a debug log

Do not flood chat with every perception sample, internal score, tick, memory lookup, or arbitrary state transition. Only intentional communicative acts should appear as agent speech.

A separate verbose trace may expose message IDs, delivery state, speech-act type, refusal reason, and correlation identifiers. Keep that trace distinguishable from dialogue. The user should see natural, concise agent communication without confusing it with raw debugging output.

### E.3.3. No fake chatter

Do not add random greetings, periodic idle chatter, scripted conversations, or prewritten exchanges merely to make the chat look active. A visible line must correspond to an actual communication decision made by that agent from its present needs, beliefs, task, relationship, perception, or response to another participant.

The current typed request/accept/decline messages can be rendered into readable language using deterministic templates grounded in the message fields. This is a valid first step. It must not imply that a richer speech, personality, or language-generation system already exists.

## E.4. Communication source and recipient semantics

The system must distinguish the following operations:

1. **Speak publicly:** create a world-visible utterance. Agents eligible for the configured world channel and connected human players can perceive it.
2. **Speak locally:** create an utterance whose modeled audience is constrained by an explicit range or location rule. The operator may still see it under the development-observation setting.
3. **Address an agent:** create an utterance directed to a specific `AgentId`; only the intended recipient(s) receive it as actionable communication, while the operator may receive an observation copy.
4. **Address a human player:** create an utterance for a human account or player session. The human sees it in chat; the message must not assume that the human will comply.
5. **Receive human chat:** recognize ordinary human chat as a communication event and make it available to the relevant agent(s), subject to addressing and scope rules.

The initial milestone does not need full natural-language understanding of every human sentence. It must at minimum support explicit addressing (for example, `@Aster`) and a small, documented set of intents that map to existing skills or questions. Unsupported or ambiguous requests must result in a clarifying question or a refusal, not an invented understanding.

A human message is information and a possible request. It is not, by itself, authority to bypass the canonical Axis, ownership checks, contracts, task scope, or action gates. A bot must not treat ordinary chat as unrestricted executable commands.

## E.5. Transport adapters: current in-process body and target networked clients

The implementation must acknowledge that there are two transport situations during migration.

### E.5.1. Current in-process `FakePlayerBody`

The existing `FakePlayerBody` uses a silent fake connection and does not behave like a fully connected client typing into the vanilla chat screen. It cannot be claimed to have emitted an ordinary client-originated, signed chat message merely because the server printed a line bearing its name.

For this legacy/test adapter, implement an explicit server-side chat presentation bridge:

- The agent creates a `CommunicationEnvelope` through its normal decision path.
- The server validates the sender's registered `AgentId` and formats the human-readable utterance.
- The server publishes a visibly attributed chat line to connected human clients using the appropriate 1.20.1 server message/broadcast mechanism.
- The same envelope is delivered to recipient agent inboxes.
- The transcript records `transport_kind = SERVER_BRIDGE`.

The line must be clearly marked as an Ensouled agent utterance. Do not falsely label it as a native client-originated signed message. The bridge is an honest compatibility path for the current in-process implementation, not proof of network-client chat parity.

### E.5.2. Target production networked bot clients

Under Addendum D, each production bot runs as a normal Fabric client connected to the one shared Fabric server. Its outgoing speech should use the normal client-to-server player chat path, and its incoming speech should be observed from the server's ordinary chat broadcast path.

For Minecraft 1.20.1 Fabric API, `ServerMessageEvents.CHAT_MESSAGE` is documented as firing when the server broadcasts player chat, typically from a client GUI or player-executed command. It is not called for messages blocked before broadcast. Use the exact pinned Fabric API version's documentation and verify its callback signature in the project's dependency set before implementation. This event is a suitable integration point for observing real incoming player chat, including bot-client messages and human messages, when they are accepted and broadcast. See the official Fabric API 0.92.1+1.20.1 documentation: <https://maven.fabricmc.net/docs/fabric-api-0.92.1+1.20.1/net/fabricmc/fabric/api/message/v1/ServerMessageEvents.html>.

A connected bot client must not rely on the server directly invoking its mind and pretending that the client sent a message. The communication adapter should submit the utterance through that bot's own client session. The server must then observe the same chat event that it observes for other connected players.

### E.5.3. One shared message contract across adapters

The `SERVER_BRIDGE` and `PLAYER_CHAT_PACKET` paths must implement the same semantic contract. A transport adapter may differ in how the message enters Minecraft, but it must not change:

- The sender's identity.
- The recipient and scope.
- The text and speech-act intent.
- The message identifier.
- The recipient's decision rights.
- The authority and world-action restrictions.
- The delivery and response evidence.

Every transcript record must identify which transport was actually used. Do not label a bridge-rendered line as a network-client chat packet.

## E.6. Message envelope and provenance

Extend the existing `AgentMessage` or define a compatible `CommunicationEnvelope`. Do not create a second unrelated message type that fragments communication state.

Required fields:

| Field | Purpose |
|---|---|
| `message_id` | Unique stable ID for this utterance. |
| `conversation_id` | Groups related turns without conflating separate conversations. |
| `sender_agent_id` / `sender_player_uuid` | Identifies the originating agent or human player. Exactly one sender identity kind is authoritative. |
| `sender_name_snapshot` | Human-readable name at send time; not used as identity. |
| `recipient_agent_ids` | Explicit agent recipients, if any. |
| `recipient_player_uuids` | Explicit human recipients, if any. |
| `scope` | `WORLD`, `LOCAL`, `DIRECT`, or another explicitly defined scope. |
| `speech_act` | `INFORM`, `REQUEST`, `OFFER`, `ACCEPT`, `DECLINE`, `QUESTION`, `ANSWER`, `COMMIT`, `REPORT`, `CLARIFY`, `WARN`, or a bounded initial subset. |
| `text` | The actual human-readable utterance. |
| `structured_payload` | Optional typed data such as item ID, count, task reference, or refusal reason. |
| `created_at_tick` | Server tick/time provenance. |
| `expires_at_tick` | Expiry for requests that should not remain actionable forever. |
| `transport_kind` | `SERVER_BRIDGE` or `PLAYER_CHAT_PACKET` (plus future explicit variants). |
| `delivery_state` | Created, submitted, broadcast, delivered, read, replied, expired, refused, or failed. |
| `correlation_id` | Links the message to task, transfer, contract, or world event. |
| `content_hash` | Detects mismatched content or duplicate processing. |

Names are presentation labels, never security principals. Use the registered `AgentId` and authenticated player UUID/session binding for identity. Never infer agent identity from a display name alone.

## E.7. Receiving chat is a perception event, not an automatic command

When a bot receives a chat message:

1. Attribute the sender using the authenticated session/identity registry.
2. Determine whether the message is addressed to this bot, its group, or the public channel.
3. Check the message's scope, age, and delivery status.
4. Convert it into a communication perception for that agent.
5. Let that agent independently interpret it and decide whether to answer, decline, ask for clarification, ignore it, or act within its authorized scope.
6. Record the result and any subsequent response.

The sender must not write directly into the recipient's intention, inventory, beliefs, or action queue. A message is evidence that someone communicated a proposition or request; it is not automatically evidence that the proposition is true or that the requested action is authorized.

The current `MessageBus` rule—an agent decides for itself whether it can satisfy a request—must be preserved. The chat integration must not regress this into remote mind control.

## E.8. Human interaction in ordinary chat

The operator must be able to type into the normal Minecraft chat box and communicate with agents while they are online.

Initial addressing behavior should include:

- `@Aster` for a direct addressed message.
- A documented group or world-addressing convention for messages intended for multiple agents.
- Ordinary public text as ambient communication only when the configured system explicitly enables agents to attend to it.

A human message must be visible to agents through the same communication ingress used for networked bot chat. For 1.20.1 Fabric, hook the accepted/broadcast player-chat event and resolve the sender through the ordinary player identity/session. Do not use a separate private command-only pathway as the sole means of human communication.

The human remains an ordinary player and retains full ability to inspect and modify the development world. The chat system must not create a watcher entity, force spectator mode, or restrict the operator to a read-only role. The separate persistent development server directive remains in force.

## E.9. Operator-facing chat controls and inspection

The minimum viable user interface is the existing vanilla chat window. Add concise commands for inspection and configuration; do not require the operator to inspect log files.

Proposed command surface (adapt naming to the existing command tree rather than duplicating commands):

```text
/ensouled chat status
/ensouled chat history [all|<agent>] [limit]
/ensouled chat trace <message-id>
/ensouled chat visibility <all|nearby|direct|off>
/ensouled chat pause <agent>
/ensouled chat resume <agent>
/ensouled agents
```

Expected behavior:

- `status` reports whether chat ingress, egress, agent subscriptions, and operator presentation are active.
- `history` prints recent actual dialogue from the persistent transcript, not reconstructed guesses.
- `trace` reports the exact sender, recipients, scope, transport, delivery state, linked reply, and associated task/event IDs.
- `visibility all` shows all agent utterances to the operator during development, including direct exchanges as clearly labelled observation copies.
- `visibility nearby` filters the presentation to a configured spatial radius but must not change underlying delivery semantics.
- `visibility off` hides agent lines from the operator only; it must not disable bot communication.
- `pause` and `resume` affect a specific agent's communication behavior only through explicit operator control and must not delete messages or fabricate responses.

If command names differ in the project, preserve the behavior and integrate into the established command namespace.

Every agent speech line should be visible without requiring a special watcher fake player. Human chat remains normal Minecraft chat.

## E.10. Persistence and transcript history

The current message bus is described as in-memory and loses pending speech on restart. This addendum requires a durable conversation transcript for operator inspection and continuity.

Persist at least:

- Message envelope and stable ID.
- Sender/recipient identities.
- Text and speech-act type.
- Timestamp/tick and scope.
- Transport kind.
- Delivery and response states.
- Relevant correlation IDs.

Pending requests may expire or be marked interrupted according to explicit policy. A restart must not silently replay a stale request as though it were newly spoken. If a message was broadcast but delivery/read confirmation was not recorded before shutdown, preserve the uncertain state and reconcile it on reconnect rather than duplicating the utterance.

The transcript is not a replacement for each agent's episodic memory. The transcript records what was communicated; each agent's memory records what that agent actually perceived, believed, and learned from it.

## E.11. Rate, loop, and echo controls

A shared chat interface introduces predictable failure modes. Implement bounded controls without hiding ordinary conversation:

- Per-agent message rate and burst limits.
- Maximum message length and bounded structured payload size.
- Duplicate/echo detection using message ID and content hash.
- Self-echo suppression so an agent does not treat its own broadcast as a new incoming message.
- Conversation correlation and turn limits to prevent endless reply loops.
- Explicit expiry for requests and offers.
- A configurable operator-visible verbosity policy.
- Clear refusal when a message is malformed, unauthenticated, stale, or outside permitted scope.

Do not solve message storms by silently disabling all dialogue. Record the refusal/throttle cause and keep the operator-facing status inspectable.

## E.12. Acceptance tests: prove actual communication, not merely display

Tests must run against the persistent shared development server where possible, without launching a new bot-owned server or resetting the operator's world. Disposable CI tests may remain separate, but must not be the only evidence.

### E.12.1. Current in-process compatibility test

With two registered `FakePlayerBody` agents and one connected human:

1. Agent A independently decides to request an item from Agent B.
2. A creates one communication envelope with a stable message ID.
3. The server bridge presents the utterance in the human's ordinary Minecraft chat window, visibly attributed to A.
4. B receives the same message ID and exact content through its own inbox/perception path.
5. B independently checks its own state and replies or declines.
6. The reply appears in ordinary Minecraft chat.
7. A receives that reply and records the result.
8. The transcript links the request, reply, and any subsequent physical transfer.
9. No duplicate event is produced by the chat echo.

This test proves the bridge path. It does not prove normal network-client chat parity.

### E.12.2. Networked-client chat test

With two real Fabric bot clients connected to one shared Minecraft 1.20.1 Fabric server and one human client:

1. Bot A submits an utterance through its own ordinary client chat path.
2. The server observes the accepted/broadcast chat message through the pinned Fabric API integration.
3. Bot B receives the message through the shared chat ingress and perceives it.
4. Bot B makes an independent response decision and submits a reply through its own client chat path.
5. The human operator sees both messages in the vanilla chat window.
6. The transcript records both messages as `PLAYER_CHAT_PACKET`.
7. The message IDs, contents, senders, recipients, and delivery states match the agent-side evidence.

### E.12.3. Human interaction test

1. The human types `@Aster` followed by a supported request in ordinary chat.
2. The server ingests that ordinary player chat message.
3. Aster receives a communication perception associated with the human player's UUID.
4. Aster independently answers, declines, or asks a clarification question.
5. The response is visible in the human's chat.
6. The human can continue playing and modifying the world normally.

### E.12.4. Negative tests

- A server log entry without a delivered envelope does not pass as communication.
- A chat line not received by the addressed agent does not pass as communication.
- A direct message is not treated as a public order for all bots.
- A bot does not process its own echoed message as a new message.
- A stale request is not silently replayed after restart.
- An unauthenticated name cannot impersonate a registered AgentId.
- A display-only bridge line is not reported as `PLAYER_CHAT_PACKET`.
- A human message cannot bypass Axis or grant an agent world-action authority.
- Rate limits do not erase the message trace or hide their reason.

## E.13. Implementation sequence and phase boundary

This work is a user-facing capability milestone, not an invitation to indefinitely expand communication tests.

### `CHAT-01` — Visible chat for current two-agent handoff

Use the current two-agent request/accept/decline protocol. Render the actual exchange in the operator's vanilla chat through an explicitly identified `SERVER_BRIDGE` adapter. The addressed agent must receive the same envelope and decide independently. Add transcript/history inspection. This should be the immediate successor task.

**Exit:** one real agent-initiated request, one independent response, both visible in chat, both linked to the same conversation, with no duplicate delivery.

### `CHAT-02` — Ordinary human chat ingress

Capture ordinary human chat, support explicit `@AgentName` addressing, and let the agent answer in visible chat. Preserve the human's full normal gameplay access.

**Exit:** a human sends a supported request in ordinary chat, the agent perceives it and responds or refuses, and the response is visible.

### `CHAT-03` — Networked bot client chat parity

Move bot egress/ingress onto each connected Fabric client's ordinary chat path under Addendum D. Verify the real player-chat event on the server and classify the transcript as `PLAYER_CHAT_PACKET`.

**Exit:** two connected bot clients exchange actual chat through the shared server, with the human watching in vanilla chat.

### `CHAT-04` — Rich conversation and social semantics

Expand speech acts, conversation memory, offers, counteroffers, negotiation, commitments, trust evidence, group communication, proximity/local speech, and relationship effects. These must emerge from agent decisions and actual interaction history.

Do not block `CHAT-01` on the full future social model. Do not keep `CHAT-01` open merely because later speech acts are not yet implemented.

## E.14. Non-goals and boundaries

This addendum does not require:

- An LLM to generate every chat message.
- Random background chatter.
- A custom GUI before vanilla chat is implemented.
- Every future speech act in the first milestone.
- Full semantic understanding of arbitrary human language in `CHAT-01`.
- Treating chat as a privileged command channel.
- Replacing the persistent AgentId registry with usernames.
- Replacing the canonical Axis with chat-specific permissions.
- Making every internal thought public. Only selected communicative acts are rendered.

The first milestone should use the current real request/accept/decline protocol and deterministic, fact-grounded rendering. Expand language and social complexity after the end-to-end communication path works.

## E.15. Binding implementation rules

1. **One source of truth:** the displayed line, recipient delivery, and transcript refer to the same communication envelope.
2. **Visible by default in development:** the human operator sees agent-to-agent dialogue in the vanilla chat window without a watcher entity or custom spectator role.
3. **Actual delivery:** a displayed line alone is not communication; the intended recipient must receive and perceive it.
4. **Independent minds:** the sender cannot dictate the recipient's response, belief, inventory, or action.
5. **Honest transport provenance:** distinguish server-bridge presentation from real network-client chat packets.
6. **Human participation:** ordinary human chat is a first-class communication input; humans retain full gameplay control.
7. **No fake chatter:** every agent utterance must originate from an actual communication decision.
8. **No hidden bypass:** chat cannot bypass Axis, contracts, permissions, ownership, or action gates.
9. **Persistent evidence:** keep a traceable conversation history and do not duplicate messages across restart/reconnect.
10. **Bounded milestones:** finish `CHAT-01`, activate `CHAT-02`, and continue. Do not turn the first visible exchange into an exhaustive speech-system project.

**The objective is not merely to make agents produce text. It is to let agents communicate with one another through Minecraft, let the human player witness that communication in the normal chat window, and preserve evidence that the recipient actually received and responded to what was said.**

---

## References

- Fabric API 0.92.1+1.20.1, `ServerMessageEvents`: <https://maven.fabricmc.net/docs/fabric-api-0.92.1+1.20.1/net/fabricmc/fabric/api/message/v1/ServerMessageEvents.html>. The `CHAT_MESSAGE` event is documented as occurring when the server broadcasts player chat sent through a client GUI or player-executed command; blocked messages do not reach this event.
- Fabric API 0.91.0+1.20.1, `ServerMessageEvents.ChatMessage`: <https://maven.fabricmc.net/docs/fabric-api-0.91.0+1.20.1/net/fabricmc/fabric/api/message/v1/ServerMessageEvents.ChatMessage.html>. Documents the callback parameters and player-chat broadcast semantics.
- Fabric API 0.92.0+1.20.1 index: <https://maven.fabricmc.net/docs/fabric-api-0.92.0+1.20.1/index-all.html>.

**Version note:** these references establish that the Fabric message event exists in the 1.20.1 API line. The implementation must use the exact Fabric API dependency pinned by the project and compile against that version; do not copy callback signatures from another Minecraft/Fabric version without verification.
