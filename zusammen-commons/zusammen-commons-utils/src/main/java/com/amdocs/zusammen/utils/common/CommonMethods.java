/*
 * Copyright © 2016-2017 European Support Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.amdocs.zusammen.utils.common;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.*;


public class CommonMethods {
//	private static final Logger logger = LoggerFactory.getLogger(CommonMethods.class);

    private static final char[] CHARS = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /**
     * Private default constructor to prevent instantiation of the class objects.
     */
    private CommonMethods() {
    }


    /**
     * Serializes an object instance into byte array.
     *
     * @param object An instance to be serialized.
     * @return Java array of bytes.
     * @see #deserializeObject(byte[])
     */
    public static byte[] serializeObject(Serializable object) {
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream(2048);
        try (ObjectOutputStream ds = new ObjectOutputStream(byteArray)) {
            ds.writeObject(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteArray.toByteArray();
    } // serializeObject

    /**
     * Deserializes an object instance.
     *
     * @param bytes Java array of bytes.
     * @return Deserialized instance of an object.
     * @see #serializeObject(Serializable)
     */
    public static Serializable deserializeObject(byte[] bytes) {
        Serializable obj;
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes))){
            obj = (Serializable) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return obj;
    } // deserializeObject


    /**
     * Checks whether the given <tt>Object</tt> is empty.
     *
     * @param obj Object to be checked.
     * @return <tt>true</tt> - if the Object is null, <tt>false</tt> otherwise.
     */
    public static boolean isEmpty(Object obj) {
        return obj == null;
    }

    /**
     * Checks whether the given <tt>Object</tt> is empty.
     *
     * @param byteArray Object to be checked.
     * @return <tt>true</tt> - if the Object is null, <tt>false</tt> otherwise.
     */
    public static boolean isEmpty(byte[] byteArray) {
        return (byteArray == null || byteArray.length == 0);
    }


    /**
     * Checks whether the given <tt>String</tt> is empty.
     *
     * @param str String object to be checked.
     * @return <tt>true</tt> - if the String is null or empty, <tt>false</tt> - otherwise.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * Checks whether the given Java array is empty.
     *
     * @param array Java array to be checked.
     * @return <tt>true</tt> - if the array is null or empty, <tt>false</tt> - otherwise.
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Checks whether the given collection is empty.
     *
     * @param collection A collection to be checked.
     * @return <tt>true</tt> - if the collection is null or empty, <tt>false</tt> - otherwise.
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks whether the given map is empty.
     *
     * @param map A map to be checked.
     * @return <tt>true</tt> - if the map is null or empty, <tt>false</tt> - otherwise.
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Converts the array with Long elements to the array with long (primitive
     * type)
     *
     * @param array input array with Long elements
     * @return array with the same elements converted to the long type (primitive)
     */
    public static long[] toPrimitive(Long array[]) {
        if (array == null) {
            return null;
        }

        long result[] = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] != null ? array[i] : 0L;
        }
        return result;
    }

    /**
     * Converts a collection to Java array.
     *
     * @param <T>  Java type of the collection element.
     * @param col  Collection to be converted to array
     * @param type Java type of collection/array element
     * @return An Java array of collection elements, or empty array if collection is null or empty.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Collection<? extends T> col, Class<T> type) {
        int length = isEmpty(col) ? 0 : col.size();
        T[] array = (T[]) Array.newInstance(type, length);
        return col != null ? col.toArray(array) : array;
    }

    /**
     * Gets an universally unique identifier (UUID).
     *
     * @return String representation of generated UUID.
     */
    public static String nextUUID() {
        UUID uuid = UUID.randomUUID();

        StringBuilder buff = new StringBuilder(32);
        long2string(uuid.getMostSignificantBits(), buff);
        long2string(uuid.getLeastSignificantBits(), buff);

        return buff.toString();
    }

    private static void long2string(long l, StringBuilder buff) {
        for (int i = 0; i < 16; i++) {
            long nextByte = l & 0xF000000000000000L;
            l <<= 4;
            boolean isNegative = nextByte < 0;
            nextByte = rightShift(nextByte, 60);

            if (isNegative) {
                nextByte |= 0x08;
            }

            buff.append(CHARS[(int) nextByte]);
        }
    }

    private static long rightShift(long l, int n) {
        return l >>> n;
    }

    /**
     * Concatenates two Java arrays. The method allocates a new array and copies
     * all elements to it or returns one of input arrays if another one is
     * empty.
     *
     * @param <T> left+right
     * @param left  Elements of this array will be copied to positions from 0 to <tt>left.length -
     *              1</tt> in the target array.
     * @param right Elements of this array will be copied to positions from <tt>left.length</tt> to
     *              <tt>left.length + right.length</tt>
     * @return T[] newly allocate Java array that accommodates elements of source (left/right)
     * arrays or
     * one of source arrays if another is empty, <tt>null</tt> - otherwise.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concat(T[] left, T[] right) {
        T[] res;

        if (isEmpty(left)) {
            res = right;
        } else if (isEmpty(right)) {
            res = left;
        } else {
            res = (T[]) Array.newInstance(left[0].getClass(), left.length + right.length);
            System.arraycopy(left, 0, res, 0, left.length);
            System.arraycopy(right, 0, res, left.length, right.length);
        }

        return res;
    } // concat

    /**
     * Casts an object to the class or interface represented by the specified
     * <tt>Class</tt> object. The method logic is similar to Java method
     * <tt>Class.cast(Object)</tt> with the only difference that unlike Java's
     * version the type name of the current object instance is specified in the
     * error message if casting fails to simplify error tracking.
     *
     * @param <B> origin instance
     * @param <D> casted instance
     * @param b   An object instance to be casted to the specified Java type.
     * @param cls Target Java type.
     * @return Object instance safely casted to the requested Java type.
     * @throws ClassCastException In case which is the given object is not instance of the specified
     *                            Java type.
     */
    @SuppressWarnings("unchecked")
    public static <B, D> D cast(B b, Class<D> cls) {
        D d = null;
        if (b != null) {
            if (!cls.isInstance(b)) {
                throw new ClassCastException(String
                        .format("Failed to cast from '%s' to '%s'", b.getClass().getName(), cls.getName()));
            } else {
                d = (D) b;
            }
        }

        return d;
    } // cast

    public static Object newInstance(String classname) {
        return newInstance(classname, Object.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(String classname, Class<T> cls) {

        if (isEmpty(classname)) {
            throw new IllegalArgumentException();
        }

        if (cls == null) {
            throw new IllegalArgumentException();
        }

        try {
            Class<?> temp = Class.forName(classname);

            if (!cls.isAssignableFrom(temp)) {
                throw new ClassCastException(
                        String.format("Failed to cast from '%s' to '%s'", classname, cls.getName()));
            }

            Class<? extends T> impl = (Class<? extends T>) temp;

            return newInstance(impl);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T newInstance(Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public static String getResourcesPath(String resourceName) {
        URL resourceURL = CommonMethods.class.getClassLoader().getResource(resourceName);
        String resourcePath = resourceURL.getPath();

        return resourcePath.substring(0, resourcePath.lastIndexOf("/") + 1);
    }

    public static String getStackTrace(Throwable t) {
        if (null == t) {
            return "";
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String printStackTrace() {

        StringWriter sw = new StringWriter();
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (StackTraceElement traceElement : trace) {
            sw.write("\t  " + traceElement);
            sw.write(System.lineSeparator());
        }
        String s = sw.toString();
        try {
            sw.close();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;

    }

    public static boolean isEqualObject(Object obj1, Object obj2) {
        boolean isEqualValue = false;
        if (obj1 == null && obj2 == null) {
            isEqualValue = true;
        }

        if (!isEqualValue && obj1 != null && obj2 != null && obj1.equals(obj2)) {
            isEqualValue = true;
        }
        return isEqualValue;
    }

    /**
     * Converts array of strings to comma-separated string.
     *
     * @param arr array of strings
     * @return String comma Separated value
     */
    public static String arrayToCommaSeparatedString(String[] arr) {
        return arrayToSeparatedString(arr, ',');
    }

    public static String collectionToCommaSeparatedString(Collection<String> elementCollection) {
        List<String> list = new ArrayList<>();
        elementCollection.stream().forEach(list::add);
        return listToSeparatedString(list, ',');
    }


    /**
     * Converts array of strings to string separated with specified character.
     *
     * @param arr array of strings
     * @param separator separator
     * @return String comma Separated value
     */
    public static String arrayToSeparatedString(String[] arr, char separator) {
        return listToSeparatedString(Arrays.asList(arr), separator);
    }


    /**
     * Converts array of strings to string separated with specified character.
     *
     * @param list array of strings
     * @param separator separator
     * @return String comma Separated value
     */
    public static String listToSeparatedString(List<String> list, char separator) {
        String res = null;
        if (null != list) {
            StringBuilder sb = new StringBuilder();
            int sz = list.size();
            for (int i = 0; i < sz; i++) {
                if (i > 0) {
                    sb.append(separator);
                }
                sb.append(list.get(i));
            }
            res = sb.toString();
        }
        return res;
    }

    public static String duplicateStringWithDelimiter(String arg, char separator,
                                                      int numberOfDuplications) {
        String res;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < numberOfDuplications; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(arg);
        }
        res = sb.toString();
        return res;
    }


    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            int x = j << 1;
            hexChars[x] = hexArray[v >>> 4];
            hexChars[x + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * @param element the single element to be contained in the returned Set
     * @param <T>     the class of the objects in the set
     * @return an immutable set containing only the specified object. The returned set is
     * serializable.
     */

    public static <T> Set<T> toSingleElementSet(T element) {
        return Collections.singleton(element);
    }

    public static <T> List<T> iteratorToList(Iterable<T> iter) {
        List<T> list = new ArrayList<>();
        for (T item : iter) {
            list.add(item);
        }
        return list;
    }
}

