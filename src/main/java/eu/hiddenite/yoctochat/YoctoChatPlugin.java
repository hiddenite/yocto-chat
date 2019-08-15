package eu.hiddenite.yoctochat;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class YoctoChatPlugin extends Plugin implements Listener {
    private Configuration config = new Configuration();
    private HashMap<UUID, UUID> lastPrivateMessages = new HashMap<>();

    public Configuration getConfig() {
        return config;
    }

    @Override
    public void onEnable() {
        if (!config.load(this)) {
            return;
        }

        if (config.chatFormats.size() == 0) {
            getLogger().warning("No chat format found in the configuration.");
            return;
        }

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new PrivateMessageCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ReplyCommand(this));
    }

    @EventHandler
    public void onPlayerChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        if (event.getMessage().startsWith("/")) {
            return;
        }

        ProxiedPlayer sender = (ProxiedPlayer)event.getSender();
        Collection<ProxiedPlayer> allPlayers = getProxy().getPlayers();

        String chatFormat = getChatFormat(sender);

        String message = event.getMessage();
        String formattedMessage = chatFormat
                .replace("{NAME}", sender.getName())
                .replace("{NAME}", sender.getName())
                .replace("{DISPLAY_NAME}", sender.getDisplayName())
                .replace("{MESSAGE}", message);

        BaseComponent[] messageComponents = TextComponent.fromLegacyText(formattedMessage);

        getLogger().info(formattedMessage);
        allPlayers.forEach((receiver) -> {
            receiver.sendMessage(messageComponents);
        });

        event.setCancelled(true);
    }

    public void sendPrivateMessage(ProxiedPlayer sender, ProxiedPlayer receiver, String message) {
        lastPrivateMessages.put(receiver.getUniqueId(), sender.getUniqueId());

        String senderMessage = config.pmSentFormat
                .replace("{NAME}", receiver.getName())
                .replace("{DISPLAY_NAME}", receiver.getDisplayName())
                .replace("{MESSAGE}", message);
        String receiverMessage = config.pmReceivedFormat
                .replace("{NAME}", sender.getName())
                .replace("{DISPLAY_NAME}", sender.getDisplayName())
                .replace("{MESSAGE}", message);

        sender.sendMessage(TextComponent.fromLegacyText(senderMessage));
        receiver.sendMessage(TextComponent.fromLegacyText(receiverMessage));
    }

    public ProxiedPlayer getLastPrivateMessageSender(ProxiedPlayer player) {
        UUID lastSender = lastPrivateMessages.get(player.getUniqueId());
        if (lastSender != null) {
            return getProxy().getPlayer(lastSender);
        }
        return null;
    }

    private String getChatFormat(ProxiedPlayer player) {
        String last = null;
        for (String key : config.chatFormats.keySet()) {
            last = key;
            if (player.hasPermission("yoctochat." + key)) {
                return config.chatFormats.get(key);
            }
        }
        return config.chatFormats.get(last);
    }
}