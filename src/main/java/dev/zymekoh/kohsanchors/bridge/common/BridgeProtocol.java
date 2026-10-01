package dev.zymekoh.kohsanchors.bridge.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The language KoHs Anchor's and this bridge speak on the {@value #CHANNEL} plugin channel; a copy
 * of the mod's own {@code BridgeProtocol}, number for number.
 *
 * <p>Every message is one plugin message: a type byte and then its fields as {@link DataOutputStream}
 * writes them. HELLO, SETTINGS and PING come from the client; WELCOME, OWNER, PONG and POLICY from the
 * server.</p>
 */
public final class BridgeProtocol {
    public static final String NAMESPACE = "kohs_anchors";
    public static final String PATH = "bridge";
    public static final String CHANNEL = NAMESPACE + ":" + PATH;
    public static final int VERSION = 1;

    public static final byte HELLO = 1;
    public static final byte WELCOME = 2;
    public static final byte OWNER = 3;
    public static final byte PING = 4;
    public static final byte PONG = 5;
    public static final byte SETTINGS = 7;
    public static final byte POLICY = 8;

    public static final int POLICY_FAST_CHAIN = 1;
    public static final int POLICY_INSTANT_DETONATION = 1 << 1;
    public static final int POLICY_OWNERSHIP = 1 << 2;
    public static final int POLICY_LATENCY = 1 << 3;
    public static final int POLICY_ANTICHEAT = 1 << 4;
    /** The anchor chain: the two options that change when clicks reach the server. */
    public static final int POLICY_CHAIN = POLICY_FAST_CHAIN | POLICY_INSTANT_DETONATION;

    public static final byte OWNER_SELF = 1;
    public static final byte OWNER_OTHER = 2;

    private BridgeProtocol() {
    }

    public static byte[] message(byte type, Body body) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(32);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(type);
            body.write(out);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
        return bytes.toByteArray();
    }

    public static byte type(byte[] data) {
        return data.length == 0 ? 0 : data[0];
    }

    /** A reader positioned after the type byte. */
    public static DataInputStream reader(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        in.skipBytes(1);
        return in;
    }

    public static byte[] welcome(String bridgeVersion, String platform, int policy) {
        return message(WELCOME, out -> {
            out.writeInt(VERSION);
            out.writeUTF(bridgeVersion);
            out.writeUTF(platform);
            out.writeInt(policy);
        });
    }

    public static byte[] policy(int policy) {
        return message(POLICY, out -> out.writeInt(policy));
    }

    public static byte[] owner(long position, boolean self) {
        return message(OWNER, out -> {
            out.writeLong(position);
            out.writeByte(self ? OWNER_SELF : OWNER_OTHER);
        });
    }

    public static byte[] pong(int id) {
        return message(PONG, out -> out.writeInt(id));
    }

    /** Minecraft's own packing of a block position into a long (x 26 bits, z 26 bits, y 12 bits). */
    public static long packPosition(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) y & 0xFFFL;
    }

    /**
     * Rewrites the policy of a WELCOME or POLICY message with {@code mask} applied: a proxy keeps its
     * network's limits. Other messages come back unchanged.
     */
    public static byte[] capPolicy(byte[] data, int mask) {
        try {
            DataInputStream in = reader(data);
            byte type = type(data);
            if (type == WELCOME) {
                int protocol = in.readInt();
                String bridgeVersion = in.readUTF();
                String platform = in.readUTF();
                int policy = in.readInt() & mask;
                return message(WELCOME, out -> {
                    out.writeInt(protocol);
                    out.writeUTF(bridgeVersion);
                    out.writeUTF(platform);
                    out.writeInt(policy);
                });
            }
            if (type == POLICY) {
                return policy(in.readInt() & mask);
            }
        } catch (IOException malformed) {
            // Not a message this version reads: passed on as it came.
        }
        return data;
    }

    @FunctionalInterface
    public interface Body {
        void write(DataOutputStream out) throws IOException;
    }
}
