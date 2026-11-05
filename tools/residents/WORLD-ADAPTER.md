# The residents body as an axis world adapter

`world-adapter.mjs` serves this body to an axis as a world adapter, so a physical act is verified
from what the server sent rather than from a frame either side authored.

```bash
node tools/residents/world-adapter.mjs --port 8789 --mc-port 25566 --name AxisBody
# GET  /version      adapter, version, server, whether the body is online
# GET  /state        position, on_ground, and a digest over position + every block within ±3
# POST /act          {actor, action:"move"|"dig", input:{x,y,z}} -> the observation after the act
```

The digest is a sha256 over the position and the 245 blocks the server sent; an unloaded cell reads
`unloaded` and is never guessed. `move` walks through the pathfinder and refuses `world-not-moved`
when the body stayed put; `dig` re-reads the block and refuses `world-not-dug` unless the server's
block update made it air.

Measured on a live 1.20.1 server (flat world, port 25566, `AxisBody` as server entity id 1):

| Act | Digest |
|---|---|
| `/state` at spawn `3.5,-60,-2.5` | `sha256:fc7cfa2449575c09…` |
| `move {6,-60,-3}` — walked 1.91 blocks | `sha256:0ee6c3f56d4d2680…` |
| `dig {6,-61,-3}` — `grass_block` now air per the server | `sha256:f49ceb591814e6ad…` |

`secure-shell`'s `node tools/axis-scale.mjs --drivers 1000 --game http://127.0.0.1:8789` exited 0
with the 41st verified frame citing that dig's digest, `source: adapter`, ticket
`civ-0000:ticket:1`.
