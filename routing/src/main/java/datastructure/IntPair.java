package datastructure;

/**
 * Struct that holds an ordered pair of ints, two struct are equal if the ints are equal
 * Created by Pieter on 30/11/2016.
 */
public class IntPair {
    private int i1;
    private int i2;
    public IntPair(int i1, int i2) {
        this.i1 = i1;
        this.i2 = i2;
    }
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + i1;
        hash = hash * 31 + i2;
        return hash;
    }
    @Override
    public boolean equals(Object aThat) {
        if ( this == aThat ) return true;
        if ( !(aThat instanceof IntPair) ) return false;
        IntPair that = (IntPair)aThat;
        return i1 == that.i1 && i2 == that.i2;
    }
}
