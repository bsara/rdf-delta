/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.seaborne.patch.thrift.wire;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

@SuppressWarnings("all")
public enum Txn implements org.apache.thrift.TEnum {
  TX(0),
  TC(1),
  TA(2),
  Segment(3);

  private final int value;

  private Txn(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static Txn findByValue(int value) { 
    switch (value) {
      case 0:
        return TX;
      case 1:
        return TC;
      case 2:
        return TA;
      case 3:
        return Segment;
      default:
        return null;
    }
  }
}
