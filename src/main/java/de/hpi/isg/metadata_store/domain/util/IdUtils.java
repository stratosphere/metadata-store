/***********************************************************************************************************************
 * Copyright (C) 2014 by Sebastian Kruse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 **********************************************************************************************************************/
package de.hpi.isg.metadata_store.domain.util;

import de.hpi.isg.metadata_store.domain.MetadataStore;

/**
 * This class offers some utility functionality to deal with IDs in a {@link MetadataStore}. Thereby, it obeys the
 * following conventions:
 * <ol>
 * <li>The 32 bit of each ID are assigned to different elements: <tt>xxx... yyy... zzz....</tt></li>
 * <li>the high {@value #NUM_SCHEMA_BITS} bits are used to encode the schema (<tt>x</tt>)</li>
 * <li>the mid {@value #NUM_TABLE_BITS} bits are used to encode the table (<tt>y</tt>)</li>
 * <li>the low {@value #NUM_COLUMN_BITS} bits are used to encode the column (<tt>y</tt>)</li>
 * <li>for schema IDs and table IDs the column (and the table bits) are all set to 1</li>
 * </ol>
 */
public class IdUtils {

    private static final int NUM_TABLE_BITS = 12;
    private static final int NUM_COLUMN_BITS = 12;
    private static final int NUM_SCHEMA_BITS = 32 - NUM_TABLE_BITS - NUM_COLUMN_BITS;

    private static final int SCHEMA_ID_BITMASK = toBitMask(NUM_SCHEMA_BITS);
    private static final int SCHEMA_ID_OFFSET = NUM_TABLE_BITS + NUM_COLUMN_BITS;

    private static final int TABLE_ID_BITMASK = toBitMask(NUM_TABLE_BITS);
    private static final int TABLE_ID_OFFSET = NUM_COLUMN_BITS;

    private static final int COLUMN_ID_BITMASK = toBitMask(NUM_COLUMN_BITS);

    public static final int MIN_SCHEMA_NUMBER = 0;
    public static final int MAX_SCHEMA_NUMBER = twoToThePowerOf(NUM_SCHEMA_BITS) - 1; // 2 ^ 4

    public static final int MIN_TABLE_NUMBER = 0;
    public static final int MAX_TABLE_NUMBER = twoToThePowerOf(NUM_TABLE_BITS) - 2; // 2^20 - 2

    public static final int MIN_COLUMN_NUMBER = 0;
    public static final int MAX_COLUMN_NUMBER = twoToThePowerOf(NUM_COLUMN_BITS) - 2; // 2^8 - 2

    private static int toBitMask(int numBits) {
        return twoToThePowerOf(numBits) - 1;
    }

    private static int twoToThePowerOf(int x) {
        return 1 << x;
    }

    private IdUtils() {
    }

    /**
     * Creates a global ID for the specified target.
     * 
     * @param localSchemaId
     *        is a unique number for the schema between {@value #MIN_SCHEMA_NUMBER} and {@value #MAX_SCHEMA_NUMBER}
     * @param localTableId
     *        is a unique number for the table within its schema between {@value #MIN_TABLE_NUMBER} and
     *        {@value #MAX_TABLE_NUMBER}
     * @param localColumnId
     *        is the offset of the column within its table between {@value #MIN_COLUMN_NUMBER} and
     *        {@value #MAX_COLUMN_NUMBER}
     * @return the global ID
     */
    public static int createGlobalId(final int localSchemaId, final int localTableId, final int localColumnId) {
        return ((localSchemaId & SCHEMA_ID_BITMASK) << SCHEMA_ID_OFFSET)
                | ((localTableId & TABLE_ID_BITMASK) << TABLE_ID_OFFSET)
                | ((localColumnId) & COLUMN_ID_BITMASK);
    }

    /**
     * Creates a global ID for the specified target.
     * 
     * @param localSchemaId
     *        is a unique number for the schema between {@value #MIN_SCHEMA_NUMBER} and {@value #MAX_SCHEMA_NUMBER}
     * @param localTableId
     *        is a unique number for the table within its schema between {@value #MIN_TABLE_NUMBER} and
     *        {@value #MAX_TABLE_NUMBER}
     * @return the global ID
     */
    public static int createGlobalId(final int localSchemaId, final int localTableId) {
        return createGlobalId(localSchemaId, localTableId, MAX_COLUMN_NUMBER + 1);
    }

    /**
     * Creates a global ID for the specified target.
     * 
     * @param localSchemaId
     *        is a unique number for the schema between {@value #MIN_SCHEMA_NUMBER} and {@value #MAX_SCHEMA_NUMBER}
     * @return the global ID
     */
    public static int createGlobalId(final int localSchemaId) {
        return createGlobalId(localSchemaId, MAX_TABLE_NUMBER + 1, MAX_COLUMN_NUMBER + 1);
    }

    /**
     * Returns the local schema ID that is encoded in the given global ID.
     * 
     * @param globalId
     *        is the ID from which the local schema ID shall be extracted
     * @return the local schema ID
     */
    public static int getLocalSchemaId(final int globalId) {
        return (globalId >> SCHEMA_ID_OFFSET) & SCHEMA_ID_BITMASK;
    }

    /**
     * Returns the local table ID that is encoded in the given global ID.
     * 
     * @param globalId
     *        is the ID from which the local table ID shall be extracted
     * @return the local table ID
     */
    public static int getLocalTableId(final int globalId) {
        return (globalId >> TABLE_ID_OFFSET) & TABLE_ID_BITMASK;
    }

    /**
     * Returns the local column ID that is encoded in the given global ID.
     * 
     * @param globalId
     *        is the ID from which the local column ID shall be extracted
     * @return the local column ID
     */
    public static int getLocalColumnId(final int globalId) {
        return globalId & COLUMN_ID_BITMASK;
    }

    public static boolean isSchemaId(final int id) {
        return (getLocalTableId(id) > MAX_TABLE_NUMBER && getLocalColumnId(id) > MAX_COLUMN_NUMBER);
    }

    public static boolean isTableId(final int id) {
        return (getLocalColumnId(id) > MAX_TABLE_NUMBER && !(getLocalTableId(id) > MAX_COLUMN_NUMBER));
    }

}
