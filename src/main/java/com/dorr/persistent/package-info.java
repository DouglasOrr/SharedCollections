/**
 * A small collection of persistent data structures for use in plain old Java code.
 *
 * <h2>Background</h2>
 * <p>An implementation of the same data structures that functional JVM languages such
 * as Clojure have, designed for use in plain Java code.
 * These data structures are designed to fit into the Java collections model as well
 * as possible, just adding the new persistent snapshot functionality.</p>
 *
 * <h2>Usage</h2>
 * <p>Each collection implements the relevant immutable Java collection, and adds just
 * a few methods needed to provide the new persistent update methods.
 * A persistent update method is just a method that returns a new collection with
 * some modification (don't worry, they do so without copying the whole collection.)
 * For example, with a Java mutable <code>Set</code>, you might write:</p>
 * <pre>{@code Set<String> s = new HashSet<>();
 * s.add("one");
 * s.add("two");}</pre>
 * With persistent collections, you could write:
 * <pre>{@code PersistentSet<String> s = HashTrieSet.empty();
 * s = s.with("one");
 * s = s.with("two");}</pre>
 * So if you have a reference to a <code>PersistentSet</code>, you can be sure that the
 * contents of that set will never change. But you can create a new set <em>based on</em>
 * the existing set efficiently, e.g. either with or without a given value.
 *
 * <h2>How do I start?</h2>
 * Here are some guides along the lines of "if I was using this mutable collection/update operation,
 * I can start using this persistent collection/update operation"...
 * <h3>Which collection</h3>
 * <table>
 *     <thead>
 *         <tr>
 *             <th>Mutable collection (<code>java.util.?</code>)</th>
 *             <th>Equivalent persistent collection (<code>com.dorr.persistent.?</code>)</th>
 *             <th>Fast operations:</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td><code>ArrayList</code></td>
 *             <td><code>TrieVector</code></td>
 *             <td>random access; random update; insertion at back</td>
 *         </tr>
 *         <tr>
 *             <td><code>LinkedList</code></td>
 *             <td><code>LinkedList</code></td>
 *             <td>query, insertion or removal at front</td>
 *         </tr>
 *         <tr>
 *             <td><code>HashSet, TreeSet</code></td>
 *             <td><code>HashTrieSet</code></td>
 *             <td>unique insert; contains</td>
 *         </tr>
 *         <tr>
 *             <td><code>HashMap, TreeMap</code></td>
 *             <td><code>HashTrieMap</code></td>
 *             <td>unique insert; lookup</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <h3>Equivalent mutation/update operations:</h3>
 * <table>
 *     <thead>
 *         <tr>
 *             <th>Mutable type/operation</th>
 *             <th>Persistent type/operation</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td><code>List.add</code></td>
 *             <td><code>PersistentArray.append</code></td>
 *         </tr>
 *         <tr>
 *             <td><code>List.set</code></td>
 *             <td><code>PersistentArray.update</code></td>
 *         </tr>
 *         <tr>
 *             <td><code>LinkedList.addFirst</code></td>
 *             <td><code>LinkedList.prepend</code></td>
 *         </tr>
 *         <tr>
 *             <td><code>Set.add</code></td>
 *             <td><code>PersistentSet.with</code></td>
 *         </tr>
 *         <tr>
 *             <td><code>Map.put</code></td>
 *             <td><code>PersistentMap.with</code></td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <h2>Collection extensions</h2>
 * <p>Each concrete collection type conforms to the Java collection model, namely extends from a <code>Collection</code>
 * interface (<code>List/Set/Map</code>), provides a zero-argument constructor, and a single-argument <code>Collection</code>
 * constructor. All destructive mutation operations throw <code>UnsupportedOperationException</code>. All collections support
 * Java's built-in serialization framework.</p>
 *
 * <p>In addition, we provide a few extensions to Java collections, to make creation easy, in particular a value <code>EMPTY</code>,
 * which is always the empty map/list/set, a convenience function <code>empty()</code> (which can be used to hide unchecked
 * conversion errors), and a factory method <code>of(...)</code> which creates a collection from a flat array.</p>
 */
package com.dorr.persistent;
