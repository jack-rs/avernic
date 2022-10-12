package script.npc.olm;

import osv.Server;
import osv.clip.Region;
import osv.clip.WorldObject;
import osv.event.CycleEvent;
import osv.event.CycleEventContainer;
import osv.event.CycleEventHandler;
import osv.model.Location;
import osv.model.npcs.NPC;
import osv.model.players.Player;
import osv.model.players.PlayerHandler;

/**
 * Olm fire wall.
 * 
 * @author Vip3r
 * @version 28/05/2017
 */
public class FireWall {
	
	private NPC render;

	private WorldObject clip;
	
	private GreatOlm olm;
	
	private int ticks;
	
	public FireWall(GreatOlm olm, Location location) {
		this.olm = olm;
		clip = new WorldObject(4766, location.getX(), location.getY(), location.getZ(), 10, 0);
		Region.addWorldObject(clip);
		for (Player player : olm.getAllNonSafePlayers()) {
			player.getPA().object(clip);
		}
		ticks = 11;
		CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[olm.spawnedBy], new CycleEvent() {

			@Override
			public void execute(CycleEventContainer container) {
				if (tick()) {
					stop();
					container.stop();
				}
			}
		}, 1);
		
	}

	public boolean tick() {
		ticks--;
		if (ticks == 0) {
			Region.removeWorldObject(clip);
			for (Player player : olm.getAllSafePlayers()) {
				player.getPA().removeObject(clip.getX(), clip.getY());
			}
			render.teleport(render.getLocation().transform(0, 0, 1));
			render.isDead = true;
			render.updateRequired = true;
		}
		return ticks == 0;
	}
	
	public void spawn(Location at) {
		render = Server.npcHandler.spawnNpc(PlayerHandler.players[olm.spawnedBy], 7558, at.getX(), at.getY(), at.getZ(), 0, 120, 7, 70, 70, false, false);
	}

}
