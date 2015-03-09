package com.github.douglasorr.shared;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TrieArrayTest {
    // must be ordered
    public static final List<Integer> INTERESTING_SIZES = asList(
            0, 1, 2, 3, 4, 16,
            31, 32, 33,
            63, 64, 65,
            100, 200, 300, 400, 500, 600,
            32 * 32 - 1, 32 * 32, 32 * 32 + 1);

    private interface Op {
        void run(int n, List<String> reference, TrieArray<String> array);
    }

    private void foreachInterestingSize(Op op) {
        int n = 0;
        TrieArray<String> array = TrieArray.empty();
        List<String> reference = new ArrayList<String>();
        for (int targetSize : INTERESTING_SIZES) {
            while (n < targetSize) {
                array = array.append("foreachInterestingSize " + n);
                reference.add("foreachInterestingSize " + n);
                ++n;
            }
            op.run(targetSize, new ArrayList<String>(reference), array);
        }
    }

    private void assertGetOutOfBounds(SharedArray<?> array, int index) {
        try {
            array.get(index);
            Assert.fail("Expected an IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            assertThat(e.toString(), containsString(Integer.toString(index)));
        }
    }

    private void checkConsistency(SharedArray<String> array, List<String> reference) {
        assertThat(array, equalTo(reference));
        if (!reference.isEmpty()) {
            assertThat(array, contains(reference.toArray()));
        }
        assertThat(array.size(), equalTo(reference.size()));
        assertThat(array.isEmpty(), equalTo(reference.isEmpty()));
        for (int i = 0; i < reference.size(); ++i) {
            assertThat(array.get(i), equalTo(reference.get(i)));
        }
        assertGetOutOfBounds(array, array.size());
        assertGetOutOfBounds(array, -1);

        // forward iteration
        ListIterator<String> rIt = reference.listIterator();
        ListIterator<String> aIt = array.listIterator();
        while (rIt.hasNext()) {
            assertThat(aIt.nextIndex(), equalTo(rIt.nextIndex()));
            assertThat(aIt.previousIndex(), equalTo(rIt.previousIndex()));
            assertThat(aIt.hasNext(), equalTo(rIt.hasNext()));
            assertThat(aIt.hasPrevious(), equalTo(rIt.hasPrevious()));
            assertThat(aIt.next(), equalTo(rIt.next()));
        }

        // reverse iteration
        rIt = reference.listIterator(reference.size());
        aIt = array.listIterator(array.size());
        while (rIt.hasPrevious()) {
            assertThat(aIt.nextIndex(), equalTo(rIt.nextIndex()));
            assertThat(aIt.previousIndex(), equalTo(rIt.previousIndex()));
            assertThat(aIt.hasNext(), equalTo(rIt.hasNext()));
            assertThat(aIt.hasPrevious(), equalTo(rIt.hasPrevious()));
            assertThat(aIt.previous(), equalTo(rIt.previous()));
        }

        // round trip serialization
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bytes);
            out.writeObject(array);
            out.writeInt(0xdeadbeef); // make sure we pad correctly
            out.close();

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
            assertThat((SharedArray<String>) in.readObject(), equalTo(reference));
            assertThat(in.readInt(), equalTo(0xdeadbeef));
            in.close();

        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    // basic use

    private void checkAppend(TrieArray<String> array, List<String> reference, int nAppend) {
        // try appending a series of elements
        List<String> newReference = new ArrayList<String>(reference);
        TrieArray<String> newArray = array;
        for (int i = 0; i < nAppend; ++i) {
            newReference.add("checkAppend " + i);
            newArray = newArray.append("checkAppend " + i);
            checkConsistency(newArray, newReference);
        }
        // original unmodified
        checkConsistency(array, reference);
    }

    @Test
    public void testAppend() {
        foreachInterestingSize(new Op() {
            @Override
            public void run(int n, List<String> reference, TrieArray<String> array) {
                assertThat(array.size(), is(n));
                checkConsistency(array, reference);

                // just try adding a single element (more than this is tested by
                // the implementation of foreachInterestingSize() anyway)
                checkAppend(array, reference, 1);
            }
        });
    }

    private void checkRemend(TrieArray<String> array, List<String> reference) {
        // try repeatedly removing the last element
        List<String> newReference = new ArrayList<String>(reference);
        TrieArray<String> newArray = array;
        while (!newReference.isEmpty()) {
            newReference.remove(newReference.size() - 1);
            newArray = newArray.remend();
            checkConsistency(newArray, newReference);
        }
        // original unmodified
        checkConsistency(array, reference);
    }

    @Test
    public void testRemend() {
        foreachInterestingSize(new Op() {
            @Override
            public void run(int n, List<String> reference, TrieArray<String> array) {
                checkRemend(array, reference);
            }
        });
    }

    private void checkUpdate(TrieArray<String> array, List<String> reference) {
        // try updating each element in turn
        List<String> newReference = new ArrayList<String>(reference);
        TrieArray<String> newArray = array;
        for (int i = 0; i < reference.size(); ++i) {
            newReference.set(i, "checkUpdate " + i);
            newArray = newArray.update(i, "checkUpdate " + i);
            checkConsistency(newArray, newReference);
        }
        // original unmodified
        assertThat(array, equalTo(reference));
    }

    @Test
    public void testUpdate() {
        foreachInterestingSize(new Op() {
            @Override
            public void run(int n, List<String> reference, TrieArray<String> array) {
                checkUpdate(array, reference);
            }
        });
    }

    private void checkConsistencyWithModification(TrieArray<String> array, List<String> reference) {
        checkConsistency(array, reference);
        // check that all the basic collection ops still work on the result
        checkUpdate(array, reference);
        checkRemend(array, reference);
        checkAppend(array, reference, 100);
    }

    @Test
    public void testTake() {
        foreachInterestingSize(new Op() {
            @Override
            public void run(int n, List<String> reference, TrieArray<String> array) {
                // Build a set of counts to test
                Set<Integer> testTakes = new HashSet<Integer>();
                if (n < 100) {
                    // check all possible sublists
                    for (int i = 0; i <= n; ++i) {
                        testTakes.add(i);
                    }
                } else {
                    // just check a few interesting sizes (otherwise it's far too slow)
                    for (int i : INTERESTING_SIZES) {
                        if (i <= n) {
                            testTakes.add(i);
                        }
                    }
                    testTakes.add(n-1);
                    testTakes.add(n);
                }
                // run the test
                for (int i : testTakes) {
                    checkConsistencyWithModification(array.take(i), reference.subList(0, i));
                }
                // original unmodified
                checkConsistency(array, reference);
            }
        });
    }

    @Test
    public void testAppendAll() {
        // A very slow test - but good to be thorough!
        foreachInterestingSize(new Op() {
            @Override
            public void run(final int n0, final List<String> reference0, final TrieArray<String> array0) {
                foreachInterestingSize(new Op() {
                    @Override
                    public void run(int n1, List<String> reference1, TrieArray<String> array1) {
                        List<String> concatReference = new ArrayList<String>(reference0);
                        concatReference.addAll(reference1);
                        checkConsistencyWithModification(array0.appendAll(array1), concatReference);
                    }
                });
            }
        });
    }

    @Test
    public void testCopy() {
        foreachInterestingSize(new Op() {
            @Override
            public void run(int n, List<String> reference, TrieArray<String> array) {
                checkConsistency(new TrieArray<String>(reference), reference);
                // sharing (cheap) copy
                checkConsistency(new TrieArray<String>(array), reference);
            }
        });
    }

    // SharedArrays

    @Test
    public void testInsert() {
        foreachInterestingSize(new Op() {
            @Override
            public void run(int n, List<String> reference, TrieArray<String> array) {
                for (int i = 0; i <= n; ++i) {
                    List<String> newReference = new ArrayList<String>(reference);
                    newReference.add(i, "foobar");
                    checkConsistency(SharedArrays.insert(array, i, "foobar"), newReference);
                }
                checkConsistency(array, reference);
            }
        });
    }

    @Test
    public void testRemove() {
        foreachInterestingSize(new Op() {
            @Override
            public void run(int n, List<String> reference, TrieArray<String> array) {
                for (int i = 0; i < n; ++i) {
                    List<String> newReference = new ArrayList<String>(reference);
                    newReference.remove(i);
                    checkConsistency(SharedArrays.remove(array, i), newReference);
                }
                checkConsistency(array, reference);
            }
        });
    }

    // special cases

    @Test
    public void testEmpty() {
        for (TrieArray<Object> empty : Arrays.asList(
                TrieArray.EMPTY,
                TrieArray.empty(),
                TrieArray.of(),
                new TrieArray<Object>(),
                new TrieArray<Object>(Collections.emptyList()),
                new TrieArray<Object>(new ArrayList<Object>())
        )) {
            assertThat(empty.size(), is(0));
            assertThat(empty.isEmpty(), is(true));
            assertThat(empty.toString(), is("[]"));
            assertThat(empty, equalTo(Collections.emptyList()));
        }
    }

    @Test
    public void testSingleton() {
        TrieArray<String> singleton = TrieArray.singleton("one");
        assertThat(singleton.size(), is(1));
        assertThat(singleton.get(0), is("one"));
        assertThat(singleton, equalTo(Collections.singletonList("one")));
    }

    @Test
    public void testOf() {
        TrieArray<String> array = TrieArray.of("one", "two", "three");
        assertThat(array.size(), is(3));
        assertThat(array.get(0), is("one"));
        assertThat(array.get(2), is("three"));
        assertThat(array, equalTo(asList("one", "two", "three")));
    }

    @Test
    public void testAppendMassive() {
        List<String> reference = new ArrayList<String>();
        TrieArray<String> array = new TrieArray<String>();
        for (int i = 0; i < 32 * 32 * 32 + 1; ++i) {
            reference.add("item " + i);
            array = array.append("item " + i);
        }
        checkConsistency(array, reference);
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
