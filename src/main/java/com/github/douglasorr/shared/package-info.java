/**
 * A small collection of <em>immutable</em> data structures for use in plain old Java code
 * (an immutable array, hash map, hash set and linked list, providing structural sharing).
 *
 * <p>If you're familiar with functional JVM languages such as Clojure &amp; Scala, this is an
 * implementation of some of the standard collections in those languages, if not, don't worry,
 * these collections just provide a different sort of update operation - the <em>shared
 * update</em> (instead of the <em>mutable update</em>).</p>
 * <p>A <em>mutable update</em> is an operation that modifies the original collection (so if
 * you <code>put</code> into a <code>HashMap</code>, the original hash map will now contain the
 * new mapping). A <em>shared update</em>, however, does not modify the original, instead
 * it returns a new collection with the update applied (the original collection is never
 * modified).</p>
 * <p>The obvious question is 'why?', to which I shall mainly just say - try it out, and see for
 * yourself. But to motivate you to try this, I'll just observe that it provides an additional
 * guarantee: <em>any reference to a shared collection I have will never be modified, never have to
 * acquire a lock, and never throw a <code>ConcurrentModificationException</code></em>. I consider
 * this guarantee to be a very helpful tool to write programs that are simpler, more likely to
 * be correct and more easily adapted to support concurrency.</p>
 *
 * <h2>Usage</h2>
 * <p>Each collection implements the relevant immutable Java collection, and adds just
 * a few methods needed to provide the new shared update methods.
 * A shared update method is just a method that returns a new collection with
 * some modification (importantly, they do so without copying the whole collection.)
 * For example, with a Java mutable <code>Set</code>, you might write:</p>
 * <pre>{@code Set<String> s = new HashSet<>();
 * s.add("one");
 * s.add("two");}</pre>
 * With shared collections, you could write:
 * <pre>{@code SharedSet<String> s = HashTrieSet.empty();
 * s = s.with("one");
 * s = s.with("two");}</pre>
 *
 * <p>So if you have a reference to a <code>SharedSet</code>, you can be sure that the
 * contents of that set will never change. But you can create a new set <em>based on</em>
 * the existing set efficiently, e.g. either with or without a given value.
 * So it is very easy to get started with shared collections - you just need to get used to the
 * shared update operation, and replace your mutable collections with the equivalent shared
 * ones (we provide a table below to help you get started). The way to read this is "if I would have
 * used <code>java.util.X</code>, I'll start using <code>com.github.douglasorr.shared.Y</code>" (these are only
 * rough guidelines).</p>
 *
 * <table summary="Equivalent mutable and shared collections">
 *     <thead>
 *         <tr>
 *             <th>Mutable collection (<code>java.util.?</code>)</th>
 *             <th>Shared collection (<code>com.github.douglasorr.shared.?</code>)</th>
 *             <th>Fast operations</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>{@link java.util.ArrayList}</td>
 *             <td>{@link com.github.douglasorr.shared.TrieArray}</td>
 *             <td>random access; random update; insertion at back</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.LinkedList}</td>
 *             <td>{@link com.github.douglasorr.shared.LinkedList}</td>
 *             <td>query, insertion or removal at front</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.HashSet}, {@link java.util.TreeSet}</td>
 *             <td>{@link com.github.douglasorr.shared.HashTrieSet}</td>
 *             <td>unique insert; contains</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.HashMap}, {@link java.util.TreeMap}</td>
 *             <td>{@link com.github.douglasorr.shared.HashTrieMap}</td>
 *             <td>unique insert; lookup</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <p>Once you've settled on the type of your collection, you'll need to know how to use it. Each collection
 * implements the appropriate Java interface for a read-only collection (all the mutating operations throw
 * a <code>UnsupportedOperationException</code>), and are replaced by equivalent shared update operations
 * as shown below.</p>
 * <table summary="Equivalent mutable and shared operations">
 *     <thead>
 *         <tr>
 *             <th>Mutable method</th>
 *             <th>Shared method</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>{@link java.util.List#add(java.lang.Object)}</td>
 *             <td>{@link com.github.douglasorr.shared.SharedArray#append(java.lang.Object)}</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.List#set(int, java.lang.Object)}</td>
 *             <td>{@link com.github.douglasorr.shared.SharedArray#update(int, java.lang.Object)}</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.List#remove(int)}</td>
 *             <td>{@link com.github.douglasorr.shared.SharedArray#remend()},
 *             {@link com.github.douglasorr.shared.SharedArrays#remove(com.github.douglasorr.shared.SharedArray, int)}</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.LinkedList#addFirst(java.lang.Object)}</td>
 *             <td>{@link com.github.douglasorr.shared.SharedList#prepend(java.lang.Object)}</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.LinkedList#getFirst()}</td>
 *             <td>{@link com.github.douglasorr.shared.SharedList#head()}</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.Set#add(java.lang.Object)}</td>
 *             <td>{@link com.github.douglasorr.shared.SharedSet#with(java.lang.Object)}</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.Set#remove(java.lang.Object)}</td>
 *             <td>{@link com.github.douglasorr.shared.SharedSet#without(java.lang.Object)}</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.Map#put(java.lang.Object, java.lang.Object)}</td>
 *             <td>{@link com.github.douglasorr.shared.SharedMap#with(java.lang.Object, java.lang.Object)}</td>
 *         </tr>
 *         <tr>
 *             <td>{@link java.util.Map#remove(java.lang.Object)}</td>
 *             <td>{@link com.github.douglasorr.shared.SharedMap#without(java.lang.Object)}</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <h2>When should I use them?</h2>
 * <p>I think these collections are great - but I don't recommend using them all the time. The main benefit is that they
 * provide a strong guarantee - if I have a reference to a shared collection, it will never be modified, and it will
 * never have to acquire a lock when I use it. This guarantee comes at the cost of slightly more work when using
 * and updating the collection, and more small memory allocations. While JVM implementations are typically very good
 * at handling small object allocations, this does result in a performance penalty for using shared collections.
 * For example, in one of my tests, the shared hash trie map with 10000 elements was just 10% slower for access,
 * and 2.5-3x slower for insertions. Please don't let this penalty put you off using them altogether - it sounds a lot,
 * but if the bulk of your runtime isn't spent updating collections, using shared collections probably won't make
 * your program slower. I would suggest that if you can make a concurrent Android app using shared collections, your
 * users will often feel better performance than they would with a 'faster' Android app using mutable collections (where
 * the concurrency is limited by the difficulties of correctly synchronizing data access).</p>
 *
 * <p>Because of this tradeoff of safety against performance, I would generally use a mutable collection where the
 * scope of the collection is very limited &amp; simple (e.g. within a single method). When the scope of the collection is
 * large or complex (e.g. returned by an interface method), I would prefer the no-modification guarantee of a shared
 * collection.</p>
 * <ul>
 * <li>For example, I am writing an algorithm that requires a stack to run, but once finished the stack is garbage. I have
 * the choice between the mutable {@link java.util.ArrayDeque} and the shared {@link com.github.douglasorr.shared.TrieArray}.
 * In this case, I would choose the mutable {@link java.util.ArrayDeque}, as it has higher performance, and the
 * no-modification guarantee doesn't really make my program any simpler or safer.</li>
 *
 * <li>Now I am writing an <code>Event</code> type for a UI framework and must provide within the payload a set of objects.
 * Again, I can choose between a mutable {@link java.util.HashSet} and a shared {@link com.github.douglasorr.shared.HashTrieSet}.
 * This time, the scope of the collection is potentially large, and certainly complex (it can change at runtime as event
 * handlers are added), so I would prefer the shared {@link com.github.douglasorr.shared.HashTrieSet}.</li>
 * </ul>
 * <p>Often your decisions won't be as obvious as this - but as you gain confidence in using shared collections, I
 * suggest that most collections that are exposed by a method or field on a class or interface should provide the
 * no-modification guarantee.</p>
 *
 * <h2>Collection extensions</h2>
 * <p>Each concrete collection type conforms to the Java collection model, namely extends from a <code>Collection</code>
 * interface (<code>List/Set/Map</code>), provides a zero-argument constructor, and a single-argument <code>Collection</code>
 * constructor. All destructive mutation operations throw <code>UnsupportedOperationException</code>. All collections support
 * Java's built-in serialization framework.</p>
 *
 * <p>In addition, we provide a few extensions to Java collections, to make creation easy, in particular a value <code>EMPTY</code>,
 * which is always the empty map/list/set, a convenience function <code>empty()</code> (which can be used to hide unchecked
 * conversion errors), and factory methods <code>singleton()</code> to create a single-element collection and <code>of(...)</code>
 * which creates a collection from a flat array.</p>
 */
package com.github.douglasorr.shared;
