package dev.zymekoh.kohsanchors.bridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.zymekoh.kohsanchors.bridge.common.BridgeProtocol;
import dev.zymekoh.kohsanchors.bridge.common.PolicyFile;
import java.io.InputStream;
import java.nio.file.Path;
import org.slf4j.Logger;

/**
 * KoHs Anchor's Bridge on a Velocity proxy: the network's limits.
 *
 * <p>The bridge itself runs on each backend server (Paper, Purpur, Folia...), which knows who placed
 * each anchor. The proxy passes the channel through and applies the network's own policy on top:
 * whatever a backend allows, an option the proxy's config turns off is off on every server of the
 * network. It never answers for a backend that has no bridge.</p>
 */
public final class VelocityBridge {
    private static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.create(BridgeProtocol.NAMESPACE, BridgeProtocol.PATH);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path directory;
    private int mask = -1;

    @Inject
    public VelocityBridge(ProxyServer proxy, Logger logger, @DataDirectory Path directory) {
        this.proxy = proxy;
        this.logger = logger;
        this.directory = directory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        this.mask = PolicyFile.load(this.directory, this::defaults, message -> this.logger.warn(message));
        this.proxy.getChannelRegistrar().register(CHANNEL);
        this.logger.info("KoHs Anchor's Bridge (Velocity): network policy " + PolicyFile.describe(this.mask));
    }

    private InputStream defaults() {
        return VelocityBridge.class.getResourceAsStream("/proxy-config.properties");
    }

    @Subscribe
    public void onMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) {
            return;
        }
        if (event.getSource() instanceof ServerConnection && event.getTarget() instanceof Player player) {
            byte[] capped = BridgeProtocol.capPolicy(event.getData(), this.mask);
            if (capped != event.getData()) {
                // The backend's answer, with the network's limits on it.
                event.setResult(PluginMessageEvent.ForwardResult.handled());
                player.sendPluginMessage(CHANNEL, capped);
            }
        }
        // Everything else (the client's hello and pings, the backend's owners and pongs) goes through.
    }
}
