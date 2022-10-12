package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import osv.Server;
import osv.model.items.ItemList;

public class FindPoisons {

	private static String[] possible = { "dart", "javelin", "knife", "bolt", "arrow", "dagger", "spear", "hasta" };

	private static ArrayList<Item> results = new ArrayList<>();

	public static void main(String[] args) {
		Server.itemHandler.loadItemList("item.cfg");
		ItemList last = null;
		try {
			for (ItemList il : Server.itemHandler.ItemList) {
				last = il;
				if (il != null && il.itemName != null && !il.itemName.isEmpty()) {
					for (String subString : possible) {
						if (il.itemName.contains(subString) && !il.itemName.contains("tip")) {
							if (!osv.model.items.Item.itemIsNote[il.itemId])
								results.add(new Item(il.itemId, il.itemName));
						}
					}
				}
			}
			Collections.sort(results, new Comparator<Item>() {

				@Override
				public int compare(Item o1, Item o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			for (Item item : results) {
				System.out.println(item.getId() + " " + item.getName());
			}
		} catch (Exception e) {
			System.out.println(last.itemName);
			e.printStackTrace();
		}
	}
}
