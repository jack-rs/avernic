package script.npc.olm;

import java.util.ArrayList;
import java.util.List;

import osv.clip.WorldObject;
import osv.clip.Region;
import osv.event.CycleEvent;
import osv.event.CycleEventContainer;
import osv.event.CycleEventHandler;
import osv.model.Location;
import osv.model.entity.Entity;
import osv.model.players.Boundary;
import osv.model.players.Player;
import osv.model.players.combat.Hitmark;
import osv.model.players.combat.melee.CombatPrayer;
import osv.util.Misc;

public enum ClawAttack {

	CRYSTAL_BURST(7356) {
		
		@Override
		public void attack(GreatOlm olm) {
			for (Player mob : olm.getAllNonSafePlayers()) {
				WorldObject crystal = new WorldObject(30033, mob.getLocation().getX(), mob.getLocation().getY(), mob.getLocation().getZ(),10, Misc.random(3));
				Region.addAbstractWorldObject(crystal);
				for (Player player : olm.getAllNonSafePlayers()) {
					player.getPA().object(crystal);
				}
				CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {

					@Override
					public void execute(CycleEventContainer container) {
						Region.removeWorldObject(crystal);
						for (Player player : olm.getAllNonSafePlayers()) {
							player.getPA().removeObject(crystal.getX(), crystal.getY());
						}
						crystal.setId(30034);
						Region.addAbstractWorldObject(crystal);
						for (Player mob : olm.getAllNonSafePlayers()) {
							if (mob.getLocation().equals(crystal.getLocation())) {
								Location newLocation = mob.getLocation().transform(Misc.random(-1, 1), Misc.random(-1, 1), 0);
								if (newLocation.getY() > 5730 && newLocation.getY() < 5748) {
									if (newLocation.getX() <= 3227) {
										newLocation = newLocation.transform(3228 - newLocation.getX(), 1, 0);
									}
									if (newLocation.getX() >= 3238) {
										newLocation = newLocation.transform(3237 - newLocation.getX(), 1, 0);
									}
								}
								mob.teleportNoHeight(newLocation);
								mob.appendDamage(Misc.random(25, 50), Hitmark.HIT);
								mob.sendMessage("The crystal beneath your feet grows rapidly and shunts you to the side.");
							}
						}
						for (Player player : olm.getAllNonSafePlayers()) {
							player.getPA().object(crystal);
						}
						for (Entity mob : olm.getPossibleTargets()) {
							Player player = (Player) mob;
							player.getPA().stillGfx(1337, crystal.getLocation().getX(), crystal.getLocation().getY(), crystal.getLocation().getZ(), 0);
						}
						CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {

							@Override
							public void execute(CycleEventContainer container) {
								Region.removeWorldObject(crystal);
								for (Player player : olm.getAllNonSafePlayers()) {
									player.getPA().removeObject(crystal.getX(), crystal.getY());
								}
								container.stop();
								stop();
							}
						}, 1);
						container.stop();
						stop();
					}
				}, 6);
			}
		}
		
	},
	
	LIGHTNING(7358) {
		
		@Override
		public void attack(GreatOlm olm) {
			// 12-21 27, 44
			List<Integer> flagged = new ArrayList<>();
			int initialCount = 0, otherCount = 0;
			for (int x1 = 13; x1 < 21; x1++) {
				if (Misc.random(5) == 3 || initialCount == 0 && x1 > 3) {
					int x = x1;
					flagged.add(x);
					initialCount++;
					throttleLightning(olm, 27, x);
				}
			}
			for (int x1 = 13; x1 < 21; x1++) {
				if (Misc.random(10) == 3) {
					if (flagged.contains(x1)) {
						x1 = flagged.contains(x1 + 1) ? x1 - 1 : x1 + 1;
					}
				} else {
					continue;
				}
				if (!flagged.contains(x1)) {
					int x = x1;
					flagged.add(x);
					otherCount++;
					throttleLightning(olm, 44, x);
				}
			}
			if (otherCount == 0) {
				for (int x1 = 13; x1 < 21; x1++) {
					if (!flagged.contains(x1)) {
						int x = x1;
						throttleLightning(olm, 44, x);
						break;
					}
				}
			}
		}
		
		private void throttleLightning(GreatOlm olm, int initial, int x) {
			
			CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {
				int y = initial;
				@Override
				public void execute(CycleEventContainer container) {
					
					
					for (Player mob : olm.getAllNonSafePlayers()) {
						Location l = olm.getBase().transform(x, y, 0);
						mob.getPA().stillGfx(1356, l.getX(), l.getY(), l.getZ(), 0);
						if (mob.getLocation().equals(l)) {
							mob.appendDamage(Misc.random(15, 30), Hitmark.HIT);
							mob.sendMessage("<col=ff1a1a>You've been electrocuted to the spot!");
							mob.getAttributes().put("stunned", true);
							mob.getAttributes().put("protection_prayers_disabled", true);
							CombatPrayer.resetPrayers(mob);
							mob.sendMessage("You've been injured and can't use protection prayers!");
							CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {
								int count = 0;
								@Override
								public void execute(CycleEventContainer container) {
									if (count == 0) {
										mob.getAttributes().remove("stunned");	
									} else if (count == 3) {
										mob.getAttributes().remove("protection_prayers_disabled");	
										stop();container.stop();
									}
									count++;
								}
							}, 4);
						}
					}
					if (y == (initial == 27 ? 44 : 27)) {
						stop();
						container.stop();
					}
					y+= initial == 27 ? 1 : -1;
				}
			}, 1);
		}
		
	},
	
	SWAP(7359) {
		
		@Override
		public void attack(GreatOlm olm) {
			int count = olm.getAllNonSafePlayers().size();
			int[] portals = { 1359, 1360, 1361, 1362 };
			if (count == 1) {
				pair(olm, olm.getAllNonSafePlayers().get(0), olm.getProbableTiles().get(Misc.random(olm.getProbableTiles().size() - 1)), portals[0]);
			} else {
				List<Player> pairs = new ArrayList<>();
				portal : for (int i = 0; i < portals.length; i++) {
					l1: for (Player player : olm.getAllNonSafePlayers()) {
						if (pairs.contains(player)) {
							continue l1;
						}
						l2: for (Player p2 : olm.getAllNonSafePlayers()) {
							if (player == p2 || pairs.contains(p2)) {
								continue l2;
							}
							pair(olm, player, p2, portals[i]);
							pair(olm, p2, player, portals[i]);
							pairs.add(player);
							pairs.add(p2);
							continue portal;
						}
					}
				}
			}
		}
		
		public void pair(GreatOlm olm, Player player, Object target, int gfx) {
			if (Boundary.isIn(player, new Boundary(3227, 5717, 3238, 5728, player.getHeight()))) {
				return;
			}
			if (target instanceof Location) {
				CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {
					int count = 0, hit = 0;
					@Override
					public void execute(CycleEventContainer container) {
						for (Player player : olm.getAllNonSafePlayers()) {
							player.getPA().stillGfx(gfx, ((Location) target).getX(), ((Location) target).getY(), ((Location) target).getZ(), 0);
						}
						if (count %2 == 0)
						player.sendGraphic(gfx, 0);
						if (count == 10) {
							int distance = (int) player.getLocation().getDistance((Location) target);
							hit = 5* distance;
							if (distance == 0) {
								player.sendMessage("The teleport attack has no effect!");
							} else {
								if (Boundary.isIn(player, Boundary.OLM) && !player.isDead()) {
									player.sendMessage("As you had no pairing... you are taken to a random spot!");
									player.appendDamage(hit, Hitmark.HIT);
									player.teleportNoHeight((Location) target);
									player.sendGraphic(1039, 0);
								}
							}
							stop();container.stop();
						} else {
							count++;
						}
					}
				}, 1);
			} else {
				Player other = (Player) target;
				player.sendMessage("You have been paired with <col=ff1a1a>" + other.getName() + "</col> The magical power will enact soon...");
				CycleEventHandler.getSingleton().addEvent(olm.spawnedBy, new CycleEvent() {
					int count = 0;
					@Override
					public void execute(CycleEventContainer container) {
						
						if (count %2 == 0)
							player.sendGraphic(gfx, 0);
						if (count == 10) {
							stop();container.stop();
							int distance = (int) player.getLocation().getDistance(((Player) target).getLocation());
							int hit = 5* distance;
							if (distance == 0) {
								player.sendMessage("The teleport attack has no effect!");
								return;
							} else {
								if (Boundary.isIn(other, Boundary.OLM) && Boundary.isIn(player, Boundary.OLM) && !player.isDead() && !other.isDead()) {
									player.sendMessage("Yourself and " + other.getName() + " have swapped places!");
									player.appendDamage(hit, Hitmark.HIT);
									player.teleportNoHeight(((Player) target).getLocation());
									player.sendGraphic(1039, 0);
								}
							}
						} else {
							count++;
						}
					}
				}, 1);
			}
		}
		
	};
	
	private int animation;
	
	private ClawAttack(int animation) {
		this.animation = animation;
	}

	public int getAnimation() {
		return animation;
	}
	
	public void attack(GreatOlm olm) {
		
	}

}
