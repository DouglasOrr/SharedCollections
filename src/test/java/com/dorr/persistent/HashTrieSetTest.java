package com.dorr.persistent;

import com.google.common.collect.ImmutableSet;
import junit.framework.TestCase;
import org.hamcrest.Matchers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

// HashTrieSet is an adapter onto HashTrieMap, so there shouldn't be too
// much testing in it
public class HashTrieSetTest extends TestCase {
    public void testEmpty() {
        for (HashTrieSet<Object> empty : asList(
                HashTrieSet.EMPTY,
                HashTrieSet.empty(),
                HashTrieSet.of(),
                new HashTrieSet<Object>(),
                new HashTrieSet<Object>(Collections.emptySet()),
                new HashTrieSet<Object>(new HashSet<Object>())
        )) {
            assertThat(empty.size(), is(0));
            assertThat(empty.isEmpty(), is(true));
            assertThat(empty.contains("foo"), is(false));
            assertThat(empty.without("foo"), sameInstance(empty));
            assertThat(empty.toString(), is("[]"));
            assertThat(empty, equalTo(Collections.emptySet()));
            assertThat(empty.iterator().hasNext(), is(false));
        }
    }

    public void testConstruction() {
        HashTrieSet<String> justOne = HashTrieSet.singleton("one");
        assertThat(justOne.contains("one"), is(true));
        assertThat(justOne.contains("two"), is(false));
        assertThat(justOne, equalTo(Collections.singleton("one")));

        HashTrieSet<String> nums = HashTrieSet.of("one", "two", "three");
        assertThat(nums.contains("three"), is(true));
        assertThat(nums.contains("four"), is(false));
        assertThat(justOne, Matchers.<Set<String>> equalTo(ImmutableSet.of("one")));
    }

    public void testWithWithout() {
        HashTrieSet<String> s = HashTrieSet.empty();
        assertThat(s, Matchers.emptyCollectionOf(String.class));

        s = s.with("a").with("b");
        assertThat(s.containsAll(asList("a", "b")), is(true));
        assertThat(s.contains("A"), is(false));

        s = s.without("a");
        assertThat(s.contains("a"), is(false));
        assertThat(s.contains("b"), is(true));
    }
}