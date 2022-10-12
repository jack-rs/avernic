package script.npc.olm;

import osv.model.items.GameItem;
import osv.model.players.Player;
import osv.util.Misc;

public class GreatOlmDropTable {
	
	private static final GameItem[] // 5 + Misc.random(2500)
			
			COMMON_DROPS = {
					new GameItem(562, 2500),//chaos rune
					new GameItem(560, 2500),//Death rune
					new GameItem(565, 2500),//blood rune
					new GameItem(1624, 50),//Uncut sapphire
					new GameItem(1622, 50),//emerald
					new GameItem(1620, 50),//ruby
					new GameItem(1618, 50),//diamond
					new GameItem(1632, 25),//dragonstone
					new GameItem(2364, 50),//rune bar
					new GameItem(200, 50),//guam leaf
					new GameItem(202, 50),//marrentill
					new GameItem(204, 50),//tarromin
					new GameItem(206, 50),//harralander
					new GameItem(208, 25),//ranarr	
					new GameItem(7937, 500),//pure ess
					new GameItem(454, 500),//coal
					new GameItem(452, 300),//rune ore
					new GameItem(13440, 250),//raw anglerfish
					new GameItem(1514, 200),//magic logs
					new GameItem(2362, 250),//adamant bar
					new GameItem(384, 500),//raw shark
					new GameItem(995, 500000),//coins
					new GameItem(995, 750000),//coins
					new GameItem(13307, 75),//bloodmoney
					new GameItem(13307, 50),//bloodmoney
					new GameItem(13307, 40),//bloodmoney
					new GameItem(20849, 25),//dragon thrownaxes
					new GameItem(11212, 500),//dragon arrows
					},
								


					UNCOMMON_DROPS = {
					new GameItem(995, 1000000),//coins
					new GameItem(11230, 750),//dragon darts
					new GameItem(12696, 25),//super combats
					new GameItem(12696, 25),//super combats
					new GameItem(11212, 1000),//dragon arrows
					new GameItem(995, 1000000),//coins
					new GameItem(11230, 750),//dragon darts
					new GameItem(12696, 25),//super combats
					new GameItem(12696, 25),//super combats
					new GameItem(11212, 1000),//dragon arrows
					new GameItem(13307, 100),//bloodmoney
					new GameItem(13307, 130),//bloodmoney
					new GameItem(13307, 175),//bloodmoney
					new GameItem(20849, 100),//dthrownaxes
					new GameItem(21034, 1),//
					new GameItem(21047, 1),//
					new GameItem(21079, 1),//
					new GameItem(216, 50),//cadantine
					new GameItem(208, 50),//ranarr weed
					new GameItem(218, 50),//dwarf weed
					new GameItem(220, 50),//torstol
					new GameItem(7937, 1000),//pure ess
					new GameItem(210, 50),//irit leaf
					new GameItem(212, 50),//avantoe
					new GameItem(214, 50),//kwarm
					new GameItem(216, 50),//cadantine
					new GameItem(208, 50),//ranarr weed
					new GameItem(218, 50),//dwarf weed
					new GameItem(220, 50),//torstol
					new GameItem(7937, 1000),//pure ess
					},

					RARE_DROPS = {
					new GameItem(21009, 1),//dsword
					new GameItem(21009, 1),//dsword
					
					new GameItem(21012, 1),//dh cbow
					new GameItem(21012, 1),//dh cbow

					new GameItem(21028, 1),//dragon harpoon
					new GameItem(21028, 1),//dragon harpoon

					new GameItem(21015, 1),//dinhs bulwark
					new GameItem(21015, 1),//dinhs bulwark

					new GameItem(21000, 1),//twisted buckler
					new GameItem(21000, 1),//twisted buckler

					new GameItem(21006, 1),//kodai wand
					new GameItem(21018, 1),//ancestrial hat
					new GameItem(21021, 1),//ancestrial top
					new GameItem(21024, 1),//ancestrial bottoms
					},
								
					VERY_RARE_DROPS = {
					new GameItem(21003, 1),//elder maul
					new GameItem(21003, 1),//elder maul
					new GameItem(13652, 1),//dclaws
					new GameItem(13652, 1),//dclaws
					new GameItem(20997, 1),//tbow
					};
	
	
	public static GameItem[] getLoot(Player player, GreatOlm olm) {
		return Misc.random(olm.killedBy == player.getIndex() ? 200 : 210) == 30 ? VERY_RARE_DROPS : Misc.random(olm.killedBy == player.getIndex() ? 135 : 150) == 40 ? RARE_DROPS : Misc.random(10) > 4 ? COMMON_DROPS : UNCOMMON_DROPS;
	}
	
	public static GameItem[] getTestLoot(Player player) {
		return Misc.random(135) == 40 ? VERY_RARE_DROPS : Misc.random(95) == 40 ? RARE_DROPS : Misc.random(10) > 6 ? COMMON_DROPS : UNCOMMON_DROPS;
	}

}
