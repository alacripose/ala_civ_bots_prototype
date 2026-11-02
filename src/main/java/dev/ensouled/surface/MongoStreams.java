package dev.ensouled.surface;

import com.google.gson.JsonObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The per-civilian stream store.
 *
 * A stream is one being's own feed: its refusals, its witness, the control rows its body opened.
 * The served payload is read from the stream of the agent it is served for, so one citizen's row
 * cannot appear in another's toast stack — that isolation is what this store is for, and it is not
 * optional. With no store reachable the streamer refuses to serve (`stream-store-unavailable`)
 * rather than falling back to a merged feed, because the merged feed is the failure it prevents.
 *
 * The shape follows `openlock/tools/axis_witness.py`, this project family's own store discipline:
 * candidate URIs (`127.0.0.1:27017`, then `:27018`, or an explicit `ENSOULED_MONGO_URI`), a ping
 * before anything is used, and **a write is not a record until it has been read back and compared**.
 * Every row is written, read back, compared field by field, and stamped `verified`; the verification
 * is kept in its own collection, and only verified rows are served. A row that reads back different
 * from what was written is a named refusal, never a silently trusted row.
 */
public final class MongoStreams implements AutoCloseable {
	public static final String DATABASE = "ensouled_streams";
	private static final List<String> CANDIDATES = List.of("mongodb://127.0.0.1:27017", "mongodb://127.0.0.1:27018");

	private final MongoClient client;
	private final MongoDatabase database;
	private final MongoCollection<Document> streams;
	private final MongoCollection<Document> events;
	private final MongoCollection<Document> verifications;
	private final String project;
	private final String uri;

	private MongoStreams(MongoClient client, MongoDatabase database, MongoCollection<Document> streams,
			MongoCollection<Document> events, MongoCollection<Document> verifications, String project, String uri) {
		this.client = client;
		this.database = database;
		this.streams = streams;
		this.events = events;
		this.verifications = verifications;
		this.project = project;
		this.uri = uri;
	}

	/** The store, or a named refusal. An explicit URI is tried alone; otherwise the candidates in order. */
	public static MongoStreams open(String explicit, String project) {
		List<String> uris = explicit == null || explicit.isBlank() ? CANDIDATES : List.of(explicit);
		StringBuilder failures = new StringBuilder();
		for (String uri : uris) {
			try {
				return connect(uri, project);
			} catch (SurfaceRefusal refused) {
				failures.append(failures.isEmpty() ? "" : "; ").append(uri).append(": ").append(refused.why);
			}
		}
		throw new SurfaceRefusal("stream-store-unavailable", "mongodb", "no store is reachable (" + failures + ")");
	}

	private static MongoStreams connect(String uri, String project) {
		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(uri))
				.applyToClusterSettings(builder -> builder.serverSelectionTimeout(1500, TimeUnit.MILLISECONDS))
				.applyToSocketSettings(builder -> builder.connectTimeout(1500, TimeUnit.MILLISECONDS)
						.readTimeout(3000, TimeUnit.MILLISECONDS))
				.build();
		MongoClient client = MongoClients.create(settings);
		try {
			MongoDatabase database = client.getDatabase(DATABASE);
			database.runCommand(new Document("ping", 1));
			MongoCollection<Document> streams = database.getCollection("streams");
			MongoCollection<Document> events = database.getCollection("events");
			MongoCollection<Document> verifications = database.getCollection("verifications");
			streams.createIndex(Indexes.ascending("surface"));
			events.createIndex(Indexes.compoundIndex(Indexes.ascending("agent"), Indexes.ascending("seq")));
			rejectIfUnreadable(streams);
			rejectIfUnreadable(events);
			rejectIfUnreadable(verifications);
			return new MongoStreams(client, database, streams, events, verifications, project, uri);
		} catch (MongoException unavailable) {
			client.close();
			throw new SurfaceRefusal("stream-store-unavailable", uri, unavailable.getMessage());
		}
	}

	/**
	 * A store the process can write to but not read is not a store: `openlock`'s rule that a
	 * reading which cannot be recorded refuses the mount has its mirror here.
	 */
	private static void rejectIfUnreadable(MongoCollection<Document> collection) {
		try {
			collection.find(Filters.eq("_id", "__probe__")).limit(1).first();
		} catch (MongoException unreadable) {
			throw new SurfaceRefusal("stream-store-unreadable", collection.getNamespace().getCollectionName(),
					unreadable.getMessage());
		}
	}

	private String key(String agent) {
		return project + ":" + agent;
	}

	/** One document per agent and surface; a second registration is an update, never a second stream. */
	public void register(String agent, String surface, String digest) {
		String at = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
		Document set = new Document("project", project).append("agent", agent).append("surface", surface)
				.append("digest", digest).append("updated_at", at);
		Document insert = new Document("created_at", at);
		streams.updateOne(Filters.eq("_id", key(agent)),
				new Document("$set", set).append("$setOnInsert", insert),
				new UpdateOptions().upsert(true));
		Document back = streams.find(Filters.eq("_id", key(agent))).first();
		if (back == null || !surface.equals(back.getString("surface")) || !digest.equals(back.getString("digest"))) {
			throw new SurfaceRefusal("stream-readback-mismatch", agent,
					"the stream registration did not read back as it was written");
		}
	}

	/**
	 * Project one committed ledger row into the agent's stream and verify it by reading it back.
	 * Returns the mismatches; an empty list means the row is the row that was written.
	 */
	public List<String> record(String agent, JsonObject entry) {
		int seq = entry.get("seq").getAsInt();
		String id = key(agent) + ":" + seq;
		Document document = Document.parse(entry.toString());
		document.put("_id", id);
		document.put("agent", agent);
		document.put("verified", false);
		events.replaceOne(Filters.eq("_id", id), document, new ReplaceOptions().upsert(true));

		Document back = events.find(Filters.eq("_id", id)).first();
		List<String> mismatches = new ArrayList<>();
		if (back == null) {
			mismatches.add("row " + seq + " was written and does not read back");
		} else {
			for (String field : List.of("agent", "seq", "kind", "outcome", "subject", "digest", "hash", "at")) {
				Object written = document.get(field);
				Object read = back.get(field);
				if (!Objects.equals(written, read)) {
					mismatches.add(field + ": wrote " + written + ", read " + read);
				}
			}
		}
		boolean verified = mismatches.isEmpty();
		events.updateOne(Filters.eq("_id", id), new Document("$set", new Document("verified", verified)));
		String at = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
		verifications.insertOne(new Document("agent", agent).append("seq", seq).append("id", id)
				.append("result", verified ? "PASS" : "FAIL").append("mismatches", mismatches)
				.append("at", at).append("uri", uri));
		if (!verified) {
			// The row is not served. The caller decides whether to refuse the append's notice; the
			// store's answer is only ever the mismatch that it can prove.
			return mismatches;
		}
		return List.of();
	}

	public boolean has(String agent) {
		return streams.find(Filters.eq("_id", key(agent))).first() != null;
	}

	public long count(String agent) {
		return events.countDocuments(Filters.and(Filters.eq("agent", agent), Filters.eq("verified", true)));
	}

	/** The agent's own verified tail, oldest of the last `limit` first. Another agent's rows cannot be here. */
	public List<Document> tail(String agent, int limit) {
		List<Document> rows = new ArrayList<>();
		events.find(Filters.and(Filters.eq("agent", agent), Filters.eq("verified", true)))
				.sort(Sorts.descending("seq")).limit(limit).into(rows);
		Collections.reverse(rows);
		return rows;
	}

	/**
	 * The tail the toast stack draws: a refusal or a mount change, not every control packet.
	 *
	 * <p>`test` rows used to be served here too, and a reader's client drew them as toasts whose
	 * title was the word "test" and whose body was the surface path — internal bookkeeping shown as
	 * news. A witness is the client's own claim about a payload it checked; it belongs in the ledger
	 * and in the client's log, not on the screen of whoever is watching the world.
	 */
	public List<Document> noticeTail(String agent, int limit) {
		List<Document> rows = new ArrayList<>();
		events.find(Filters.and(Filters.eq("agent", agent), Filters.eq("verified", true),
				Filters.or(Filters.eq("outcome", "rejected"), Filters.eq("kind", "mount"))))
				.sort(Sorts.descending("seq")).limit(limit).into(rows);
		Collections.reverse(rows);
		return rows;
	}

	public String uri() {
		return uri;
	}

	public String databaseName() {
		return database.getName();
	}

	@Override
	public void close() {
		client.close();
	}
}
