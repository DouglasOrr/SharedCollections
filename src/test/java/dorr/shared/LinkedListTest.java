package dorr.shared;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class LinkedListTest {
    @Test
    public void testEmpty() {
        for (LinkedList<?> empty : asList(
                LinkedList.EMPTY,
                LinkedList.empty(),
                LinkedList.of(),
                new LinkedList<Object>(),
                new LinkedList<Object>(Collections.emptySet()),
                new LinkedList<Object>(new ArrayList<Object>())
        )) {
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
        LinkedList<Integer> singleton = empty.prepend(42);
        LinkedList<Integer> tri = singleton.prepend(20).prepend(10);

        assertThat(empty, hasSize(0));
        assertThat(singleton, hasSize(1));
        assertThat(tri, hasSize(3));

        // Check using Iterable's contract
        assertThat(singleton, contains(42));
        assertThat(tri,       contains(10, 20, 42));

        // Check using SharedList's contract
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
    public void testConstruction() {
        LinkedList<Integer> oneTwoThree = LinkedList.of(1, 2, 3);
        assertThat(oneTwoThree, contains(1, 2, 3));
        assertThat("structural sharing", new LinkedList<Integer>(oneTwoThree).tail(), sameInstance(oneTwoThree.tail()));
        assertThat(new LinkedList<Integer>(asList(1, 2, 3)), contains(1, 2, 3));
        assertThat(new LinkedList<Integer>(ImmutableSet.of(1, 2, 3)), containsInAnyOrder(1, 2, 3));
        assertThat(LinkedList.singleton("element"), contains("element"));
    }

    @Test
    public void testListIterator() {
        ListIterator<Integer> it = LinkedList.of(10, 20, 30).listIterator();

        // initial state
        assertThat(it.hasNext(), is(true));
        assertThat(it.nextIndex(), is(0));
        assertThat(it.previousIndex(), is(-1));
        assertThat(it.hasPrevious(), is(false));

        // advancing
        assertThat(it.next(), is(10));
        assertThat(it.nextIndex(), is(1));
        assertThat(it.next(), is(20));
        assertThat(it.nextIndex(), is(2));
        assertThat(it.next(), is(30));
        assertThat(it.nextIndex(), is(3));
        assertThat(it.hasNext(), is(false));

        // start with offset
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

    @Test(expected=UnsupportedOperationException.class)
    public void testListIteratorBackwardsNotSupported() {
        ListIterator<Integer> it = LinkedList.of(10, 20, 30).listIterator(1);
        it.previous();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testListIteratorAddNotSupported() {
        LinkedList.of(10, 20, 30).listIterator().add(0);
    }
    @Test(expected=UnsupportedOperationException.class)
    public void testListIteratorRemoveNotSupported() {
        ListIterator<Integer> it = LinkedList.of(10, 20, 30).listIterator();
        it.next();
        it.remove();
    }
    @Test(expected=UnsupportedOperationException.class)
    public void testListIteratorSetNotSupported() {
        ListIterator<Integer> it = LinkedList.of(10, 20, 30).listIterator();
        it.next();
        it.set(100);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(LinkedList.of(1, 100, 33));
        out.writeObject(LinkedList.empty());
        out.writeObject(LinkedList.singleton("foobar"));
        out.close();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertThat((LinkedList<Integer>) in.readObject(), equalTo(LinkedList.of(1, 100, 33)));
        assertThat((LinkedList<Object>) in.readObject(), emptyCollectionOf(Object.class));
        assertThat((LinkedList<String>) in.readObject(), equalTo(LinkedList.singleton("foobar")));
        in.close();
    }
}
