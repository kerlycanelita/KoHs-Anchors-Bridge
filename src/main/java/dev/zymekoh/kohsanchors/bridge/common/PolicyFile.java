package dev.zymekoh.kohsanchors.bridge.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * A proxy's policy file ({@code config.properties}): what the network turns off on every server,
 * whatever each server's own bridge allows. Written from the jar's default the first time.
 */
public final class PolicyFile {
    private PolicyFile() {
    }

    public interface Source {
        InputStream open() throws IOException;
    }

    /** The mask the proxy applies to every server's policy; all bits on when the file cannot be read. */
    public static int load(Path directory, Source defaults, Consumer<String> warn) {
        Path file = directory.resolve("config.properties");
        Properties properties = new Properties();
        try {
            Files.createDirectories(directory);
            if (!Files.exists(file)) {
                try (InputStream in = defaults.open()) {
                    if (in != null) {
                        Files.copy(in, file);
                    }
                }
            }
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            }
        } catch (IOException failed) {
            warn.accept("KoHs Anchor's Bridge: could not read " + file + ", each server's own policy stands: " + failed);
            return -1;
        }
        int mask = -1;
        if (!Boolean.parseBoolean(properties.getProperty("allow-anchor-chain", "true"))) {
            mask &= ~(BridgeProtocol.POLICY_CHAIN | BridgeProtocol.POLICY_ANTICHEAT);
        }
        if (!Boolean.parseBoolean(properties.getProperty("allow-better-enemy-glow", "true"))) {
            mask &= ~BridgeProtocol.POLICY_OWNERSHIP;
        }
        if (!Boolean.parseBoolean(properties.getProperty("allow-real-latency", "true"))) {
            mask &= ~BridgeProtocol.POLICY_LATENCY;
        }
        return mask;
    }

    public static String describe(int mask) {
        return "anchor chain " + ((mask & BridgeProtocol.POLICY_CHAIN) != 0 ? "as each server allows" : "off")
                + " · better enemy glow " + ((mask & BridgeProtocol.POLICY_OWNERSHIP) != 0 ? "as each server allows" : "off")
                + " · real latency " + ((mask & BridgeProtocol.POLICY_LATENCY) != 0 ? "as each server allows" : "off");
    }
}
