/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datastructure;

import java.util.*;

/**
 *
 * @author pieter
 * @param <T>
 */
public class TreeHeap<T> {
    private final HashMap<Long, TreeHeapElement<T>> idToEl = new HashMap<Long, TreeHeapElement<T>>();
    private final TreeMap<Double, ArrayList<TreeHeapElement<T>>> wToEl = new TreeMap<Double, ArrayList<TreeHeapElement<T>>>();

    public boolean isEmpty() {
        return idToEl.isEmpty();
    }

    public Set<T> getElements() {
        Set<T> out = new HashSet<T>();
        for (TreeHeapElement<T> el : idToEl.values()) {
            out.add(el.o);
        }
        return out;
    }
    
    public TreeHeap() {}
    public TreeHeapElement<T> add(Long id, Double w, T o) {
        TreeHeapElement<T> el = idToEl.get(id);
        TreeHeapElement<T> el_new = new TreeHeapElement<T>(id, w, o);
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
    
    public int size() {
        return idToEl.size();
    }
    
    public TreeHeapElement<T> extractMin() {
        Double minWeight = wToEl.firstKey();
        TreeHeapElement<T> out = wToEl.get(minWeight).get(0);
        removeFromTreeMap(out);
        idToEl.remove(out.id);
        return out;
    }

    public double minWeight() {
        return wToEl.firstKey();
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
