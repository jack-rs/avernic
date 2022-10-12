package script.npc.olm;

import lombok.Getter;
import lombok.Setter;
import osv.clip.Region;
import osv.clip.WorldObject;
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
 * Acid spit attack.
 * 
 * @author Vip3r
 * @version 28/05/2017
 */
@Getter
@Setter
public class AcidSpray extends WorldObject {
	
	private GreatOlm olm;

	/**
	 * Constructs a new {@link AcidSpray} {@code Object}.
	 * @param location
	 */
	public AcidSpray(GreatOlm olm, Location location) {
		super(30032, location.getX(), location.getY(), location.getZ(), 10, 0);
		Region.addAbstractWorldObject(this);
		for (Entity mob : olm.getPossibleTargets()) {
			if (mob.getLocation().equals(location) && Misc.random(10) > 6) {
				mob.appendDamage(6, Hitmark.VENOM);
			}
			((Player) mob).getPA().object(this);
		}
		WorldObject pool = this;
		CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[olm.spawnedBy], new CycleEvent() {

			int cycle = Misc.random(8, 12);
			
			@Override
			public void execute(CycleEventContainer container) {
				for (Entity mob : olm.getPossibleTargets()) {
					((Player) mob).getPA().object(AcidSpray.this);
					if (mob.getLocation().equals(location)) {
						mob.appendDamage(6, Hitmark.VENOM);
					}
				}
				if (cycle == 0) {
					Region.removeWorldObject(pool);
					for (Player player : olm.getAllNonSafePlayers()) {
						player.getPA().removeObject(pool.getX(), pool.getY());
					}
					stop();
					container.stop();
				}
				cycle--;
			}
		}, 4);
	}

	public GreatOlm getOlm() {
		return olm;
	}

}
