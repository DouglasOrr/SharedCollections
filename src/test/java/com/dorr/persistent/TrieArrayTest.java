package com.dorr.persistent;

import junit.framework.TestCase;
import org.hamcrest.Matchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class TrieArrayTest extends TestCase {
    public void testEmpty() {
        TrieArray<String> empty = TrieArray.empty();
        assertThat(empty.size(), is(0));
    }

    public void testSingleton() {
        TrieArray<String> singleton = TrieArray.singleton("one");
        assertThat(singleton.size(), is(1));
        assertThat(singleton.get(0), is("one"));
        assertThat(singleton, equalTo(Collections.singletonList("one")));
    }

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
        assertThat(trie.get(idx+1), is("item " + (idx+1)));
    }

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

    public void testTake() {
        final int limit = 32 * 32 + 1;
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
