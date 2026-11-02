package dev.ensouled.body;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;

/**
 * A connection that never touches a socket. The vanilla server player path expects a
 * ClientConnection and a ServerPlayNetworkHandler; this adapter keeps the player object queryable
 * and drops every outbound packet instead of pretending a client exists.
 */
public final class FakeClientConnection extends ClientConnection {
	private static final SocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 0);

	private boolean open = true;

	public FakeClientConnection() {
		super(NetworkSide.SERVERBOUND);
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public boolean isChannelAbsent() {
		return false;
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public SocketAddress getAddress() {
		return ADDRESS;
	}

	@Override
	public void send(Packet<?> packet) {
		// No client to receive it. Deliberately silent: the body is server-side only.
	}

	@Override
	public void send(Packet<?> packet, PacketCallbacks callbacks) {
	}

	@Override
	public void tick() {
	}

	@Override
	public void disconnect(Text disconnectReason) {
		open = false;
	}
}
