// mineflayer-pathfinder is CommonJS; its named exports are not visible to the ESM loader directly.
// One wrapper keeps the rest of the runtime on ordinary named imports.
import pkg from "mineflayer-pathfinder";

export const { Movements, goals, pathfinder } = pkg;
