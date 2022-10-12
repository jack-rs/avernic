package script.npc.olm;

import osv.Server;
import osv.clip.Region;
import osv.clip.WorldObject;
import osv.event.CycleEvent;
import osv.event.CycleEventContainer;
import osv.event.CycleEventHandler;
import osv.model.Location;
import osv.model.Projectile;
import osv.model.entity.Entity;
import osv.model.npcs.MultiwayCombatScript;
import osv.model.npcs.NPC;
import osv.model.players.Boundary;
import osv.model.players.Player;
import osv.model.players.PlayerHandler;
import osv.model.players.combat.Hitmark;
import osv.model.players.combat.melee.CombatPrayer;
import osv.model.players.xeric.Party;
import osv.script.Script;
import osv.script.npc.AstuteNPC;
import osv.util.Misc;
import osv.util.area.Rectangle;
import osv.world.objects.GlobalObject;

import java.util.*;

/**
 * The great olm boss.
 * 
 * @author Vp3r
 * @version 25/05/2017
 */
public class GreatOlm extends AstuteNPC implements MultiwayCombatScript {
	
	//private static final int FACE_NORTH = 7337, FACE_CENTRE = 7336, FACE_SOUTH = 7338;
	
	private static final int AWAKEN = 7335, GRAPPLE_RIGHT = 7350, GRAPPLE_LEFT = 7354;
	
	private static final int GREAT_OLM = 29880, LEFT_CLAW = 29883, RIGHT_CLAW = 29886;
	
	private static final int DEFAULT_MAGC_PROJECTILE = 1339, DEFAULT_RANGED_PROJECTILE = 1340, FIRE_SHOT = 1347, BURN = 1349, ACID_SPRAY = 1354, CRYSTAL_BOMB = 1357;
	
	private List<OlmPhase> phases = new ArrayList<>(3);
	
	private OlmPhase currentPhase;
	
	private byte stage = -1, nextSwitch = 0;

	public static int olmStage = 0;

	private Rectangle[][] RADIUS;
	
	private boolean west;
	
	private int face = -1;
	
	private OlmClaw rightClaw, leftClaw;
	
	private WorldObject olm;
	
	private OlmAttack attack;
	
	private boolean ritualising, switching, bombs;
	
	private int render;
	
	private int ticks, lastHeal, flameTicks;
	
	private final int plane;
	
	private int attackDelay;

	public boolean gaveLoot = false;
	
	private Map<Player, Long> hitStamps = new HashMap<>();

	public GreatOlm() {
		plane = 0;
	}
	
	public GreatOlm(int id, Location location, int plane) {
		super(id, location);
		this.plane = plane;
		this.gaveLoot = false;
		setWest(Misc.random(10) > 5);
		RADIUS = new Rectangle[][] {
			{ new Rectangle(getBase().transform(25, 46, 0), getBase().transform(9, 36, 0)), new Rectangle(getBase().transform(25, 41, 0), getBase().transform(9, 30, 0)), new Rectangle(getBase().transform(25, 36, 0), getBase().transform(7, 25, 0)) },
			{ new Rectangle(getBase().transform(25, 46, 0), getBase().transform(9, 36, 0)), new Rectangle(getBase().transform(25, 41, 0), getBase().transform(9, 30, 0)), new Rectangle(getBase().transform(25, 36, 0), getBase().transform(7, 25, 0)) } };
			phases.add(OlmPhase.ACID);
			phases.add(OlmPhase.FLAME);
			phases.add(OlmPhase.CRYSTAL);
		Collections.shuffle(phases);
	}

	public int olmHealth;

	public int handHealth;
	
	@Override
	public Object getIdentifier() {
		return new Object[] { 7554 };
	}
	
	@Override
	public int getMovementType() {
		return 0;
	}

	@Override
	public int getHitCapacity() {
		return 45;
	}

	@Override
	public int getHealthCapacity() {
		return 1200;
	}
	
	@Override
	public int getAttackConstant() {
		return 160;
	}

	@Override
	public int getDefenceConstant() {
		return 220;
	}

	@Override
	public ArrayList<Entity> getPossibleTargets(int ratio, boolean players, boolean npcs) {  
		ArrayList<Entity> possibleTargets = new ArrayList<>();
		if (players) {
			for (Player player : PlayerHandler.players) {
				if (player == null) { 
					continue;
				}
				if (player == null || player.getLocation().getZ() != plane || player.getLocation().getRegionId() != getLocation().getRegionId() || player.getLocation().getY() < 5730) {
					continue;
				}
				if (player.isDead || !RADIUS[isWest() ? 0 : 1][getFace()].inside(player.getLocation())) {
					continue;
				}
				possibleTargets.add(player);
			}
		}
		return possibleTargets;
	}
	
	public AstuteNPC construct(int id, Location location) {
		return new GreatOlm(id, isWest() ? location.transform(6, 0, 0) : location.transform(18, 0, 0), -1);
	}
	
	public void render(int anim) {
//		forceChat("Rendering " + anim);
		for (Player player : getAllSafePlayers()) {
			player.getPA().animateObject(olm, anim);
		}
	}
	
	public Location getBase() {
		return new Location(3216, 5704, plane);
	}
	
	@Override
	public void tick() {
		if (isDead) {
			if (!gaveLoot) {
				Server.getGlobalObjects().add(new GlobalObject(-1, 3232, 5749, getPlane(), 0, 10, 300, 30018));
				Server.getGlobalObjects().add(new GlobalObject(30028, 3233, 5751, getPlane(), 0, 10, 300, 30027));
				for (Player player : getAllSafePlayers()) {
					Party party = player.raidParty;
					if (party == null) {
						player.sendMessage("You did not receive any loot because you are not a part of this team.");
						continue;
					}
					if (party.getPoints(player) < 100) {
						player.sendMessage("You did not do enough damage to be considered for a reward.");
						player.sendMessage("You needed to do " + (100 - party.getPoints(player)) + " more damage.");
						OlmInstanceManager.get().freeUpI(player.raidParty);
						player.raidParty = null;
						player.setSidebarInterface(7, 18128);
						if (!player.isDead())
							player.teleport(new Location(1256, 3562));
						party.resetPoints(player);
						continue;
					}
					/*GameItem[] loot = GreatOlmDropTable.getLoot(player, this);
					int lootChoice = Misc.random(loot.length);
					if (player.getItems().freeSlots() < 2) {
						int amount = Misc.random(loot[lootChoice].getAmount());
						if (amount == 0) {
							amount = 1;
						}
						player.getItems().sendItemToAnyTab(loot[lootChoice].getId(), amount);
						//player.sendMessage("@red@ You have received " + amount + "x of " + ItemDefinition.get(loot[lootChoice].getId()).getName());
						player.sendMessage("@red@You do not have any space in your inventory, your loot is added to your bank.");
					} else {
						int amount = Misc.random(loot[lootChoice].getAmount());
						if (amount == 0) {
							amount = 1;
						}
						player.getItems().addItem(loot[lootChoice].getId(), amount);
						//player.sendMessage("@red@ You have received " + amount + "x of " + ItemDefinition.get(loot[lootChoice].getId()).getName());
						player.sendMessage("@red@Your loot has been added to your inventory.");
					}*/
					player.getAttributes().remove("olm_burn");
					player.getPA().stopShake();
					player.hasOpenedChest = false;
					OlmInstanceManager.get().freeUpI(player.raidParty);
					player.raidParty = null;
					player.setSidebarInterface(7, 18128);
					player.raidCount += 1;
					player.sendMessage("<col=006600>The raid has concluded! You ended with "+party.getPoints(player)+" points.");
					player.sendMessage("<col=006600>You have now completed "+player.raidCount+" raids.");
					party.resetPoints(player);
				}
				olm.setId(GREAT_OLM);
				updateAllObjects();
				gaveLoot = true;
			}

			return;
		}
		for (Player player : getAllNonSafePlayers()) {
			if (player.getLocation().getY() > 5730 && player.getLocation().getY() < 5748) {
				if (player.getLocation().getX() <= 3225) {
					player.teleportNoHeight(player.getLocation().transform(3228 - player.getLocation().getX(), 0, 0));
					player.getCombat().resetPlayerAttack();
				}
				if (player.getLocation().getX() >= 3240) {
					player.teleportNoHeight(player.getLocation().transform(3237 - player.getLocation().getX(), 0, 0));
					player.getCombat().resetPlayerAttack();
				}
			}
		}
		setTicks(getTicks() + 1);
		if ((bombs || stage == 2) && ticks %5 == 0) {
			for (int i = 0; i < Misc.random(2, 6); i++)
			dropBomb();
		}
		if (ticks < 10 || isRitualising()) {
			return;
		}
		if (getTicks() %4 == 0) {
			checkFace(true);
		}
		if (getFlameTicks() > 0) {
			flameTicks--;
			if (flameTicks == 0) {
				int burnY = (int) getAttributes().get("flame_wall_y");
				for (Entity mob : getPossibleTargets()) {
					Player target = (Player) mob;
					if (target.getLocation().getY() == burnY) {
						target.appendDamage(Misc.random(35, 50), Hitmark.HIT);
						target.sendMessage("You are scorched by the imbued flames.");
					}
				}
				getAttributes().remove("flame_wall");
			}
		}
		if (!getPossibleTargets().isEmpty()) {
            attack();
        }
		if ((getRightClaw().isDead && getLeftClaw().isDead) || getHealth().getAmount() == getHealth().getMaximum()) {
			return;
		}
		if (lastHeal > 0 && ticks > 15)
		lastHeal--;
		if (lastHeal <= 0 && (!getRightClaw().isDead || !getLeftClaw().isDead)
				&& (getHealth().getAmount() < getHealth().getMaximum())) {
			GreatOlm.this.appendDamage(getHealth().getMaximum(), Hitmark.HEAL_PURPLE);
			lastHeal = 10;
		}	
	}
	
	@Override
	public void appendDamage(int damage, Hitmark hitmark) {
		super.appendDamage(damage, hitmark);
		if (isDead || health.getAmount() == 0) {
			render(7348);
			for (Player player : getAllNonSafePlayers()) {
				player.getPA().stopShake();
			}
		}
	}
	
	private void checkFace(boolean render) {
		int face = 1, count = count(1);
		if (count(0) > count) {
			face = 0;
		} else if (count(2) > count) {
			face = 2;
		}
//		forceChat(count(0) + ", " + count(1) + ", " + count(2) + " -- " + face);
		if (render)
			face(face);
		else
			this.face = face;
	}
	
	private int count(int side) {
		int count = 0;
		Rectangle area = RADIUS[west ? 0 : 1][side];
//		if (side == 1) {
//			area = new Rectangle(getBase().transform(23, 38, 0), getBase().transform(9, 34, 0));
//		}
		if (area.getNumberOfPlayers() > count || (area.getNumberOfPlayers() >= count && side == 1)) {
			count = area.getNumberOfPlayers();
		}
		return count;
	}
	
	private void face(int face) {
		if (this.face == face) {
			setRitualising(false);
			return;
		}
		setRitualising(true);
		switch (GreatOlm.this.face) {
		case 0:
			render(face == 1 ? !west ? 7340 : 7342 : !west ? 7344 : 7343);
			break;
		case 1:
			render(face == 0 ? !west ? 7339 : 7341 : !west ? 7341 : 7339);
			break;
		case 2:
			render(face == 1 ? !west ? 7342 : 7340 : !west ? 7343 : 7344);
			break;
		}
		GreatOlm.this.face = face;
		render();
		CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {

			@Override
			public void execute(CycleEventContainer container) {
				container.stop();
				render(getRender());
				setRitualising(false);
				stop();
			}
		}, 3);
	}
	
	@Override
	public Script instance(Object... args) throws Throwable {
		return new GreatOlm();
	}

	@Override
	public void attack() {
		if (attackDelay > 0) {
			attackDelay--;
			return;
		}
		if (isRitualising()) {
			return;
		}
		for (Player player : getAllNonSafePlayers()) {
			hit(this, player);
		}
	}
	
	@Override
	public void hit(Entity attacker, Entity victim) {
		if (isRitualising()) {
			return;
		}
		int attackDelay = 10;
		setAttackDelay(attackDelay);
		switch (getCurrentPhase()) {
			case CRYSTAL:
				if (Misc.random(15) == 6) {
					if (Misc.random(10) > 6) {
						int targetCount = 0;
						
						while (targetCount < 2) {
							Location target = null;
							
							while (target == null) {
								target = getProbableTiles().get(Misc.random(getProbableTiles().size() - 1));
								if (target != null && getRadius()[isWest() ? 0 : 1][getFace()].inside(target)) {
									int delay = 60;
									targetCount++;
									playProjectile(Projectile.create(getCentreLocation().transform(!isWest() ? -2 : 3, getFace() == 1 ? 0 : getFace() == 0 ? 1 : -1, 0), target, CRYSTAL_BOMB, 45, 50, 100, 68, 0, 2, 0));
									if (getLocation().getDistance(target) == 1) {
										delay = 60;
							        } else if (getLocation().getDistance(target) <= 5) {
							        	delay =  80;
							        } else if (getLocation().getDistance(target) <= 8) {
							        	delay =  100;
							        } else {
							        	delay = 120;
							        }
									Location finalLocation = target;
									CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {

										@Override
										public void execute(CycleEventContainer container) {
											Region.addAbstractWorldObject(new CrystalBomb(GreatOlm.this, finalLocation));
											container.stop();
											this.stop();
										}
									}, (delay / 20) - 1);
									break;
								} else {
									target = null;
								}
							}
						}
						return;
					} else {
						Player target = (Player) getPossibleTargets().get(Misc.random(getPossibleTargets().size() - 1));
						for (Player player : getAllNonSafePlayers()	) {
							player.sendMessage("The Great Olm sounds a cry...");
						}
						render(getAttackAnimation(getFace()));
						target.sendMessage("<col=ff1a1a>The Great Olm has chosen you as its target - watch out!");
						CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
							int cycle = 0;
							@Override
							public void execute(CycleEventContainer container) {
								target.gfx(246, 0);
								playProjectile(Projectile.create(target.getLocation().transform(-1, 0, 0), target.getLocation().transform(-2, 0, 0), 1352, 45, 90, 80, 200, 0, 0, 0));
								Location hitLocation = target.getLocation();
								CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
									@Override
									public void execute(CycleEventContainer container) {
										container.stop();
										for (Player mob : getAllNonSafePlayers()) {
											if (mob.getLocation().equals(hitLocation)) {
												target.appendDamage(Misc.random(10, 25), Hitmark.HIT);
											}
										}
										container.stop();
										stop();
									}
								}, 2);
								CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
									@Override
									public void execute(CycleEventContainer container) {
										container.stop();
										for (Player mob : getAllNonSafePlayers()) {
											mob.getPA().stillGfx(1353, hitLocation.getX(), hitLocation.getY(), hitLocation.getZ(), 0);
										}
										container.stop();
										stop();
									}
								}, 3);
								if (cycle == 20) {
									container.stop();
									stop();
								}
								cycle++;
							}
						}, 2);
						return;
					}
				}
				break;
			case ACID:
				if (Misc.random(15) == 6) {
					if (Misc.random(10) > 3) {
						Player target = (Player) getPossibleTargets().get(Misc.random(getPossibleTargets().size() - 1));
						if (Boundary.isIn(target, new Boundary(3227, 5717, 3238, 5728, target.getHeight()))) {
							return;
						}
						int delay = 60;
						 render(getAttackAnimation(getFace()));
								playProjectile(Projectile.create(getCentreLocation().transform(!isWest() ? -2 : 3, getFace() == 1 ? 0 : getFace() == 0 ? 1 : -1, 0), target.getLocation(), ACID_SPRAY, 45, 50, 100, 68, 0, target.getProjectileLockonIndex(), 10, 48));
						if (getLocation().getDistance(target.getLocation()) == 1) {
							delay = 60;
				        } else if (getLocation().getDistance(target.getLocation()) <= 5) {
				        	delay =  80;
				        } else if (getLocation().getDistance(target.getLocation()) <= 8) {
				        	delay =  100;
				        } else {
				        	delay = 120;
				        }
						CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
							@Override
							public void execute(CycleEventContainer container) {
								target.sendMessage("<col=ff1a1a>The Great Olm has smothered you in acid. It starts to drip off slowly.");
								Region.addAbstractWorldObject(new AcidSpray(GreatOlm.this, target.getLocation()));
								target.getAttributes().put("acid_pool", true);
								CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
									@Override
									public void execute(CycleEventContainer container) {
										target.getAttributes().remove("acid_pool");
										container.stop();
										this.stop();
									}
								}, 15);
								container.stop();
								this.stop();
							}
						}, (delay / 20) - 1);
						return;
					} else {
						 render(getAttackAnimation(getFace()));
						int amount = Misc.random(8, 10);
						ArrayList<Location> tiles = new ArrayList<>();
						int count = 0;
						while (count < amount) {
							Location tile = getProbableTiles().get(Misc.random(getProbableTiles().size() - 1));
							if (!tiles.contains(tile)) {
								tiles.add(tile);
								count++;
							}
						}
						for (Location tile : tiles) {
							int delay = 60;
							playProjectile(Projectile.create(getCentreLocation().transform(!isWest() ? -2 : 3, getFace() == 1 ? 0 : getFace() == 0 ? 1 : -1, 0), tile, ACID_SPRAY, 45, 50, 100, 68, 0, 10, 48));
							if (getLocation().getDistance(tile) == 1) {
								delay = 60;
					        } else if (getLocation().getDistance(tile) <= 5) {
					        	delay =  80;
					        } else if (getLocation().getDistance(tile) <= 8) {
					        	delay =  100;
					        } else {
					        	delay = 120;
					        }
							CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
								@Override
								public void execute(CycleEventContainer container) {
									container.stop();
									Region.addAbstractWorldObject(new AcidSpray(GreatOlm.this, tile));
									this.stop();
								}
							}, (delay / 25));
						}
						CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
							@Override
							public void execute(CycleEventContainer container) {
								container.stop();
								render();
								render(getRender());
								this.stop();
							}
						}, 2);
						return;
					}
				}
				break;
			case FLAME:
				if (Misc.random(10) == 2) {
					if (Misc.random(10) > 6) {
						Player target = (Player) getPossibleTargets().get(Misc.random(getPossibleTargets().size() - 1));
						throttleFarcast(target, 5, BURN);
						CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
							@Override
							public void execute(CycleEventContainer container) {
								container.stop();
								burn(target, true);
								target.appendDamage(5, Hitmark.HIT);
								this.stop();
							}

							private void burn(Player target, boolean message) {
								if (Boundary.isIn(target, new Boundary(3227, 5717, 3238, 5728, target.getHeight()))) {
									return;
								}
								
								long lastAttack = target.hasAttribute("olm_burn") ? (long) target.getAttributes().get("olm_burn") : -1;
								if (lastAttack > System.currentTimeMillis()) {
									return;
								}
								if (message) {
									target.forcedChat("Burn with me!");
								}
								target.getAttributes().put("olm_burn", System.currentTimeMillis() + 60000);
								CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
									int count = 0;
									@Override
									public void execute(CycleEventContainer container) {

										target.appendDamage(5, Hitmark.HIT);
										target.forcedChat("Burn with me!");
										for (Player mob : getAllNonSafePlayers()) {
											long lastAttack = mob.hasAttribute("olm_burn") ? (long) mob.getAttributes().get("olm_burn") : -1;
											if (lastAttack > System.currentTimeMillis()) {
												continue;
											}
											if (target == mob) {
												continue;
											}
											if (mob.getLocation().isWithinDistance(target.getLocation(), 1)) {
												mob.forcedChat("I will burn with you");
												target.playProjectile(Projectile.create(target.getLocation(), mob.getLocation(), BURN + 1, 45, 50, 60, 35, 35, 0, 0));
												burn((Player) mob, false);
											}
										}
										if (count == 5) {
											this.stop();
											container.stop();
											target.sendMessage("You feel the deep burning inside dissipate.");
										}
										++count;
									}
								}, 10);
							}
						}, (getHitDelay(target) / 20) - 1);
					} else if (getAttributes().get("flame_wall") == null) {
						Player target = (Player) getPossibleTargets().get(Misc.random(getPossibleTargets().size() - 1));
						if (Boundary.isIn(target, new Boundary(3227, 5717, 3238, 5728, target.getHeight()))) {
							return;
						}
						if (target != null && target.getLocation().getY() != getBase().transform(0, 35, 0).getY() && target.getLocation().getY() != getBase().transform(0, 53, 0).getY()) {
							getAttributes().put("flame_wall", true);
							getAttributes().put("flame_wall_y", target.getLocation().getY());
							int rY = target.getLocation().getY() - ((target.getLocation().getRegionId() & 0xFF) << 6) - 8;
							playProjectile(Projectile.create(getCentreLocation().transform(!isWest() ? -2 : 3, getFace() == 1 ? 0 : getFace() == 0 ? 1 : -1, 0), getBase().transform(isWest() ? 22 : 12, rY - 1, 0), FIRE_SHOT, 45, 50, 90, 68, 0, 10, 48));
							playProjectile(Projectile.create(getCentreLocation().transform(!isWest() ? -2 : 3, getFace() == 1 ? 0 : getFace() == 0 ? 1 : -1, 0), getBase().transform(isWest() ? 22 : 12, rY + 1, 0), FIRE_SHOT, 45, 50, 90, 68, 0, 10, 48));
					        render(getAttackAnimation(getFace()));
					        CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
								@Override
								public void execute(CycleEventContainer container) {
									container.stop();
									this.stop();
									render(getRender());
									if (!isWest()) {
										int rYa = rY + 2;
										for (int i = 0; i < 10; i++) {
											playProjectile(Projectile.create(getBase().transform(12, rYa - 1, 0), getBase().transform(14 + i, rYa - 1, 0), FIRE_SHOT + 1, 45, 0, 80, 0, 0, 20, 0));
											playProjectile(Projectile.create(getBase().transform(12, rYa + 1, 0), getBase().transform(14 + i, rYa + 1, 0), FIRE_SHOT + 1, 45, 0, 80, 0, 0, 20, 0));
										}
									} else {
										int rYa = rY + 2;
										for (int i = 10; i > 0; i--) {
											playProjectile(Projectile.create(getBase().transform(25, rYa - 1, 0), getBase().transform(25 - i, rYa - 1, 0), FIRE_SHOT + 1, 45, 0, 80, 0, 0, 20, 0));
											playProjectile(Projectile.create(getBase().transform(25, rYa + 1, 0), getBase().transform(25 - i, rYa + 1, 0), FIRE_SHOT + 1, 45, 0, 80, 0, 0, 20, 0));
										}
									}
									CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
										@Override
										public void execute(CycleEventContainer container) {
											container.stop();
											this.stop();
											if (isWest()) {
												for (int i = 10; i > 0; i--) {
													Location l1 = getBase().transform(22 - i, rY + 1, 0), l2 = getBase().transform(22 - i, rY - 1, 0);
													for (Player mob : getAllNonSafePlayers()) {
														Player colliding = (Player) mob;
														if (colliding.getLocation().getY() != l1.getY() && colliding.getLocation().getY() != l2.getY())
															continue;
														long lastAttack = target.hasAttribute("wall_burn") ? (long) target.getAttributes().get("wall_burn") : -1;
														if (lastAttack > System.currentTimeMillis()) {
															continue;
														}
														target.getAttributes().put("wall_burn", System.currentTimeMillis() + 3000);
														Location newLocation = new Location(colliding.getLocation().getX(), colliding.getLocation().getY() == l1.getY() ? colliding.getLocation().getY() + 1 : colliding.getLocation().getY() - 1);
														target.sendMessage("You leap away from the flame, getting slightly scorched in the process.");
														target.teleportNoHeight(newLocation);
														colliding.appendDamage(5, Hitmark.HIT);
													}
													addWall(new FireWall(GreatOlm.this, l1), l1);
													addWall(new FireWall(GreatOlm.this, l2), l2);
												}
											} else {
												for (int i = 0; i < 10; i++) {
													Location l1 = getBase().transform(21 - i, rY + 1, 0), l2 = getBase().transform(21 - i, rY - 1, 0);
													for (Player mob : getAllNonSafePlayers()) {
														Player colliding = (Player) mob;
														if (colliding.getLocation().getY() != l1.getY() && colliding.getLocation().getY() != l2.getY())
															continue;
														long lastAttack = target.hasAttribute("wall_burn") ? (long) target.getAttributes().get("wall_burn") : -1;
														if (lastAttack > System.currentTimeMillis()) {
															continue;
														}
														target.getAttributes().put("wall_burn", System.currentTimeMillis() + 3000);
														Location newLocation = new Location(colliding.getLocation().getX(), colliding.getLocation().getY() == l1.getY() ? colliding.getLocation().getY() + 1 : colliding.getLocation().getY() - 1);
														target.sendMessage("You leap away from the flame, getting slightly scorched in the process.");
														target.teleportNoHeight(newLocation);
														colliding.appendDamage(5, Hitmark.HIT);
													}
													addWall(new FireWall(GreatOlm.this, l1), l1);
													addWall(new FireWall(GreatOlm.this, l2), l2);
												}
											}
											setFlameTicks(12);
										}
									}, 1);
								}
							}, 2);
						}
						
					}
					CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
						@Override
						public void execute(CycleEventContainer container) {
							container.stop();
							render();
							render(getRender());
							this.stop();
						}
					}, 3);
					setAttackDelay(attackDelay);
					return;
				}
				break;
			case OMEGA:
				break;
			default:
				break;
		}
		long lastSphere = hasAttribute("last_sphere") ? (long) getAttributes().get("last_sphere") : -1;
		if (Misc.random(12) == 3 && System.currentTimeMillis() > lastSphere) {
			 render(getAttackAnimation(getFace()));
			sphereAttack();
		} else {
			lastSphere = hasAttribute("last_sphere_delta") ? (long) getAttributes().get("last_sphere_delta") : -1;
			if (lastSphere + (600 * 3) > System.currentTimeMillis()) {
				return;
			}
			switch(getAttack()) {
				case DEFAULT_MAGIC:
					for (Player mob : getAllNonSafePlayers()) {
						Player target = (Player) mob;
						if (Boundary.isIn(target, new Boundary(3227, 5717, 3238, 5728, target.getHeight()))) {
							return;
						}
						throttleFarcast(target, 45, DEFAULT_MAGC_PROJECTILE);
					}
					break;
				case DEFAULT_RANGED:
					for (Player mob : getAllNonSafePlayers()) {
						Player target = (Player) mob;
						if (Boundary.isIn(target, new Boundary(3227, 5717, 3238, 5728, target.getHeight()))) {
							return;
						}
						throttleFarcast(target, 28, DEFAULT_RANGED_PROJECTILE);
					}
					break;
				default:
					break;
			
			}
		}
		CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				container.stop();
				render();
				render(getRender());
				this.stop();
			}
		}, 3);
	}
	
	private void throttleFarcast(Player target, int hit, int gfx) {
		long last = hitStamps.get(target) == null ? -1 : (long) hitStamps.get(target);
		if (last + 100 > System.currentTimeMillis()) {
			return;
		}
		hitStamps.put(target, System.currentTimeMillis());
		int gfxDelay = getHitDelay(target);
		int hitDelay = (gfxDelay / 20) - 1;
        render(getAttackAnimation(getFace()));
       	playProjectile(Projectile.create(getCentreLocation().transform(!isWest() ? -2 : isWest() ? 3 : 3, face == 1 ? isWest() ? -2 : -3 : face == 0 ? -3 : face == 2 ? -3 : -3, 2), target, gfx, 45, 50, gfxDelay, 68, 35, 10, 48));
        CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				container.stop();
				this.stop();
				int damage = !praying(target, gfx == DEFAULT_RANGED_PROJECTILE ? CombatPrayer.PROTECT_FROM_RANGED : CombatPrayer.PROTECT_FROM_MAGIC) ? Misc.random(1,hit) : 0;
				if (damage > health.getAmount()) {
					damage = health.getAmount();
				}
				target.appendDamage(damage, damage == 0 ? Hitmark.MISS : Hitmark.HIT);
			}
		}, hitDelay);
	}
	
	@Override
	public Location getCentreLocation() {
		return super.getCentreLocation().transform(isWest() ? 0 : 2, isWest() ? 3 : 2, 0);
	}

	@SuppressWarnings("serial")
	private void sphereAttack() {
		int playerCount = getAllNonSafePlayers().size();
		int count = 0;
		List<Player> marked = new ArrayList<>();
		List<Integer> types = new ArrayList<Integer>() { { add(1341); add(1343); add(1345); } };
		Collections.shuffle(types);
		attacks : for (;;) {
			for (Player player : getAllNonSafePlayers()) {
				if (!marked.contains(player) && Misc.random(3) == 2) {
					marked.add(player);
				}
				int type = types.get(Misc.random(types.size() - 1));
				int prayer = type == 1341 ?  CombatPrayer.PROTECT_FROM_MAGIC : type == 1343 ? CombatPrayer.PROTECT_FROM_RANGED : CombatPrayer.PROTECT_FROM_MELEE;
				sphere(player, 50, type, prayer);
				count++;
			}
			if (count == playerCount || count == 2) {
				break attacks;
			}
		}
		getAttributes().put("last_sphere", System.currentTimeMillis() + Misc.random(10000, 15000));
		getAttributes().put("last_sphere_delta", System.currentTimeMillis());
	}

	private void sphere(Player target, int maxHit, int graphic, int prayer) {
		int gfxDelay = (int) Math.ceil(getHitDelay(target) * 1.5);
		int hitDelay = (gfxDelay / 20) - 1;
		String message = "";
		switch(prayer) {
			case CombatPrayer.PROTECT_FROM_MAGIC:
				message = "<col=e600e6>The Olm fires a sphere of magical power your way.</col>";
				break;
			case CombatPrayer.PROTECT_FROM_MELEE:
				message = "<col=ff1a1a>The Olm fires a sphere of aggression your way.</col>";
				break;
			case CombatPrayer.PROTECT_FROM_RANGED:
				message = "<col=1aff1a>The Olm fires a sphere of accuracy and dexterity your way.</col>";
				break;
		}
		//target.getAttributes().put("protection_prayers_disabled", true);
		CombatPrayer.resetPrayers(target);
		target.sendMessage("Your prayers were disabled!");
		CycleEventHandler.getSingleton().addEvent(spawnedBy, new CycleEvent() {
			int count = 0;

			@Override
			public void execute(CycleEventContainer container) {
				if (count == 3) {
					//target.getAttributes().remove("protection_prayers_disabled");
					stop();
					container.stop();
				}
				count++;
			}
		}, 4);
		target.sendMessage(message);
		render(getAttackAnimation(face));
		target.prayerPoint /= 2;
		target.getPA().refreshSkill(5);
		CombatPrayer.resetPrayers(target);
		playProjectile(Projectile.create(getCentreLocation().transform(!isWest() ? -2 : isWest() ? 3 : 3, face == 1 ? isWest() ? -2 : -3 : face == 0 ? -3 : face == 2 ? -3 : -3, 2), target, graphic, 45, 50, gfxDelay, 68, 35, 10, 48));
		CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {

			@Override
			public void execute(CycleEventContainer container) {
				if (!praying(target, prayer)) {

				}
				int damage = !praying(target, prayer) ? Misc.random(10, 50) : 0;
				if (damage > target.getHealth().getAmount()) {
					damage = target.getHealth().getAmount();
				}
				target.appendDamage(damage, Hitmark.HIT);
				container.stop();
				this.stop();
			}

		}, hitDelay);
		checkSwitch();
	}
	
	private boolean praying(Player target, int prayer) {
		return target.prayerActive[prayer];
	}

	public void playProjectile(Projectile projectile) {
		int toX = projectile.getTarget() == null ? projectile.getFinish().getX() : projectile.getTarget().getX(), toY = projectile.getTarget() == null ? projectile.getFinish().getY() : projectile.getTarget().getY();
		for (Player player : getAllSafePlayers()) {
			if (player == null) {
				continue;
			}
			player.getPA().createProjectile2(projectile.getStart().getX(), projectile.getStart().getY(), (projectile.getStart().getY() - toY) * -1, (projectile.getStart().getX() - toX) * -1, projectile.getAngle(), projectile.getSpeed(), projectile.getId(), projectile.getStartHeight(), projectile.getEndHeight(), projectile.getLockon(), projectile.getDelay(), projectile.getSlope());

		}
	}

	public void render() {
		if (!west)  {
			this.render = face == 1 ? 7336 : face == 0 ? 7337 : 7338;
		} else {
			this.render = face == 1 ? 7336 : face == 0 ? 7338 : 7337;
		}
	}
	
	public void dropBomb() {
		if (isDead)
			return;
		Location target = getProbableTiles().get(Misc.random(getProbableTiles().size() - 1));;
		if (Misc.random(1, 9) == 3 && !getAllNonSafePlayers().isEmpty()) {
			target = getAllNonSafePlayers().get(Misc.random(getAllNonSafePlayers().size() - 1)).getLocation();
		}
		for (Player player : getAllNonSafePlayers()) {
			if (Boundary.isIn(player, new Boundary(3227, 5717, 3238, 5728, player.getHeight()))) {
				continue;
			}
			player.getPA().sendShake();
			playProjectile(Projectile.create(target.transform(-1, 0, 0), target.transform(-2, 0, 0), 1357, 45, 90, 160, 200, 0, 0, 0));
		}
		//System.out.println(target);
		Location hitLocation = target;
		CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {

			@Override
			public void execute(CycleEventContainer container) {
				for (Player mob : getAllNonSafePlayers()) {
					if (Boundary.isIn(mob, new Boundary(3227, 5717, 3238, 5728, mob.getHeight()))) {
						continue;
					}
					mob.getPA().sendStillGraphics(1358, hitLocation.getZ(), hitLocation.getY(), hitLocation.getX(), 0);
					if (mob.getLocation().equals(hitLocation)) {
						mob.appendDamage(Misc.random(10, 25), Hitmark.HIT);
					}
				}
				container.stop();
				stop();
			}
		}, 5);
	}
	
	private int getHitDelay(Player target) {
		if (olm.getLocation().isWithinDistance(target.getLocation(), 1)) {
            return 60;
        } else if (olm.getLocation().isWithinDistance(target.getLocation(), 5)) {
        	return  80;
        } else if (olm.getLocation().isWithinDistance(target.getLocation(), 8)) {
        	return  100;
        } else {
        	return 120;
        }
	}

	public void initialise() {
		for (int i = 5732; i < 5748; i++) {
			for (Player player : getAllSafePlayers()) {
				player.getPA().object(new WorldObject(-1, 3228, i, getPlane()));
				player.getPA().object(new WorldObject(-1, 3238, i, getPlane()));
			}
		}
		if (++stage == 3) {
			return;
		}
		getHealth().setMaximum(getHealthCapacity() + olmHealth);
		currentPhase = phases.get(stage);
		setRitualising(true);
		face = 1;
		attack = OlmAttack.DEFAULT_MAGIC;
		teleport(isWest() ? getBase().transform(6, 34, 0) : getBase().transform(22, 34, 0));
		{
			olm = new WorldObject(isWest() ? getBase().transform(4, 34, 0) : getBase().transform(22, 34, 0), GREAT_OLM, 10, isWest() ? 3 : 1, false);
			olm.setId(GREAT_OLM);
			olm.setLocation(isWest() ? getBase().transform(4, 34, 0) : getBase().transform(22, 34, 0));
			olm.setFace(isWest() ? 3 : 1);
		}
		{
			setRightClaw(new OlmClaw(this, 7553, isWest() ? getBase().transform(6, 29, 0) : getBase().transform(22, 39, 0), new WorldObject(isWest() ? getBase().transform(4, 29, 0) : getBase().transform(22, 39, 0), RIGHT_CLAW, 10,  isWest() ? 3 : 1, false)));
			Server.npcHandler.spawnNpc(PlayerHandler.players[spawnedBy], getRightClaw());
			getRightClaw().getHealth().reset();
			rightClaw.teleport(isWest() ? getBase().transform(6, 29, 0) : getBase().transform(22, 39, 0));
			rightClaw.getRender().setId(RIGHT_CLAW);
			rightClaw.getRender().setLocation(isWest() ? getBase().transform(4, 29, 0) : getBase().transform(22, 39, 0));
			rightClaw.getRender().setFace(isWest() ? 3 : 1);
			Region.addAbstractWorldObject(rightClaw.getRender());
			rightClaw.getHealth().reset();
			rightClaw.isDead = false;
		}
		{
			setLeftClaw(new OlmClaw(this, 7555, isWest() ? getBase().transform(6, 39, 0) : getBase().transform(22, 29, 0), new WorldObject(isWest() ? getBase().transform(4, 39, 0) : getBase().transform(22, 29, 0), LEFT_CLAW, 10,  isWest() ? 3 : 1, false)));
			Server.npcHandler.spawnNpc(PlayerHandler.players[spawnedBy], getLeftClaw());
			getLeftClaw().getHealth().reset();
			getLeftClaw().teleport(isWest() ? getBase().transform(6, 39, 0) : getBase().transform(22, 29, 0));
			getLeftClaw().getRender().setLocation(isWest() ? getBase().transform(4, 39, 0) : getBase().transform(22, 29, 0));
			getLeftClaw().getRender().setId(LEFT_CLAW);
			getLeftClaw().getRender().setFace(isWest() ? 3 : 1);
			Region.addAbstractWorldObject(getLeftClaw().getRender());
			getLeftClaw().getHealth().reset();
			getLeftClaw().isDead = false;
		}
		Region.addAbstractWorldObject(olm);
		CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {

			int cycle = 0;
			@Override
			public void execute(CycleEventContainer container) {
				if (cycle == 0) {
					updateAllObjects();
				} else if (cycle == 1) {
					render(AWAKEN);
					for (Player player : getAllNonSafePlayers()) {
						player.sendMessage(currentPhase.getMessage());
					}
					System.out.println("Awake");
					getRightClaw().render(GRAPPLE_RIGHT);
					getLeftClaw().render(GRAPPLE_LEFT);
				} else if (cycle == 6) {
					olm.setId(olm.getId() + 1);
					getRightClaw().update();
					getLeftClaw().update();
					Region.addAbstractWorldObject(olm);
					checkFace(false);
					render();
					render(getRender());	
				} else if (cycle == 8) {
					getLeftClaw().setNextAttackCycle(System.currentTimeMillis() + Misc.random(7000, 10000));
					setSwitching(false);
					container.stop();
					checkFace(true);
					stop();
				}
				cycle++;
			}
		}, 1);
	}
	
	public void updateAllObjects() {
		for (Player player : getAllNonSafePlayers()) {
			if (player == null) continue;
			updateAllObjects(player);
		}
	}
	
	public void updateAllObjects(Player player) {
		player.getPA().object(olm);
		player.getPA().object(rightClaw.getRender());
		player.getPA().object(leftClaw.getRender());
	}

	public int getAttackAnimation(int face) {
		if (!west) {
			return face == 0 ? 7346 : face == 1 ? 7345 : 7347;
		} else {
			return face == 0 ? 7347 : face == 1 ? 7345 : 7346;
		}
	}
	
	public void addWall(FireWall fireWall, Location at) {
		CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {

			@Override
			public void execute(CycleEventContainer container) {
				container.stop();
				fireWall.spawn(at);
				stop();
			}
		}, 2);
	}
	
	
	public ArrayList<Location> getProbableTiles() {
		ArrayList<Location> tiles = new ArrayList<>();
		for (int x = 12; x <= 21; x++) {
			for (int y = 27; y <= 43; y++) {
				tiles.add(getBase().transform(x, y, 0));
			}
		}
		return tiles;
	}
	
	public void killed(OlmClaw olmClaw) {
		if (getRightClaw().isDead && getLeftClaw().isDead && getStage() < 2) {
			if (isSwitching()) {
				return;
			}
			ritualising = true;
			setSwitching(true);
			CycleEventHandler.getSingleton().addEvent(PlayerHandler.players[spawnedBy], new CycleEvent() {
				int count = 0;
				@Override
				public void execute(CycleEventContainer container) {
					
					if (count == 2) {
						render(7348);
					} else if (count == 8) {
						bombs = true;
					} else if (count == 5) {
						olm.setId(olm.getId() - 1);
						updateAllObjects();
						Region.addAbstractWorldObject(olm);
						rightClaw.getRender().setId(rightClaw.getRender().getId() - 1);
						Region.addAbstractWorldObject(rightClaw.getRender());
						getLeftClaw().getRender().setId(getLeftClaw().getRender().getId() - 1);
						Region.addAbstractWorldObject(getLeftClaw().getRender());
					} else if (count == 36) {
						bombs = false;
						for (Player player : getAllNonSafePlayers()) {
							player.getPA().stopShake();
						}
					} else if (count == 41) {
						west = !west;
						initialise();
						container.stop();
						stop();
						return;
					}
					count++;
				}
				
			}, 1);
		} else if (olmClaw == getRightClaw()) {
			if (!getLeftClaw().isDead && getLeftClaw().isFlinching()) {
				getLeftClaw().unflinch();
			}
		} else {
			setSwitching(false);
		}
	}
	
	public void checkSwitch() {
		if (nextSwitch > 0) {
			nextSwitch--;
			return;
		}
		attack = attack == OlmAttack.DEFAULT_RANGED ? OlmAttack.DEFAULT_MAGIC : OlmAttack.DEFAULT_RANGED;
		nextSwitch = (byte) Misc.random(3, 10);
	}

	@Override
	public NPC getBoss() {
		return this;
	}

	/**
	 * @return the rADIUS
	 */
	public Rectangle[][] getRadius() {
		return RADIUS;
	}

	/**
	 * @return the face
	 */
	public int getFace() {
		return face;
	}

	/**
	 * @param face the face to set
	 */
	public void setFace(int face) {
		this.face = face;
	}

	/**
	 * @return the attack
	 */
	public OlmAttack getAttack() {
		checkSwitch();
		return attack;
	}
	
	/**
	 * @param attack the attack to set
	 */
	public void setAttack(OlmAttack attack) {
		this.attack = attack;
	}

	/**
	 * @return the west
	 */
	public boolean isWest() {
		return west;
	}

	/**
	 * @param west the west to set
	 */
	public void setWest(boolean west) {
		this.west = west;
	}

	/**
	 * @return the render
	 */
	public int getRender() {
		return render;
	}

	/**
	 * @param render the render to set
	 */
	public void setRender(int render) {
		this.render = render;
	}

	/**
	 * @return the ritualising
	 */
	public boolean isRitualising() {
		return ritualising;
	}

	/**
	 * @param ritualising the ritualising to set
	 */
	public void setRitualising(boolean ritualising) {
		this.ritualising = ritualising;
	}

	/**
	 * @return the ticks
	 */
	public int getTicks() {
		return ticks;
	}

	/**
	 * @param ticks the ticks to set
	 */
	public void setTicks(int ticks) {
		this.ticks = ticks;
	}

	/**
	 * @return the currentPhase
	 */
	public OlmPhase getCurrentPhase() {
		return currentPhase;
	}

	/**
	 * @return the nextSwitch
	 */
	public byte getNextSwitch() {
		return nextSwitch;
	}

	/**
	 * @return the flameTicks
	 */
	public int getFlameTicks() {
		return flameTicks;
	}

	/**
	 * @param flameTicks the flameTicks to set
	 */
	public void setFlameTicks(int flameTicks) {
		this.flameTicks = flameTicks;
	}

	public OlmClaw getRightClaw() {
		return rightClaw;
	}

	public void setRightClaw(OlmClaw rightClaw) {
		this.rightClaw = rightClaw;
	}

	public boolean isSwitching() {
		return switching;
	}

	public void setSwitching(boolean switching) {
		this.switching = switching;
	}

	public OlmClaw getLeftClaw() {
		return leftClaw;
	}

	public void setLeftClaw(OlmClaw leftClaw) {
		this.leftClaw = leftClaw;
	}

	public ArrayList<Player> getAllNonSafePlayers() {
		ArrayList<Player> list = new ArrayList<>();
		for (Player player : PlayerHandler.players) {
			if (player == null || player.getLocation().getZ() != plane || player.getLocation().getRegionId() != getLocation().getRegionId() || player.getLocation().getY() < 5730) {
				continue;
			}
			list.add(player);
		}
		return list;
	}
	
	public ArrayList<Player> getAllSafePlayers() {
		ArrayList<Player> list = new ArrayList<>();
		for (Player player : PlayerHandler.players) {
			if (player == null || player.getLocation().getZ() != plane || player.getLocation().getRegionId() != getLocation().getRegionId() || player.getLocation().getY() < 5718) {
				continue;
			}
			list.add(player);
		}
		return list;
	}

	/**
	 * @return the plane
	 */
	public int getPlane() {
		return plane;
	}

	/**
	 * @return the attackDelay
	 */
	public int getAttackDelay() {
		return attackDelay;
	}

	/**
	 * @param attackDelay the attackDelay to set
	 */
	public void setAttackDelay(int attackDelay) {
		this.attackDelay = attackDelay;
	}

	/**
	 * @return the stage
	 */
	public byte getStage() {
		return stage;
	}

	/**
	 * @param stage the stage to set
	 */
	public byte setStage(byte stage) {
		this.stage = stage;
		return stage;
	}

}
