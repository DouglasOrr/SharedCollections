package com.dorr.persistent;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class LinkedListTest {
    @Test
    public void testEmpty() {
        for (LinkedList<? extends Object> empty : asList(LinkedList.EMPTY, LinkedList.empty())) {
            assertThat(empty, Matchers.empty());
            assertThat(empty, hasSize(0));
            assertThat(empty.iterator().hasNext(), is(false));
        }
    }
    @Test(expected = NoSuchElementException.class)
    public void testEmptyIteratorNext() {
        LinkedList.empty().iterator().next();
    }
    @Test(expected = NoSuchElementException.class)
    public void testEmptyHead() {
        LinkedList.empty().head();
    }
    @Test(expected = NoSuchElementException.class)
    public void testEmptyTail() {
        LinkedList.empty().tail();
    }

    @Test
    public void testPrepend() {
        LinkedList<Integer> empty = LinkedList.empty();
        LinkedList<Integer> singleton = empty.after(42);
        LinkedList<Integer> tri = singleton.after(20).after(10);

        assertThat(empty, hasSize(0));
        assertThat(singleton, hasSize(1));
        assertThat(tri, hasSize(3));

        // Check using Iterable's contract
        assertThat(singleton, contains(42));
        assertThat(tri,       contains(10, 20, 42));

        // Check using PersistentList's contract
        assertThat(singleton.head(), is(42));
        assertThat(singleton.tail(), sameInstance(LinkedList.<Integer>empty()));
        assertThat(tri.head(), is(10));
        assertThat(tri.tail().head(), is(20));
        assertThat("structural sharing", tri.tail().tail(), sameInstance(singleton));

        // Check using Collection's contract
        assertThat(empty, Matchers.<Collection<Integer>> equalTo(Collections.<Integer>emptyList()));
        assertThat(singleton, Matchers.<Collection<Integer>> equalTo(asList(42)));
        assertThat(tri, Matchers.<Collection<Integer>> equalTo(ImmutableList.of(10, 20, 42)));
    }

    @Test
    public void testListIterator() {
        ListIterator<Integer> it = LinkedList.of(10, 20, 30).listIterator();
        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(10));
        assertThat(it.next(), is(20));
        assertThat(it.next(), is(30));
        assertThat(it.hasNext(), is(false));

        it = LinkedList.of(10, 20, 30).listIterator(2);
        assertThat(it.next(), is(30));
        assertThat(it.hasNext(), is(false));

        it = LinkedList.of(10, 20, 30).listIterator(3);
        assertThat(it.hasNext(), is(false));
    }

    @Test(expected=NoSuchElementException.class)
    public void testListIteratorOutOfBounds() {
        ListIterator it = LinkedList.of(1).listIterator();
        it.next();
        it.next();
    }
    @Test(expected=IndexOutOfBoundsException.class)
    public void testListIteratorStartAfterBounds() {
        LinkedList.of(1,2,3).listIterator(4);
    }
    @Test(expected=IndexOutOfBoundsException.class)
    public void testListIteratorStartBeforeBounds() {
        LinkedList.of(1,2,3).listIterator(-1);
    }
}
