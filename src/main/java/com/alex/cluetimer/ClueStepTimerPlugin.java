package com.alex.cluetimer;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
		name = "Clue Step Timer",
		description = "Times each clue step from reading the clue to receiving the next clue or casket",
		tags = {"clue", "timer", "speedrun"}
)
public class ClueStepTimerPlugin extends Plugin
{
	private static final Set<Integer> CLUE_SCROLL_IDS = Set.of(
    	ItemID.CLUE_SCROLL_EASY,
    	ItemID.CLUE_SCROLL_MEDIUM,
    	ItemID.CLUE_SCROLL_HARD,
    	ItemID.CLUE_SCROLL_ELITE,
    	ItemID.CLUE_SCROLL_MASTER
);

private static final Set<Integer> CASKET_IDS = Set.of(
    	ItemID.CASKET_EASY,
    	ItemID.CASKET_MEDIUM,
    	ItemID.CASKET_HARD,
    	ItemID.CASKET_ELITE,
    	ItemID.CASKET_MASTER
);

	@Inject
	private Client client;

	private Instant stepStart;
	private int startClueCount;
	private int startCasketCount;

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption();
		if (option == null || (!option.equalsIgnoreCase("Read") && !option.equalsIgnoreCase("Open")))
		{
			return;
		}

		if (!CLUE_SCROLL_IDS.contains(event.getItemId()))
		{
			return;
		}

		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		if (inv == null)
		{
			return;
		}

		startClueCount = count(inv, CLUE_SCROLL_IDS);
		startCasketCount = count(inv, CASKET_IDS);
		stepStart = Instant.now();

		send("Clue step timer started");
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (stepStart == null || event.getContainerId() != InventoryID.INVENTORY.getId())
		{
			return;
		}

		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		if (inv == null)
		{
			return;
		}

		int clueNow = count(inv, CLUE_SCROLL_IDS);
		int casketNow = count(inv, CASKET_IDS);

		if (clueNow != startClueCount || casketNow > startCasketCount)
		{
			Duration d = Duration.between(stepStart, Instant.now());
			stepStart = null;

			send("Clue step time: " + format(d));
		}
	}

	private int count(ItemContainer inv, Set<Integer> ids)
	{
		int total = 0;
		for (int id : ids)
		{
			total += inv.count(id);
		}
		return total;
	}

	private void send(String msg)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[ClueTimer] " + msg, null);
	}

	private String format(Duration d)
	{
		return d.toSecondsPart() + "." + String.format("%03d", d.toMillisPart()) + "s";
	}
}
