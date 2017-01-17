package datastructure;

import java.util.*;

/**
 * Datastructure that allows to add, update and extract minimum elements in O(log(n))
 * @author Pieter
 * @param <T> The type of elements that is stored in the datastructure
 */
public class TreeHeap<T> {
    private final HashMap<Long, TreeHeapElement<T>> idToEl = new HashMap<>();
    private final TreeMap<Double, ArrayList<TreeHeapElement<T>>> wToEl = new TreeMap<>();
    
    public TreeHeap() {}

    // Add an element to the tree in O(log(n))
    // A weight is provided to order added elements
    // If an element with the provided id is present, the element will be stored with the minimum of the present and the new weight
    public TreeHeapElement<T> add(Long id, Double w, T o) {
        TreeHeapElement<T> el = idToEl.get(id);
        TreeHeapElement<T> el_new = new TreeHeapElement<>(id, w, o);
        if (el!=null) {
            if (w<el.weight) {
                removeFromTreeMap(el);
            } else {
                return el_new;
            }
        }
        idToEl.put(id, el_new);
        addToTreeMap(el_new);
        return el;
    }

    // Get the number of elements in the datastructure
    public int size() {
        return idToEl.size();
    }

    // Extract an element with minimum weight in O(log(n))
    public TreeHeapElement<T> extractMin() {
        Double minWeight = wToEl.firstKey();
        TreeHeapElement<T> out = wToEl.get(minWeight).get(0);
        removeFromTreeMap(out);
        idToEl.remove(out.id);
        return out;
    }
    
    private void addToTreeMap(TreeHeapElement<T> the) {
        if (!wToEl.containsKey(the.weight)) {
            wToEl.put(the.weight, new ArrayList<>());
        }
        wToEl.get(the.weight).add(the);
    }
    
    private void removeFromTreeMap(TreeHeapElement<T> the) {
        ArrayList<TreeHeapElement<T>> minList = wToEl.get(the.weight);
        minList.remove(the);
        if (minList.isEmpty()) {
            wToEl.remove(the.weight);
        }
    }

    // stuct that holds the stored element with it's weight and id
    public static class TreeHeapElement<T> {
        public Long id;
        public Double weight;
        public T o;
        TreeHeapElement(Long id, Double weight, T o) {
            this.id = id;
            this.weight = weight;
            this.o = o;
        }
    }
}
