package script.npc.olm;

import java.util.AbstractCollection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import osv.model.players.Player;
import osv.model.players.xeric.Party;

/**
 * Manages olm instances.
 * 
 * @author Vip3r
 *
 * @param <T>
 * 		Olms.
 */
public class OlmInstanceManager<T extends Party> extends AbstractCollection<T> {
	
	private static final int MIN_VALUE = 1;
	public int curIndex = MIN_VALUE;
	public int capacity;
	
	/**
	 * Olm instances.
	 */
	private Party[] instances;
	
	public Set<Integer> indicies = new HashSet<>();
	
	private static final OlmInstanceManager<Party> INSTANCE = new OlmInstanceManager<>();
	
	public OlmInstanceManager() {
		instances = new Party[2048];
		capacity = 2048;
	}

	@Override
	public Iterator<T> iterator() {
		return new InstanceIterator<>(instances, indicies, this);
	}

	@Override
	public int size() {
		int count = 0;
		for (Party party : instances) {
			if (party != null) {
				count++;
			}
		}
		return count;
	}
	
	public int free() {
		for (int slot = MIN_VALUE; slot < capacity; slot++) {
			if (instances[slot] == null)
				return slot;
		}
		return -1;
	}
	
	public int addI(Party party) {
		int id = free();
		if (id == -1) {
			return -1;
		}
		instances[id] = party;
		party.setIndex(id);
		capacity++;
		return id;
	}
	
	public void freeUpI(Party party) {
		if (party == null)
			return;
		instances[party.getIndex()] = null;
		capacity--;
	}
	
	/**
	 * @return the instance
	 */
	public static OlmInstanceManager<Party> get() {
		return INSTANCE;
	}

	public Party createParty(Player leader, String name, int index) {
		Party party = new Party(leader, name, index);
		party.setPlane((addI(party) * 4) + (leader.getTutorial().isActive() ? 1000 : 0));
		return party;
	}

//	@SuppressWarnings("unchecked")
//	public T instance(Party party) {
//		GreatOlm olm = new GreatOlm(7554, new Position(3223, 5738, free()), free() * 4);
//		party.setPlane(addI((T) olm));
//		return (T) olm;
//	}

}
