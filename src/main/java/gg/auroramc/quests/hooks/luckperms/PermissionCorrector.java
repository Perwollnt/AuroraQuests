package gg.auroramc.quests.hooks.luckperms;

import gg.auroramc.aurora.api.reward.PermissionReward;
import gg.auroramc.aurora.api.reward.RewardCorrector;
import gg.auroramc.quests.AuroraQuests;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.util.Tristate;
import org.bukkit.entity.Player;

import java.util.List;

public class PermissionCorrector implements RewardCorrector {
    @Override
    public void correctRewards(Player player) {
        var plugin = AuroraQuests.getInstance();
        var manager = plugin.getQuestManager();

        for (var pool : manager.getQuestPools()) {
            // Correct global quests
            if (pool.isGlobal()) {
                for (var quest : pool.getQuests()) {
                    if (!quest.isCompleted(player)) continue;

                    for (var reward : quest.getRewards().values()) {
                        if (reward instanceof PermissionReward permissionReward) {
                            if (permissionReward.getPermissions() == null || permissionReward.getPermissions().isEmpty())
                                continue;
                            var nodes = permissionReward.buildNodes(player, quest.getPlaceholders(player));
                            updatePermissionNodes(player, nodes);
                        }
                    }
                }
            }

            // Correct quest pool leveling
            if (!pool.hasLeveling()) return;
            var level = pool.getPlayerLevel(player);

            for (int i = 1; i < level + 1; i++) {
                var matcher = pool.getMatcherManager().getBestMatcher(i);
                if (matcher == null) continue;
                var placeholders = pool.getLevelPlaceholders(player, i);
                for (var reward : matcher.computeRewards(i)) {
                    if (reward instanceof PermissionReward permissionReward) {
                        if (permissionReward.getPermissions() == null || permissionReward.getPermissions().isEmpty())
                            continue;

                        var nodes = permissionReward.buildNodes(player, placeholders);

                        updatePermissionNodes(player, nodes);

                    }
                }
            }
        }
    }

    private void updatePermissionNodes(Player player, List<Node> nodes) {
        LuckPermsProvider.get().getUserManager().modifyUser(player.getUniqueId(), user -> {
            for (var node : nodes) {
                var hasPermission = user.data().contains(node, NodeEqualityPredicate.EXACT);

                if (hasPermission.equals(Tristate.UNDEFINED)) {
                    AuroraQuests.logger().debug("Permission " + node.getKey() + " is undefined for player " + player.getName());
                    user.data().add(node);
                }
            }
        });
    }
}
