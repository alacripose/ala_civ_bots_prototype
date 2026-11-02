package dev.ensouled.surface;

/** A refusal in the shipped vocabulary: a code, a subject and a reason. Never a new word. */
public final class SurfaceRefusal extends RuntimeException {
	public final String code;
	public final String subject;
	public final String why;

	public SurfaceRefusal(String code, String subject, String why) {
		super(code + ": " + subject + ": " + why);
		this.code = code;
		this.subject = subject;
		this.why = why;
	}
}
