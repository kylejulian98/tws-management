package dev.kylejulian.twsmanagement.server;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LuckPermsHelper {

    private final JavaPlugin plugin;
    private final LuckPerms api;

    public LuckPermsHelper(@NotNull JavaPlugin plugin, @NotNull LuckPerms  api) {
        this.plugin = plugin;
        this.api = api;
    }

    public CompletableFuture<Boolean> hasPermissionAsync(UUID playerId, String node) {
        UserManager userManager = this.api.getUserManager();
        CompletableFuture<User> userFuture = userManager.loadUser(playerId);

        return userFuture.thenApplyAsync(user -> {
            if (user != null) {
                ContextManager contextManager = this.api.getContextManager();
                QueryOptions queryOptions = contextManager.getQueryOptions(user).orElse(contextManager.getStaticQueryOptions());

                CachedPermissionData userCachedPermissions = user.getCachedData().getPermissionData(queryOptions);
                return userCachedPermissions.checkPermission(node).asBoolean();
            }

            this.plugin.getLogger().log(Level.WARNING, "Unable to find Player [{0}] with the LuckPerms Api", playerId);
            return false;
        });
    }
}
