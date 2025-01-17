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

import edu.kit.kastel.formal.mimaflux.capacitor.generated.MimaAsmParser.CommandContext;
import org.antlr.v4.runtime.Token;

public record Command(int address, String label, String instruction,
                      String labelArg, int valueArg,
                      CommandContext ctx) {
    public Command updateArg(int val) {
        return new Command(address, label, instruction, labelArg, val, ctx);
    }

    public int getMnemonicLine() {
        if(ctx.mnemomicWith() == null) {
            return ctx.mnemomicWithout().getStart().getLine();
        } else {
            return ctx.mnemomicWith().getStart().getLine();
        }
    }

    public Token getMnemonic() {
        if (ctx.mnemomicWith() != null) {
            return ctx.mnemomicWith().getStart();
        } else {
            return ctx.mnemomicWithout().getStart();
        }
    }
}
