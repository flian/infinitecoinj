/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.infinitecoinj.core;

public class InventoryItem {
    
    /**
     * 4 byte uint32 type field + 32 byte hash
     */
    static final int MESSAGE_LENGTH = 36;
    
    public enum Type {
        Error,
        Transaction,
        Block,
        FilteredBlock
    }

    public final Type type;
    public final Sha256Hash hash;

    public InventoryItem(Type type, Sha256Hash hash) {
        this.type = type;
        this.hash = hash;
    }


    public String toString() {
        return type.toString() + ": " + hash;
    }

    public int hashCode() {
        return hash.hashCode() + type.ordinal();
    }

    public boolean equals(Object o) {
        return o instanceof InventoryItem &&
                ((InventoryItem)o).type == this.type &&
                ((InventoryItem)o).hash.equals(this.hash);
    }
}
