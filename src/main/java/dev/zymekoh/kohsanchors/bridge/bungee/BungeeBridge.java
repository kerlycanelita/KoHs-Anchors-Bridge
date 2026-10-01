package dev.zymekoh.kohsanchors.bridge.bungee;

import dev.zymekoh.kohsanchors.bridge.common.BridgeProtocol;
import dev.zymekoh.kohsanchors.bridge.common.PolicyFile;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

/**
 * KoHs Anchor's Bridge on a BungeeCord or Waterfall proxy: the network's limits, as on Velocity
 * (dev.zymekoh.kohsanchors.bridge.velocity.VelocityBridge). The bridge runs on each backend server; the proxy passes the channel
 * through and turns off, on every server of the network, what its own config turns off.
 */
public final class BungeeBridge extends Plugin implements Listener {
    private int mask = -1;

    @Override
    public void onEnable() {
        this.mask = PolicyFile.load(getDataFolder().toPath(),
                () -> BungeeBridge.class.getResourceAsStream("/proxy-config.properties"), message -> getLogger().warning(message));
        getProxy().registerChannel(BridgeProtocol.CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);
        getLogger().info("KoHs Anchor's Bridge (BungeeCord): network policy " + PolicyFile.describe(this.mask));
    }

    @EventHandler
    public void onMessage(PluginMessageEvent event) {
        if (!BridgeProtocol.CHANNEL.equals(event.getTag())) {
            return;
        }
        if (event.getSender() instanceof Server && event.getReceiver() instanceof ProxiedPlayer player) {
            byte[] capped = BridgeProtocol.capPolicy(event.getData(), this.mask);
            if (capped != event.getData()) {
                event.setCancelled(true);
                player.sendData(BridgeProtocol.CHANNEL, capped);
            }
        }
    }
}
