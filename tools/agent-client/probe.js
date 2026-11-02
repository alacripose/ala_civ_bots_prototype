// Protocol-only probe (Addendum G: narrow protocol testing only, never called parity).
// It logs in over the ordinary login path, sends one chat line through the normal client packet,
// prints the chat it receives back from the server, and disconnects.
import mineflayer from "mineflayer";

const host = process.env.MC_HOST ?? "127.0.0.1";
const port = Number(process.env.MC_PORT ?? 25565);
const username = process.env.MC_USER ?? "Probe";
const line = process.env.MC_LINE ?? "hello from a real client connection";

const bot = mineflayer.createBot({ host, port, username, auth: "offline", version: "1.20.1" });

bot.once("spawn", () => {
	console.log(`[probe] spawned as ${bot.username} at ${bot.entity.position}`);
	setTimeout(() => {
		bot.chat(line);
		console.log(`[probe] sent chat: ${line}`);
	}, 1500);
});

bot.on("chat", (sender, message) => {
	console.log(`[probe] chat <${sender}> ${message}`);
	if (sender === username && message === line) {
		console.log("[probe] received own line back through the server chat pipeline");
		setTimeout(() => {
			bot.quit("probe done");
			process.exit(0);
		}, 1000);
	}
});

bot.on("kicked", (reason) => {
	console.error(`[probe] kicked: ${reason}`);
	process.exit(1);
});
bot.on("error", (err) => {
	console.error(`[probe] error: ${err.message}`);
	process.exit(1);
});
bot.on("end", () => {
	console.log("[probe] disconnected");
});

setTimeout(() => {
	console.error("[probe] timeout without a delivered chat line");
	process.exit(1);
}, 45000);
