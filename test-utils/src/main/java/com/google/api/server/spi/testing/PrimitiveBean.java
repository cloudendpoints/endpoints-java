/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.google.api.server.spi.testing;

/**
 * A bean containing all of the primitive types.
 *
 * @author sven@google.com (Sven Mawson)
 */
public class PrimitiveBean {

  public boolean getBool() {
    return true;
  }

  public byte getByte() {
    return 4;
  }

  public char getChar() {
    return 'c';
  }

  public double getDouble() {
    return 45.5d;
  }

  public float getFloat() {
    return 22.2f;
  }

  public int getInt() {
    return 42;
  }

  public long getLong() {
    return 123456789012L;
  }

  public short getShort() {
    return 22;
  }

  public String getStr() {
    return "hello";
  }
}
