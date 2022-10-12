package script.npc.olm;

import osv.clip.WorldObject;
import osv.clip.Region;
import osv.event.CycleEvent;
import osv.event.CycleEventContainer;
import osv.event.CycleEventHandler;
import osv.model.Location;
import osv.model.entity.Entity;
import osv.model.players.Player;
import osv.model.players.PlayerHandler;
import osv.model.players.combat.Hitmark;
import osv.util.Misc;

/**
 * crystal bomb attack.
 * 
 * @author Vip3r
 * @version 28/05/2017
 */
public class CrystalBomb extends WorldObject {
	
	private GreatOlm olm;

	/**
	 * Constructs a new {@link CrystalBomb} {@code Object}.
	 * @param location
	 */
	public CrystalBomb(GreatOlm olm, Location location) {
		super(29766, location.getX(), location.getY(), location.getZ(), 10, Misc.random(3));
		CrystalBomb bomb = this;
		for (Player player : olm.getAllNonSafePlayers()) {
			player.getPA().object(this);
		}
		CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[olm.spawnedBy], new CycleEvent() {

			
			@Override
			public void execute(CycleEventContainer container) {
				
				for (Entity mob : olm.getPossibleTargets()) {
					((Player) mob).getPA().object(CrystalBomb.this);
					((Player) mob).getPA().sendStillGraphics(1368, 0, location.getY(), location.getX(), 0);
					if (mob.getLocation().getDistance(location) <= 3) {
						mob.appendDamage(6, Hitmark.HIT);
					}
				}
				CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[olm.spawnedBy], new CycleEvent() {

					
					@Override
					public void execute(CycleEventContainer container) {
						Region.removeWorldObject(bomb);
						for (Player player : olm.getAllNonSafePlayers()) {
							player.getPA().removeObject(bomb.getX(), bomb.getY());
						}
						stop();
						container.stop();
					}
				}, 1);
				container.stop();
				stop();
			}
		}, 10);
	}

	public GreatOlm getOlm() {
		return olm;
	}

}
