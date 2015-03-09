package com.github.douglasorr.shared;

import java.util.Set;

/**
 * A set that supports shared updates in place of mutable ones.
 * Apart from this, it behaves as a normal Java immutable Set.
 * <p>Instead of using <code>Set.add</code>, use {@link #with(Object)}, and
 * instead of using <code>Set.remove</code>, use {@link #without(Object)}.</p>
 */
public interface SharedSet<T> extends Set<T> {
    /**
     * Return a new set, with the given value present.
     * @param value the value to add
     * @return a new set with the value present (the original set is unchanged),
     * so <code>set.contains(key) == true</code>.
     */
    SharedSet<T> with(T value);

    /**
     * Return a new set, without the given value.
     * @param value the value to remove
     * @return a new set without the given value (the original set is unchanged),
     * so <code>set.contains(key) == false</code>.
     */
    SharedSet<T> without(T value);
}
