/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.jdk8;

import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.bind.DatatypeConverter;


/**
 * Place holder for some functionalities defined only in JDK8.
 * This file will be deleted on the SIS JDK8 branch.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
public final class JDK8 {
    /**
     * A shared Gregorian calendar to use for {@link #printDateTime(Date)}.
     * We share a single instance instead than using {@link ThreadLocal} instances
     * on the assumption that usages of this calendar will be relatively rare.
     */
    private static final AtomicReference<Calendar> CALENDAR = new AtomicReference<Calendar>();

    /**
     * Do not allow instantiation of this class.
     */
    private JDK8() {
    }

    /**
     * Returns the floating-point value adjacent to {@code value} in the direction of negative infinity.
     *
     * @param  value The value for which to get the adjacent value.
     * @return The adjacent value in the direction of negative infinity.
     *
     * @since 0.4
     */
    public static double nextDown(final double value) {
        return Math.nextAfter(value, Double.NEGATIVE_INFINITY);
    }

    /**
     * Returns the value of the given key, or the given default value if there is no value for that key.
     *
     * @param <V> The type of values.
     * @param map The map from which to get the value.
     * @param key The key for the value to fetch.
     * @param defaultValue The value to return if the map does not contain any value for the given key.
     * @return The value for the given key (which may be {@code null}), or {@code defaultValue}.
     */
    public static <V> V getOrDefault(final Map<?,V> map, final Object key, final V defaultValue) {
        V value = map.get(key);
        if (value == null && !map.containsKey(key)) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Stores the value in the given map, provided that no value were set.
     * This implementation presumes that the map can not contain null values.
     *
     * @param  <K>   The type of keys.
     * @param  <V>   The type of values.
     * @param  map   The map where to store the value.
     * @param  key   The key for the value to store.
     * @param  value The value to store.
     * @return The previous value, or {@code null} if none.
     */
    public static <K,V> V putIfAbsent(final Map<K,V> map, final K key, final V value) {
        final V previous = map.put(key, value);
        if (previous != null) {
            // Restore previous value.
            map.put(key, previous);
        }
        return previous;
    }

    /**
     * Atomically computes and stores the value for the given key. This is a substitute for
     * {@link ConcurrentMap#compute(java.lang.Object, java.util.function.BiFunction)}
     * on pre-JDK8 branches.
     *
     * @param  <K> The type of keys.
     * @param  <V> The type of values.
     * @param  map The map where to store the value.
     * @param  key The key for the value to compute and store.
     * @param  remappingFunction The function for computing the value.
     * @return The new value computed by the given function.
     *
     * @since 0.5
     */
    public static <K,V> V compute(final ConcurrentMap<K,V> map, final K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    {
        V newValue;
        boolean success;
        do {
            final V oldValue = map.get(key);
            newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                if (oldValue != null) {
                    success = map.replace(key, oldValue, newValue);
                } else {
                    success = (map.putIfAbsent(key, newValue) == null);
                }
            } else {
                if (oldValue != null) {
                    success = map.remove(key, oldValue);
                } else {
                    return null;
                }
            }
        } while (!success);
        return newValue;
    }

    /**
     * Parses a date from a string in ISO 8601 format. More specifically, this method expects the
     * format defined by <cite>XML Schema Part 2: Datatypes for {@code xsd:dateTime}</cite>.
     *
     * <p>This method will be replaced by {@link java.time.format.DateTimeFormatter} on the JDK8 branch.</p>
     *
     * @param  date The date to parse.
     * @return The parsed date.
     * @throws IllegalArgumentException if the given date can not be parsed.
     *
     * @see DatatypeConverter#parseDateTime(String)
     */
    public static Date parseDateTime(final String date) throws IllegalArgumentException {
        return DatatypeConverter.parseDateTime(date).getTime();
    }

    /**
     * Formats a date value in a string, assuming UTC timezone and US locale.
     * This method should be used only for occasional formatting.
     *
     * <p>This method will be replaced by {@link java.time.format.DateTimeFormatter} on the JDK8 branch.</p>
     *
     * @param  date The date to format.
     * @return The formatted date.
     *
     * @see DatatypeConverter#printDateTime(Calendar)
     */
    public static String printDateTime(final Date date) {
        Calendar calendar = CALENDAR.getAndSet(null);
        if (calendar == null) {
            calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        }
        calendar.setTime(date);
        final String text = DatatypeConverter.printDateTime(calendar);
        CALENDAR.set(calendar); // Recycle for future usage.
        return text;
    }
}