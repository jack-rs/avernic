package script.npc.olm;

import osv.clip.WorldObject;
import osv.clip.Region;
import osv.event.CycleEvent;
import osv.event.CycleEventContainer;
import osv.event.CycleEventHandler;
import osv.model.Location;
import osv.model.npcs.NPC;
import osv.model.players.Player;
import osv.model.players.combat.Hitmark;
import osv.util.Misc;

/**
 * OLM claw.
 * 
 * @author Vip3r
 * @version 26/05/2017
 */
public class OlmClaw extends NPC {
	
	private static final int FLINCH = 7360;
	
	private GreatOlm olm;

	private WorldObject render;
	
	private boolean flinching;
	
	private int flinchingTicks;
	
	private long nextFlinch, nextAttackCycle;
	
	private ClawAttack attack;
	
	/**
	 * Olm claw.
	 * 
	 * @param id
	 * 
	 * @param location
	 */
	public OlmClaw(GreatOlm olm, int id, Location location, WorldObject render) {
		super(id, location);
		this.olm = olm;
		this.render = render;
		Region.addAbstractWorldObject(render);
		height = location.getZ();
		walkingType = 0;
		getHealth().setMaximum(olm.handHealth);
		getHealth().reset();
		maxHit = 0;
		super.attack = 0;
		defence = 0;
	}

	/**
	 * @return the render
	 */
	public WorldObject getRender() {
		return render;
	}
	
	@Override
	public int appendDamage(Player player, int damage, Hitmark h) {
		if (!olm.getRightClaw().isDead && nextFlinch < System.currentTimeMillis() && npcType == 7555 && !flinching && (health.getAmount() < (double) health.getMaximum() * 0.9)) {
			flinch();
		}
		if (flinching) {
			player.sendMessage("The Great Olm's left claw clenches to protect itself temporarily.");
			return 0;
		}
		int rt = super.appendDamage(player, damage, h);
		player.raidParty.addPoints(player, rt);
		if (isDead || getHealth().getAmount() == 0) {
			render(npcType == 7553 ? 7352 : 7360);
			CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {

				@Override
				public void execute(CycleEventContainer container) {
					Region.removeWorldObject(render);
					for (Player player : olm.getAllNonSafePlayers()) {
						player.getPA().removeObject(render.getX(), render.getY());
					}
					render.setId(npcType == 7555 ? 29883 : 29886);
					Region.addAbstractWorldObject(render);
					olm.killed(OlmClaw.this);
					for (Player player : olm.getAllNonSafePlayers()) {
						player.getPA().object(render);
					}
					stop();
					container.stop();
				}
			}, 2);
		}
		return rt;
	}
	
	private void flinch() {
		flinching = true;
		render(FLINCH);
		flinchingTicks = Misc.random(50, 80);
		nextFlinch = System.currentTimeMillis() + (flinchingTicks * 600) + (Misc.random(34, 50) * 600);
		CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {

			@Override
			public void execute(CycleEventContainer container) {
				render(7361);
				stop();
				container.stop();
			}
		}, 2);
	}

	@Override
	public void tick() {
		if (flinchingTicks > 0 && !olm.getRightClaw().isDead) {
			flinchingTicks--;
			if (flinchingTicks == 0) {
				unflinch();
			}
		} else if (olm.getRightClaw().isDead && flinching) {
			unflinch();
		}
		if (!flinching && getNextAttackCycle() != 0 && getNextAttackCycle() < System.currentTimeMillis()) {
			startAttack();
		}
	}

	private void startAttack() {
		if (isDead) {
			return;
		}
		setNextAttackCycle(System.currentTimeMillis() + (attack == ClawAttack.SWAP ? (Misc.random(15000, 35000)) : attack == ClawAttack.LIGHTNING ? 11000 : (Misc.random(10000, 15000))));
		attack = attack == null || attack == ClawAttack.SWAP ? ClawAttack.CRYSTAL_BURST :  attack == ClawAttack.CRYSTAL_BURST ? ClawAttack.LIGHTNING : ClawAttack.SWAP;
		render(attack.getAnimation());
		attack.attack(olm);
		CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {

			@Override
			public void execute(CycleEventContainer container) {
				render(7355);
				container.stop();
				stop();
			}
		}, 4);
	}

	public void unflinch() {
		flinching = false;
		render(7362);
		CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {

			@Override
			public void execute(CycleEventContainer container) {
				flinching = false;
				render(7355);
				stop();
			}
		}, 2);
	}

	/**
	 * @param render the render to set
	 */
	public void setRender(WorldObject render) {
		this.render = render;
	}

	public void render(int anim) {
//		forceChat("Render " + anim);
		if (flinching) {
			anim = 7361;
		}
		if (this == olm.getLeftClaw() && olm.getRightClaw().isDead && anim != 7362) {
			anim += 10;
		}
		if (flinching && anim != FLINCH) {
			anim = 7361;
		}
		if (isDead) {
			if (anim != 7352 && anim != 7360 && !olm.isSwitching()) {
				return;
			}
		}
		for (Player player : olm.getAllSafePlayers()) {
			if (player == null) {
				continue;
			}
			player.getPA().animateObject(render, anim);
		}
	}

	public void update() {
		Region.removeWorldObject(render);
		for (Player player : olm.getAllSafePlayers()) {
			player.getPA().removeObject(render.getX(), render.getY());;
		}
		render.setId(render.getId() + 1);
		Region.addAbstractWorldObject(render);
		for (Player player : olm.getAllSafePlayers()) {
			player.getPA().object(render);
		}
	}

	/**
	 * @return the flinching
	 */
	public boolean isFlinching() {
		return flinching;
	}

	/**
	 * @param flinching the flinching to set
	 */
	public void setFlinching(boolean flinching) {
		this.flinching = flinching;
	}

	/**
	 * @return the flinchingTicks
	 */
	public int getFlinchingTicks() {
		return flinchingTicks;
	}

	/**
	 * @param flinchingTicks the flinchingTicks to set
	 */
	public void setFlinchingTicks(int flinchingTicks) {
		this.flinchingTicks = flinchingTicks;
	}

	/**
	 * @return the lastFlinch
	 */
	public long getNextFlinch() {
		return nextFlinch;
	}

	/**
	 * @param lastFlinch the lastFlinch to set
	 */
	public void setNextFlinch(long lastFlinch) {
		this.nextFlinch = lastFlinch;
	}

	public long getNextAttackCycle() {
		return nextAttackCycle;
	}

	public void setNextAttackCycle(long nextAttackCycle) {
		this.nextAttackCycle = nextAttackCycle;
	}

	public ClawAttack getAttack() {
		return attack;
	}

	public void setAttack(ClawAttack attack) {
		this.attack = attack;
	}

}
