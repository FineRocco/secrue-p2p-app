/**
 * The {@code SerializationService} class provides utility methods for serializing
 * and deserializing Java objects to and from byte arrays. This service allows for 
 * the conversion of objects into a byte array form suitable for storage or transmission 
 * over a network and then restores them to their original state.
 *
 * <p>This class includes:
 * <ul>
 *   <li>{@link #serialize(Object)}: Converts an object to a byte array</li>
 *   <li>{@link #deserialize(byte[])}: Restores an object from a byte array</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 *     byte[] data = SerializationService.serialize(myObject);
 *     MyClass restoredObject = (MyClass) SerializationService.deserialize(data);
 * </pre>
 * 
 * <p>Note: Classes to be serialized must implement {@link java.io.Serializable}.
 */
package com.psd.services;

import java.io.*;

public class SerializationService {

    /**
     * Serializes an object into a byte array.
     *
     * @param object The object to be serialized. Must implement {@link java.io.Serializable}.
     * @return A byte array representing the serialized object.
     * @throws RuntimeException if an {@link IOException} occurs during serialization.
     */
    public static byte[] serialize(Object object) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(object);
            return byteOut.toByteArray();
        } catch (IOException e) {
            System.out.println("Serialization error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes an object from a byte array.
     *
     * @param data The byte array containing the serialized object.
     * @return The deserialized object.
     * @throws RuntimeException if an {@link IOException} or {@link ClassNotFoundException} occurs.
     */
    public static Object deserialize(byte[] data) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Deserialization error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
