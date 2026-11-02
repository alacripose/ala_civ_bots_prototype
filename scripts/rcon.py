#!/usr/bin/env python3
"""Minimal RCON client for the local dev server (DEV-01 attach control channel)."""
import socket
import struct
import sys


def send_packet(sock, request_id, kind, body):
    payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(payload)) + payload)


def recv_exact(sock, count):
    data = b""
    while len(data) < count:
        chunk = sock.recv(count - len(data))
        if not chunk:
            raise ConnectionError("connection closed by server")
        data += chunk
    return data


def read_packet(sock):
    (length,) = struct.unpack("<i", recv_exact(sock, 4))
    data = recv_exact(sock, length)
    request_id, kind = struct.unpack("<ii", data[:8])
    return request_id, kind, data[8:-2].decode("utf-8", "replace")


def main():
    if len(sys.argv) != 5:
        print("usage: rcon.py <host> <port> <password> <command>", file=sys.stderr)
        return 2
    host, port, password, command = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
    with socket.create_connection((host, port), timeout=10) as sock:
        sock.settimeout(10)
        send_packet(sock, 1, 3, password)
        request_id, _, _ = read_packet(sock)
        if request_id == -1:
            print("rcon authentication failed", file=sys.stderr)
            return 3
        send_packet(sock, 2, 2, command)
        chunks = []
        while True:
            try:
                request_id, kind, body = read_packet(sock)
            except socket.timeout:
                break
            if kind == 0:
                chunks.append(body)
            if request_id == 2:
                break
        text = "".join(chunks).strip()
        if text:
            print(text)
        return 0


if __name__ == "__main__":
    sys.exit(main())
