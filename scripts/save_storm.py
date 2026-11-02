#!/usr/bin/env python3
"""Hammer /ensouled save over the local RCON channel so a hard kill can land inside a snapshot.

Not a test harness: it is the operator command, repeated, so the server is genuinely writing a
generation when the process is terminated.
"""
import socket
import struct
import sys
import time


def send_packet(sock, request_id, kind, body):
    payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(payload)) + payload)


def recv_exact(sock, count):
    data = b""
    while len(data) < count:
        chunk = sock.recv(count - len(data))
        if not chunk:
            raise ConnectionError("closed")
        data += chunk
    return data


def read_packet(sock):
    (length,) = struct.unpack("<i", recv_exact(sock, 4))
    data = recv_exact(sock, length)
    request_id, kind = struct.unpack("<ii", data[:8])
    return request_id, kind, data[8:-2].decode("utf-8", "replace")


def main():
    port = int(sys.argv[1])
    password = sys.argv[2]
    deadline = time.time() + float(sys.argv[3]) if len(sys.argv) > 3 else time.time() + 30
    sent = 0
    sock = socket.create_connection(("127.0.0.1", port), timeout=15)
    sock.settimeout(15)
    send_packet(sock, 1, 3, password)
    read_packet(sock)
    request = 10
    while time.time() < deadline:
        request += 1
        send_packet(sock, request, 2, "ensouled save")
        while True:
            rid, kind, body = read_packet(sock)
            if rid == request:
                break
        sent += 1
    print(f"save storm: {sent} snapshot request(s)")
    sock.close()


if __name__ == "__main__":
    main()
