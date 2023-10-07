/*
 * This file is part of the tool MimaFlux.
 * https://github.com/mattulbrich/mimaflux
 *
 * MimaFlux is a time travel debugger for the Minimal Machine
 * used in Informatics teaching at a number of schools.
 *
 * The system is protected by the GNU General Public License Version 3.
 * See the file LICENSE in the main directory of the project.
 *
 * (c) 2016-2022 Karlsruhe Institute of Technology
 *
 * Adapted for Mima by Mattias Ulbrich
 */
package edu.kit.kastel.formal.mimaflux.capacitor;

public class Constants {
    public static final String START_LABEL = "START";

    public static final int VALUE_WIDTH = 24;
    public static final int VALUE_RANGE = 1 << VALUE_WIDTH;
    public static final int VALUE_MASK = VALUE_RANGE - 1;
    public static final int SIGNBIT = 1 << (VALUE_WIDTH - 1);
    public static final String UNKNOWN_OPCODE = "???";

    public static final int ADDRESS_WIDTH = 20;
    public static final int ADDRESS_RANGE = 1 << ADDRESS_WIDTH;
    public static final int ADDRESS_MASK = ADDRESS_RANGE - 1;

    /**
     * A value is ok between -2^23 and 2^24-1.
     * values can be specified as unsigned or as signed constants ...
     */
    public static boolean isValue(int number) {
        return ~VALUE_MASK <= number && number < VALUE_RANGE;
    }

    /**
     * Addresses must be positive and in range
     */
    public static boolean isAddress(int number) {
        return 0 <= number && number < ADDRESS_RANGE;
    }
}
