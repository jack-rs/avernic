package script.npc.olm;

import java.util.Iterator;
import java.util.Set;

import osv.model.players.xeric.Party;

/**
 * Iterator for collection.
 * 
 * @author Adil
 *
 * @param <T>
 * 			Olm param.
 */
public class InstanceIterator<T extends Party> implements Iterator<T> {
	
	/**
	 * The current instances to iterate through. 
	 */
	private Object[] instances;
	
	/**
	 * The container.
	 */
	private OlmInstanceManager<T> container;

	private Integer[] indicies;
	
	/**
	 * Element pointers.
	 */
	private int last;
	
	public InstanceIterator(Party[] instances, Set<Integer> indicies, OlmInstanceManager<T> container) {
		this.instances = instances;
		this.indicies = indicies.toArray(new Integer[indicies.size()]);
		this.container = container;
	}

	@Override
	public boolean hasNext() {
		return indicies.length != last;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T next() {
		Object temp = instances[indicies[last]];
		last++;
		return (T) temp;
	}

	@Override
	public void remove() {
		if (last >= 1) {
			container.remove(indicies[last - 1]);
		}
	}
	

}
