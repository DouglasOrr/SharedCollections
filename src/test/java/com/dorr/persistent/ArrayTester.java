package com.dorr.persistent;

import clojure.lang.IPersistentVector;
import scala.collection.Seq;
import scala.collection.Seq$;
import scala.collection.generic.CanBuildFrom;
import scala.collection.mutable.Builder;

import java.util.ArrayList;
import java.util.List;

/** Abstracts away the details of array implementations, for fair-ish comparison. */
public abstract class ArrayTester<T> extends Tester {
    public abstract T get(int index);
    public abstract void add(T value);

    public static class PersistentArrayTester<T> extends ArrayTester<T> {
        private final PersistentArray<T> mEmpty;
        private PersistentArray<T> mArray;

        public PersistentArrayTester(PersistentArray<T> empty) {
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
        public void reset() {
            mArray = mEmpty;
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
        public void reset() {
            mList = new ArrayList<T>();
        }
    }

    public static class ClojureVectorTester<T> extends ArrayTester<T> {
        private final clojure.lang.IPersistentVector mEmpty;
        private clojure.lang.IPersistentVector mVector;

        public ClojureVectorTester(IPersistentVector empty) {
            mEmpty = empty;
            mVector = empty;
        }

        @Override
        public T get(int index) {
            return (T) mVector.nth(index);
        }
        @Override
        public void add(T value) {
            mVector.assocN(mVector.length(), value);
        }
        @Override
        public void reset() {
            mVector = mEmpty;
        }
    }

    public static class ScalaIndexedSeqTester<T> extends ArrayTester<T> {
        private final CanBuildFrom<Seq<T>, T, Seq<T>> mBuildFrom = new CanBuildFrom<Seq<T>, T, Seq<T>>() {
            final CanBuildFrom<Seq<?>, T, Seq<T>> mBuild = Seq$.MODULE$.canBuildFrom();
            @Override
            public Builder<T, Seq<T>> apply(Seq<T> tSeq) {
                return mBuild.apply();
            }
            @Override
            public Builder<T, Seq<T>> apply() {
                return mBuild.apply();
            }
        };
        private final scala.collection.Seq<T> mEmpty;
        private scala.collection.Seq<T> mSeq;

        public ScalaIndexedSeqTester(scala.collection.Seq<T> empty) {
            mEmpty = empty;
            mSeq = mEmpty;
        }

        @Override
        public T get(int index) {
            return mSeq.apply(index);
        }
        @Override
        public void add(T value) {
            mSeq = mSeq.$colon$plus(value, mBuildFrom);
        }
        @Override
        public void reset() {
            mSeq = mEmpty;
        }
    }
}
