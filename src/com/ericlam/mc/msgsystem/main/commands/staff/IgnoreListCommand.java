package com.ericlam.mc.msgsystem.main.commands.staff;

import com.ericlam.mc.bungee.hnmc.commands.caxerx.CommandNode;
import com.ericlam.mc.msgsystem.main.commands.MSGSystemCommandNode;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;

public class IgnoreListCommand extends MSGSystemCommandNode {
    public IgnoreListCommand(CommandNode parent, String command, String permission, String description, String placeholder, String... alias) {
        super(parent, command, permission, description, placeholder, alias);
    }

    @Override
    public void executionPlayer(ProxiedPlayer player, List<String> list) {

    }

    @Override
    public List<String> executeTabCompletion(CommandSender commandSender, List<String> list) {
        return null;
    }
}
