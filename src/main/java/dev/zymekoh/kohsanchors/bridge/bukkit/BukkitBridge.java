package dev.zymekoh.kohsanchors.bridge.bukkit;

import dev.zymekoh.kohsanchors.bridge.common.BridgeProtocol;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * KoHs Anchor's Bridge on Bukkit, Spigot, Paper, Purpur and Folia: the server's side of the mod's
 * Anchors Server tab.
 *
 * <ul>
 *   <li>Answers the mod's hello with this server's policy (config.yml): which anchor options are
 *   allowed here. Players without the mod never hear from it.</li>
 *   <li>Better glow enemy anchors: when a respawn anchor is placed, every bridged player in that world
 *   is told whether it is theirs or someone else's.</li>
 *   <li>Real latency: answers the mod's pings at once.</li>
 *   <li>With the anchor chain allowed and Grim installed, tells Grim through its API that the chain is
 *   allowed here ({@link GrimCooperation}).</li>
 * </ul>
 *
 * <p>It never changes a block, an item, an attack or a movement: the server keeps deciding everything.
 * Every handler only reads events and sends plugin messages, so it is safe on Folia's region threads.</p>
 */
public final class BukkitBridge extends JavaPlugin implements Listener, PluginMessageListener {
    /** A player of the mod who said hello: what they use and where they are. */
    private record Session(int protocol, String modVersion, int wanted, UUID world) {
        Session withWanted(int bits) {
            return new Session(this.protocol, this.modVersion, bits, this.world);
        }

        Session inWorld(UUID next) {
            return new Session(this.protocol, this.modVersion, this.wanted, next);
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAnchorAction = new ConcurrentHashMap<>();
    private int policy;
    private GrimCooperation grim;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPolicy();
        getServer().getMessenger().registerIncomingPluginChannel(this, BridgeProtocol.CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, BridgeProtocol.CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);
        this.grim = GrimCooperation.hook(this);
        getLogger().info("KoHs Anchor's Bridge " + getDescription().getVersion() + " on " + platform() + ": policy "
                + describe(this.policy) + (this.grim != null ? " · Grim cooperation ready" : ""));
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.sessions.clear();
    }

    /** The policy from config.yml, as the mod's policy bits. */
    private void loadPolicy() {
        reloadConfig();
        int bits = 0;
        if (getConfig().getBoolean("anchor-chain.enabled", false)) {
            if (getConfig().getBoolean("anchor-chain.fast-chain", true)) {
                bits |= BridgeProtocol.POLICY_FAST_CHAIN;
            }
            if (getConfig().getBoolean("anchor-chain.instant-detonation", true)) {
                bits |= BridgeProtocol.POLICY_INSTANT_DETONATION;
            }
        }
        if (getConfig().getBoolean("better-enemy-glow", true)) {
            bits |= BridgeProtocol.POLICY_OWNERSHIP;
        }
        if (getConfig().getBoolean("real-latency", true)) {
            bits |= BridgeProtocol.POLICY_LATENCY;
        }
        if ((bits & BridgeProtocol.POLICY_CHAIN) != 0 && getConfig().getBoolean("anchor-chain.grim-cooperation", true)
                && Bukkit.getPluginManager().getPlugin("GrimAC") != null) {
            bits |= BridgeProtocol.POLICY_ANTICHEAT;
        }
        this.policy = bits;
    }

    int policy() {
        return this.policy;
    }

    long cooperationWindowMillis() {
        return Math.max(50L, getConfig().getLong("anchor-chain.grim-window-ms", 300L));
    }

    java.util.List<String> cooperationChecks() {
        return getConfig().getStringList("anchor-chain.grim-checks");
    }

    /**
     * Whether Grim may be told to let a flag of this player pass: the anchor chain is allowed here,
     * the player's KoHs Anchor's uses it, and the player acted on an anchor within the window.
     */
    boolean chainAllowedNow(UUID player) {
        Session session = this.sessions.get(player);
        if (session == null || (this.policy & BridgeProtocol.POLICY_ANTICHEAT) == 0) {
            return false;
        }
        int chain = session.wanted() & this.policy & BridgeProtocol.POLICY_CHAIN;
        Long at = this.lastAnchorAction.get(player);
        return chain != 0 && at != null && System.currentTimeMillis() - at <= cooperationWindowMillis();
    }

    private static String platform() {
        return Bukkit.getName() + " " + Bukkit.getBukkitVersion().replace("-R0.1-SNAPSHOT", "");
    }

    private static String describe(int bits) {
        StringBuilder text = new StringBuilder();
        if ((bits & BridgeProtocol.POLICY_FAST_CHAIN) != 0) {
            text.append("fast-chain ");
        }
        if ((bits & BridgeProtocol.POLICY_INSTANT_DETONATION) != 0) {
            text.append("instant-detonation ");
        }
        if ((bits & BridgeProtocol.POLICY_OWNERSHIP) != 0) {
            text.append("better-enemy-glow ");
        }
        if ((bits & BridgeProtocol.POLICY_LATENCY) != 0) {
            text.append("real-latency ");
        }
        if ((bits & BridgeProtocol.POLICY_ANTICHEAT) != 0) {
            text.append("grim-cooperation ");
        }
        return text.length() == 0 ? "nothing" : text.toString().trim();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!BridgeProtocol.CHANNEL.equals(channel)) {
            return;
        }
        try {
            DataInputStream in = BridgeProtocol.reader(message);
            switch (BridgeProtocol.type(message)) {
                case BridgeProtocol.HELLO -> {
                    int protocol = in.readInt();
                    String modVersion = in.readUTF();
                    this.sessions.put(player.getUniqueId(), new Session(protocol, modVersion, 0, player.getWorld().getUID()));
                    send(player, BridgeProtocol.welcome(getDescription().getVersion(), platform(), playerPolicy(player)));
                }
                case BridgeProtocol.SETTINGS -> {
                    int bits = in.readInt();
                    this.sessions.computeIfPresent(player.getUniqueId(), (id, session) -> session.withWanted(bits));
                }
                case BridgeProtocol.PING -> {
                    if ((this.policy & BridgeProtocol.POLICY_LATENCY) != 0) {
                        send(player, BridgeProtocol.pong(in.readInt()));
                    }
                }
                default -> {
                    // A message of a newer mod: nothing this version understands.
                }
            }
        } catch (IOException malformed) {
            // Not a message of the mod's: ignored.
        }
    }

    /** The policy for one player: the anchor chain needs the kohsanchors.bridge.chain permission. */
    private int playerPolicy(Player player) {
        return player.hasPermission("kohsanchors.bridge.chain") ? this.policy : this.policy & ~BridgeProtocol.POLICY_CHAIN;
    }

    private void send(Player player, byte[] message) {
        if (player.isOnline()) {
            player.sendPluginMessage(this, BridgeProtocol.CHANNEL, message);
        }
    }

    /** Better glow enemy anchors: who placed the anchor, to every bridged player in that world. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Material type = block.getType();
        UUID placer = event.getPlayer().getUniqueId();
        if (type == Material.RESPAWN_ANCHOR || type == Material.GLOWSTONE) {
            this.lastAnchorAction.put(placer, System.currentTimeMillis());
        }
        if (type != Material.RESPAWN_ANCHOR || (this.policy & BridgeProtocol.POLICY_OWNERSHIP) == 0 || this.sessions.isEmpty()) {
            return;
        }
        long position = BridgeProtocol.packPosition(block.getX(), block.getY(), block.getZ());
        UUID world = block.getWorld().getUID();
        for (Map.Entry<UUID, Session> entry : this.sessions.entrySet()) {
            Session session = entry.getValue();
            if (!world.equals(session.world()) || (session.wanted() & BridgeProtocol.POLICY_OWNERSHIP) == 0) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                send(player, BridgeProtocol.owner(position, entry.getKey().equals(placer)));
            }
        }
    }

    /** A click on a respawn anchor: the start of the window Grim cooperation covers. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && event.getClickedBlock().getType() == Material.RESPAWN_ANCHOR) {
            this.lastAnchorAction.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // A player reconnecting says hello again; nothing is kept from before.
        this.sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.sessions.remove(event.getPlayer().getUniqueId());
        this.lastAnchorAction.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        UUID world = event.getPlayer().getWorld().getUID();
        this.sessions.computeIfPresent(event.getPlayer().getUniqueId(), (id, session) -> session.inWorld(world));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("reload")) {
            loadPolicy();
            for (Map.Entry<UUID, Session> entry : this.sessions.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    send(player, BridgeProtocol.policy(playerPolicy(player)));
                }
            }
            sender.sendMessage("KoHs Anchor's Bridge reloaded: " + describe(this.policy));
            return true;
        }
        sender.sendMessage("KoHs Anchor's Bridge " + getDescription().getVersion() + " · " + platform());
        sender.sendMessage("Policy: " + describe(this.policy));
        sender.sendMessage("Players with KoHs Anchor's: " + this.sessions.size()
                + (this.grim != null ? " · Grim cooperation: " + this.grim.passed() + " flags let through" : ""));
        return true;
    }
}
