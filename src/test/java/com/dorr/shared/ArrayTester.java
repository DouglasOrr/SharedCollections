package com.dorr.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Abstracts away the details of array implementations, for fair-ish comparison. */
public abstract class ArrayTester<T> extends Tester {
    public abstract T get(int index);
    public abstract void add(T value);
    public abstract void add(int index, T value);
    public abstract void set(int index, T value);
    public abstract void remove();
    public abstract void remove(int index);
    public abstract Iterator<T> iterator();

    public static class SharedArrayTester<T> extends ArrayTester<T> {
        private final SharedArray<T> mEmpty;
        private SharedArray<T> mArray;

        public SharedArrayTester(SharedArray<T> empty) {
            mEmpty = empty;
            mArray = mEmpty;
        }

        @Override
        public T get(int index) {
            return mArray.get(index);
        }
        @Override
        public void add(T value) {
            mArray = mArray.append(value);
        }
        @Override
        public void add(int index, T value) {
            mArray = SharedArrays.insert(mArray, index, value);
        }
        @Override
        public void set(int index, T value) {
            mArray = mArray.update(index, value);
        }
        @Override
        public void remove() {
            mArray = mArray.remend();
        }
        @Override
        public void remove(int index) {
            mArray = SharedArrays.remove(mArray, index);
        }
        @Override
        public void reset() {
            mArray = mEmpty;
        }
        @Override
        public Iterator<T> iterator() {
            return mArray.iterator();
        }
        @Override
        public String toString() {
            return "Doug.TrieArray";
        }
    }

    public static class JavaArrayListTester<T> extends ArrayTester<T> {
        private List<T> mList;
        { reset(); }

        @Override
        public T get(int index) {
            return mList.get(index);
        }
        @Override
        public void add(T value) {
            mList.add(value);
        }
        @Override
        public void add(int index, T value) {
            mList.add(index, value);
        }
        @Override
        public void set(int index, T value) {
            mList.set(index, value);
        }
        @Override
        public void remove() {
            remove(mList.size() - 1);
        }
        @Override
        public void remove(int index) {
            mList.remove(index);
        }
        @Override
        public Iterator<T> iterator() {
            return mList.iterator();
        }
        @Override
        public void reset() {
            mList = new ArrayList<T>();
        }
        @Override
        public String toString() {
            return "Java.ArrayList";
        }
    }

    public static class ClojureVectorTester<T> extends ArrayTester<T> {
        private final clojure.lang.IPersistentVector mEmpty;
        private clojure.lang.IPersistentVector mVector;

        public ClojureVectorTester(clojure.lang.IPersistentVector empty) {
            mEmpty = empty;
            mVector = empty;
        }

        @Override
        public T get(int index) {
            return (T) mVector.nth(index);
        }
        @Override
        public void add(T value) {
            mVector = mVector.cons(value);
        }
        @Override
        public void add(int index, T value) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void set(int index, T value) {
            mVector = mVector.assocN(index, value);
        }
        @Override
        public void remove() {
            mVector = (clojure.lang.IPersistentVector) mVector.pop();
        }
        @Override
        public void remove(int index) {
            throw new UnsupportedOperationException();
        }
        @Override
        public Iterator<T> iterator() {
            throw new UnsupportedOperationException();
        }
        @Override
        public void reset() {
            mVector = mEmpty;
        }
        @Override
        public String toString() {
            return "Clojure.Vector";
        }
    }
}

