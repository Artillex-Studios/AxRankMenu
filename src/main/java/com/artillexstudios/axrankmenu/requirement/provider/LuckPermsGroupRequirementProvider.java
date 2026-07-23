package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.NodeType;
import org.bukkit.entity.Player;

public final class LuckPermsGroupRequirementProvider implements RankRequirementProvider {
    @Override
    public String id() {
        return "luckperms-group";
    }

    @Override
    public RequirementResult evaluate(Player player, RequirementContext context) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        String group = context.string("group", context.string("value", ""));
        boolean expected = context.bool("has", true);
        boolean current = user != null && user.getNodes(NodeType.INHERITANCE).stream()
                .anyMatch(node -> node.getGroupName().equalsIgnoreCase(group) && node.getValue());
        boolean met = current == expected;
        return met
                ? RequirementResult.success(Boolean.toString(current), Boolean.toString(expected), "")
                : RequirementResult.failure("requirements.invalid-group", "LuckPerms group state did not match", Boolean.toString(current), Boolean.toString(expected), "");
    }
}
