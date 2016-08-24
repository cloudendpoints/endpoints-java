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
package com.google.api.server.spi.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

/**
 * Utilities for handling jackson DOM nodes.
 *
 * @author sven@google.com (Sven Mawson)
 */
public class JacksonUtil {

  /**
   * Safely merge two object nodes. This will place any values from the second
   * node into the first if they do not already exist there. The first object
   * is returned as the merged result.
   *
   * @throws IllegalArgumentException if the merge cannot be completed.
   */
  public static ObjectNode mergeObject(ObjectNode object1, ObjectNode object2) {
    return mergeObject(object1, object2, true);
  }

  public static ObjectNode mergeObject(ObjectNode object1, ObjectNode object2,
      boolean throwOnConflict) {
    Iterator<String> fieldNames = object2.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode child2 = object2.get(fieldName);
      JsonNode child1 = object1.get(fieldName);
      JsonNode merged = (child1 == null) ? child2 : mergeNode(child1, child2, throwOnConflict);
      object1.put(fieldName, merged);
    }
    return object1;
  }

  /**
   * Safely merge two array nodes. This will append values from the second
   * array into the first array, and return the first array.
   */
  public static ArrayNode mergeArray(ArrayNode array1, ArrayNode array2) {
    array1.addAll(array2);
    return array1;
  }

  /**
   * Safely merge two nodes. This will appropriately merge objects or lists, as
   * well as verifying that values are compatible if both nodes are values.
   */
  public static JsonNode mergeNode(JsonNode node1, JsonNode node2) {
    return mergeNode(node1, node2, true);
  }

  /**
   * Safely merge two nodes. This will appropriately merge objects or lists, as
   * well as verifying that values are compatible if both nodes are values. If
   * {@code throwOnConflict} is set, an exception will be thrown if there is a merge conflict.
   * Otherwise, node1 will be returned as the conflict resolution.
   */
  public static JsonNode mergeNode(JsonNode node1, JsonNode node2, boolean throwOnConflict) {
    if (node1.isArray()) {
      if (!node2.isArray()) {
        if (throwOnConflict) {
          throw new IllegalArgumentException("Cannot merge array and non-array: "
              + node1 + ", " + node2);
        }
        return node1;
      }
      return mergeArray((ArrayNode) node1, (ArrayNode) node2);
    } else if (node1.isObject()) {
      if (!node2.isObject()) {
        if (throwOnConflict) {
          throw new IllegalArgumentException("Cannot merge object and non-object: "
              + node1 + ", " + node2);
        }
        return node1;
      }
      return mergeObject((ObjectNode) node1, (ObjectNode) node2, throwOnConflict);
    } else {
      // Value node, verify equivalence.
      if (throwOnConflict && !node1.equals(node2)) {
        throw new IllegalArgumentException("Cannot merge different values: "
            + node1 + ", " + node2);
      }
      return node1;
    }
  }
}
