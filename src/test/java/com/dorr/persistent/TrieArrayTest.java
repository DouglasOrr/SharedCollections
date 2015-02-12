package com.dorr.persistent;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TrieArrayTest {
    public static final List<Integer> INTERESTING_SIZES = asList(
            0, 1, 2, 3, 4, 16,
            31, 32, 33,
            63, 64, 65,
            100, 200, 300, 400, 500, 600,
            32 * 32 - 1, 32 * 32, 32 * 32 + 1);

    @Test
    public void testEmpty() {
        TrieArray<String> empty = TrieArray.empty();
        assertThat(empty.size(), is(0));
    }

    @Test
    public void testSingleton() {
        TrieArray<String> singleton = TrieArray.singleton("one");
        assertThat(singleton.size(), is(1));
        assertThat(singleton.get(0), is("one"));
        assertThat(singleton, equalTo(Collections.singletonList("one")));
    }

    @Test
    public void testAppendUpdate() {
        TrieArray<String> trie = TrieArray.empty();
        ArrayList<String> reference = new ArrayList<String>();
        for (int i = 0; i < 32 * 32 + 1; ++i) {
            trie = trie.append("item " + i);
            reference.add("item " + i);
            for (int j = 0; j <= i; ++j) {
                assertThat(trie.get(j), equalTo("item " + j));
            }
        }
        assertThat(trie, Matchers.<List<String>> equalTo(reference));

        int idx = 3*32+5;
        trie = trie.update(idx, "monkey");
        assertThat(trie.get(idx), is("monkey"));
        assertThat(trie.get(idx-1), is("item " + (idx-1)));
        assertThat(trie.get(idx+1), is("item " + (idx + 1)));
    }

    @Test
    public void testRemend() {
        final int limit = 32 * 32 + 1;
        TrieArray<String> trie = TrieArray.empty();
        for (int i = 0; i < limit; ++i) {
            trie = trie.append("item " + i);
        }
        for (int i = limit; i != 0; --i) {
            trie = trie.remend();
            assertThat(trie.size(), is(i - 1));
            for (int j = 0; j < i - 1; ++j) {
                assertThat(trie.get(j), is("item " + j));
            }
        }
    }

    @Test
    public void testTake() {
        for (int limit : INTERESTING_SIZES) {
            TrieArray<String> trie = TrieArray.empty();
            for (int i = 0; i < limit; ++i) {
                trie = trie.append("item " + i);
            }
            for (int i = 0; i <= limit; ++i) {
                TrieArray<String> head = trie.take(i);
                assertThat(head.size(), is(i));
                for (int j = 0; j < i; ++j) {
                    assertThat(head.get(j), is("item " + j));
                }
            }
        }
    }

    @Test
    public void testIterateCopy() {
        for (int limit : asList(
                0, 1, 2, 3, 4, 16,
                31, 32, 33,
                63, 64, 65,
                100, 200, 300, 400, 500, 600,
                32*32 - 1, 32*32, 32*32 + 1)) {
            TrieArray<String> trie = TrieArray.empty();
            ArrayList<String> reference = new ArrayList<String>();
            for (int i = 0; i < limit; ++i) {
                trie = trie.append("item " + i);
                reference.add("item " + i);
            }
            assertThat(new ArrayList<String>(trie), equalTo(reference));
        }
    }

    @Test
    public void testIterate() {
        TrieArray<String> trie = TrieArray.of("one", "two", "three", "four");
        ListIterator<String> it = trie.listIterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.hasPrevious(), is(false));
        assertThat(it.nextIndex(), is(0));

        assertThat(it.next(), is("one"));
        assertThat(it.hasNext(), is(true));
        assertThat(it.hasPrevious(), is(true));
        assertThat(it.next(), is("two"));
        assertThat(it.previous(), is("two"));
        assertThat(it.nextIndex(), is(1));
        assertThat(it.previousIndex(), is(0));

        assertThat(it.next(), is("two"));
        assertThat(it.next(), is("three"));
        assertThat(it.next(), is("four"));
        assertThat(it.hasNext(), is(false));

        assertThat(trie.listIterator(3).next(), is("four"));
        assertThat(trie.listIterator(4).hasNext(), is(false));
    }

    @Test(expected=NoSuchElementException.class)
    public void testIterateNextNoSuchElement() {
        Iterator<String> it = TrieArray.of("one", "two").iterator();
        it.next();
        it.next();
        it.next();
    }
    @Test(expected=NoSuchElementException.class)
    public void testIteratePreviousNoSuchElement() {
        ListIterator<String> it = TrieArray.of("one", "two").listIterator(1);
        it.previous();
        it.previous();
    }
    @Test(expected=IndexOutOfBoundsException.class)
    public void testListIteratorAfterBounds() {
        TrieArray.of("one", "two").listIterator(3);
    }
    @Test(expected=IndexOutOfBoundsException.class)
    public void testListIteratorBeforeBounds() {
        TrieArray.of("one", "two").listIterator(-1);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(TrieArray.of(1, 100, 33));
        out.writeObject(TrieArray.empty());
        out.writeObject(TrieArray.singleton("foobar"));
        out.close();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        Assert.assertThat((TrieArray<Integer>) in.readObject(), equalTo(TrieArray.of(1, 100, 33)));
        Assert.assertThat((TrieArray<Object>) in.readObject(), emptyCollectionOf(Object.class));
        Assert.assertThat((TrieArray<String>) in.readObject(), equalTo(TrieArray.singleton("foobar")));
        in.close();
    }
}
