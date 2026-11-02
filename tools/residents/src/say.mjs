// Test participant: one ordinary player session, one chat line, then a short listen.
//
// This is not an agent and carries no mind. It exists so the resident's hearing is exercised through
// the server's real player-chat path rather than an operator broadcast.
import mineflayer from "mineflayer";

const args = process.argv.slice(2);
const line = args.join(" ");
const bot = mineflayer.createBot({
	host: process.env.MC_HOST ?? "127.0.0.1",
	port: Number(process.env.MC_PORT ?? 25565),
	username: process.env.MC_USER ?? "Visitor",
	auth: "offline",
	version: "1.20.1",
});

bot.once("spawn", () => {
	console.log(`[say] ${bot.username} joined at ${bot.entity.position}`);
	if (!line) {
		console.log("[say] no line given");
		bot.quit("nothing to say");
		return;
	}
	setTimeout(() => {
		bot.chat(line);
		console.log(`[say] <${bot.username}> ${line}`);
		setTimeout(() => bot.quit("said"), 12000);
	}, 1500);
});

bot.on("chat", (username, message) => {
	console.log(`[say] chat <${username}> ${message}`);
});
bot.on("error", (error) => console.error(`[say] error ${error.message}`));
bot.on("end", () => process.exit(0));
setTimeout(() => process.exit(0), 30000);
