package dev.zymekoh.kohsanchors.bridge.bukkit;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Grim, told by the server's own admin that the anchor chain is allowed here.
 *
 * <p>When the admin allows the anchor chain (config.yml), a player whose KoHs Anchor's uses it sends
 * clicks on an exploding anchor without waiting, or a detonation between ticks. Grim can read those
 * as an impossible placement or packet order. Through Grim's own API (its event bus, version 1.6 and
 * later), a flag of one of the configured checks is let through, only:</p>
 * <ul>
 *   <li>for a player who said hello with KoHs Anchor's and switched the chain on,</li>
 *   <li>within the configured window after that player acted on a respawn anchor (placed one or
 *   glowstone, or clicked one),</li>
 *   <li>and for the checks the admin listed; every other check, and every other moment, is Grim's as
 *   usual.</li>
 * </ul>
 * <p>Reflection only: the bridge needs no Grim at build time, and without it nothing here loads.</p>
 */
final class GrimCooperation {
    private final BukkitBridge bridge;
    private final Set<String> checks = new HashSet<>();
    private final AtomicInteger passed = new AtomicInteger();
    private Method getUser;
    private Method getCheck;
    private Method setCancelled;
    private Method getUniqueId;
    private Method getCheckName;

    private GrimCooperation(BukkitBridge bridge) {
        this.bridge = bridge;
        for (String check : bridge.cooperationChecks()) {
            this.checks.add(check.trim());
        }
    }

    /** Subscribes to Grim's flags when Grim is installed and its API answers; null otherwise. */
    static GrimCooperation hook(BukkitBridge bridge) {
        Plugin grim = Bukkit.getPluginManager().getPlugin("GrimAC");
        if (grim == null) {
            return null;
        }
        try {
            ClassLoader loader = grim.getClass().getClassLoader();
            Class<?> provider = Class.forName("ac.grim.grimac.api.GrimAPIProvider", true, loader);
            Class<?> apiType = Class.forName("ac.grim.grimac.api.GrimAbstractAPI", true, loader);
            Class<?> busType = Class.forName("ac.grim.grimac.api.event.EventBus", true, loader);
            Class<?> flagType = Class.forName("ac.grim.grimac.api.event.events.FlagEvent", true, loader);
            Class<?> listenerType = Class.forName("ac.grim.grimac.api.event.GrimEventListener", true, loader);
            Object api = provider.getMethod("get").invoke(null);
            Object bus = apiType.getMethod("getEventBus").invoke(api);
            GrimCooperation cooperation = new GrimCooperation(bridge);
            Object listener = Proxy.newProxyInstance(loader, new Class<?>[] {listenerType}, (proxy, method, args) -> switch (method.getName()) {
                case "handle" -> {
                    cooperation.onFlag(args[0]);
                    yield null;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "KoHs Anchor's Bridge";
                default -> null;
            });
            busType.getMethod("subscribe", Object.class, Class.class, listenerType).invoke(bus, bridge, flagType, listener);
            return cooperation;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            bridge.getLogger().warning("Grim is installed but its API could not be used, so it is not told about the anchor chain: "
                    + unavailable);
            return null;
        }
    }

    int passed() {
        return this.passed.get();
    }

    private void onFlag(Object event) {
        try {
            if (this.getUser == null) {
                this.getUser = event.getClass().getMethod("getUser");
                this.getCheck = event.getClass().getMethod("getCheck");
                this.setCancelled = event.getClass().getMethod("setCancelled", boolean.class);
            }
            Object user = this.getUser.invoke(event);
            if (this.getUniqueId == null) {
                this.getUniqueId = user.getClass().getMethod("getUniqueId");
            }
            UUID player = (UUID) this.getUniqueId.invoke(user);
            if (!this.bridge.chainAllowedNow(player)) {
                return;
            }
            Object check = this.getCheck.invoke(event);
            if (this.getCheckName == null) {
                this.getCheckName = check.getClass().getMethod("getCheckName");
            }
            String name = (String) this.getCheckName.invoke(check);
            if (!this.checks.contains(name)) {
                return;
            }
            this.setCancelled.invoke(event, true);
            this.passed.incrementAndGet();
        } catch (ReflectiveOperationException | RuntimeException unexpected) {
            // A Grim this version does not read: its flag stands.
        }
    }
}
