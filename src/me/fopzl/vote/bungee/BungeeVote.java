package me.fopzl.vote.bungee;

import java.io.EOFException;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import me.neoblade298.bungeecore.commands.*;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeVote extends Plugin implements Listener
{
    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new CmdHub());
        getProxy().getPluginManager().registerCommand(this, new CmdTp());
        getProxy().getPluginManager().registerCommand(this, new CmdTphere());
        getProxy().getPluginManager().registerCommand(this, new CmdUptime());
        getProxy().getPluginManager().registerCommand(this, new CmdSendAll());
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("neocore:bungee");
    }
    
    @EventHandler
    public void onBungeeMessage(PluginMessageEvent e) {
		ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
		try {
			if (in.readUTF().equals("NeoCore")) {
				if (in.readUTF().equals("cmd")) {
					getProxy().getPluginManager().dispatchCommand(getProxy().getConsole(), in.readUTF());
				}
			}
		} catch (Exception ex) {	}
    }
}
