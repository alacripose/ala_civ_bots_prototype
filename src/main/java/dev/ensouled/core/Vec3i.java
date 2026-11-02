package dev.ensouled.core;

/** Immutable block coordinate shared by sensors, skills and the motor layer. */
public record Vec3i(int x, int y, int z) {
	public Vec3i offset(int dx, int dy, int dz) {
		return new Vec3i(x + dx, y + dy, z + dz);
	}

	public Vec3i above() {
		return offset(0, 1, 0);
	}

	public double distanceTo(Vec3i other) {
		double dx = this.x - other.x;
		double dy = this.y - other.y;
		double dz = this.z - other.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public long squaredDistanceTo(Vec3i other) {
		long dx = this.x - other.x;
		long dy = this.y - other.y;
		long dz = this.z - other.z;
		return dx * dx + dy * dy + dz * dz;
	}

	@Override
	public String toString() {
		return x + "," + y + "," + z;
	}
}
