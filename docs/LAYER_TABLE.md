# Layer table — who owns what, and what the Minecraft mod is allowed to be

Ruling recorded 2026-10-31, from the user's own words: the Java mod is a **downstream client** of the
secure shell's package streamer, the `toasts` package addon is how game toasts are displayed, and the
mod's server side reads **its own** store for the civilians. This file is the table that ruling
implies, and the refusals that keep each layer from quietly becoming a second copy of another.

## The table

| Layer | Owner | In this tree | Not this tree |
|---|---|---|---|
| Package declaration | `dl-enclave.surface/1` (vendored from `dl-enclave` via `secure-shell`) | [`packages/toast/toast.surface`](../packages/toast/toast.surface), copied byte for byte | a second surface grammar, a new `secure-shell.*` format |
| Port baseline | `secure-shell.port-baseline/1` (`secure-shell`'s `baselines/`) | [`packages/toast/toast.port.json`](../packages/toast/toast.port.json) — one window, two nodes | a re-baselined surface to make a mount pass |
| Mount receipt | `surface.mount/2` (`secure-shell` `tools/port/store.mjs`) | `dev.ensouled.surface.MountReceipt` — same eleven fields, same `receipt-id` formula | a receipt that carries the window tree |
| Served record | `secure-shell.served/1` (`secure-shell` `tools/port/served.mjs`) | `dev.ensouled.surface.ServedPayload` — wishes, notices, windows, nodes, evidence rows | a tier that arrives as a value, a server-side toast tally |
| Ledger | `secure-shell.ledger/1` (`secure-shell` `docs/LEDGER.md`) | `dev.ensouled.surface.StreamLedger` at `run/stream/ledger.jsonl` | the client as a writer |
| Streamer transport | `secure-shell` HTTP/SSE boundary | `dev.ensouled.surface.SurfaceStream` on `127.0.0.1:7664`, bearer token from `run/stream.token` | a browser page, a second port for the display |
| Display (in-game) | **the game client's HUD layer** — the display owner named by the user | `dev.ensouled.client.StreamClient` → vanilla `ToastManager` | a Java-drawn palette, a custom toast graphic, a window manager |
| Display (desktop shell) | `secure-shell` / Openlock | not ported here | a fork of Openlock in this tree |
| Civilians' state | this mod's own store | `run/world/ensouled/minds.dat` (§49.2 continuity) | reading civilians from the ledger, writing minds into it |
| Control (CUA path) | the mod's server side, ticketed | `ControlRelay` — declared action set, ticket per program, presenter, ledger row | an action with no ticket, a widened action set, a screenshot as the record |
| Witness | the client that is showing the window | `StreamClient` runs the check over the served payload, displays command / exit / checks / digest, posts it | a screenshot alone, a Node result reported as a client witness |

## The refusals this table buys

1. **No second display or palette.** The toast stack is the game's own `ToastManager`; the client
   adds text to it. A colour, radius, font or border added by this project would be a second palette.
2. **No server-side summary of the display.** The toast tally is computed by the display from the
   served rows, as `openlock`'s `reportWishes()` does; a summary field in the payload is refused so
   the number beside the window cannot disagree with the window.
3. **No tier carried as a value.** The served payload carries requirement names and evidence
   digests; the tier is derived on read. In this tree the material ladder is not declared, so every
   derivation answers `ladder-absent` — an honest absent, not a floor.
4. **The client never writes the ledger.** It runs the witness check, shows the result, and posts it
   to `/api/witness`; the streamer validates and appends the `test` row, then the client renders the
   row's notice. The display is not the writer.
5. **Control without a ticket is refused before it runs.** Every program opens a ticket with a named
   presenter and a ledger row; every packet carries that ticket; an action outside the declared set
   is refused at the sender, and a packet with no ticket is refused at the client.
6. **The stream is not an authority.** A served payload can say what to draw and what evidence
   exists; it cannot move a body, grant a capability, or raise a rung.

## What is deliberately not ported yet

The desktop surfaces (frame, control, tab, world, motion lanes), the Openlock window manager, the
axis viewer, the DOM renderer and the browser fixtures. This tree has one window — `beacon` /
`small-right`, no chrome, no moves — because one window is what the sequencing proof needs, and
`secure-shell`'s H9/H10 are gated on a client witness this port does not claim to be.

The civilians' store is still `minds.dat`. MongoDB is not in this tree; the ruling is recorded as the
layer's direction, not as a read-back from the code (this build has no MongoDB dependency).

## Sequence the proof follows

a. the beings' own refusals and lifecycle reach the streamer (gate refusals, mount, witness);
b. mount + `surface.mount/2` receipt on the server side;
c. serve `secure-shell.served/1` on loopback with a bearer token;
d. the game client fetches it with the token, over HTTP, and subscribes to the SSE feed;
e. one toast is drawn by the game client's own toast stack for a ledger refusal;
f. the witness shows command / exit / checks / digest for the check the client ran, and the row it
   produces comes back to the client as a notice.
