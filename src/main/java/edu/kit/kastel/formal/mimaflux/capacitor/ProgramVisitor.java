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

import edu.kit.kastel.formal.mimaflux.capacitor.generated.MimaAsmParser.Adr_specContext;
import edu.kit.kastel.formal.mimaflux.capacitor.generated.MimaAsmParser.CommandContext;
import edu.kit.kastel.formal.mimaflux.capacitor.generated.MimaAsmParser.Label_specContext;
import edu.kit.kastel.formal.mimaflux.capacitor.generated.MimaAsmBaseVisitor;

import java.util.ArrayList;
import java.util.List;

public class ProgramVisitor extends MimaAsmBaseVisitor<Void> {

    List<Command> commands = new ArrayList<>();

    private int curAddress = 0;

    @Override
    public Void visitAdr_spec(Adr_specContext ctx) {
        curAddress = Integer.decode(ctx.NUMBER().getText());
        if(curAddress >= Constants.ADDRESS_MASK) {
            throw new TokenedException(ctx.NUMBER().getSymbol(), "Address out of range");
        }
        return null;
    }

    @Override
    public Void visitLabel_spec(Label_specContext ctx) {
        try {
            Integer number = Integer.decode(ctx.NUMBER().getText());
            if (!Constants.isValue(number)) {
                throw new TokenedException(ctx.NUMBER().getSymbol(), ctx.NUMBER().getText() + " is out of range for a 24-bit value.");
            }
            commands.add(new Command(number, ctx.ID().getText(), null, null, 0, null));
            return null;
        } catch (NumberFormatException e) {
            throw new TokenedException(ctx.NUMBER().getSymbol(), ctx.NUMBER().getText() + " out of range.");
        }
    }

    @Override
    public Void visitCommand(CommandContext ctx) {
        String label = null;
        if (ctx.label != null) {
            label = ctx.label.getText();
        }
        int valueArg = 0;
        if (ctx.numberArg != null) {
            try {
                valueArg = Integer.decode(ctx.numberArg.getText());
            } catch (NumberFormatException e) {
                throw new TokenedException(ctx.numberArg, ctx.numberArg.getText() + " out of range.");
            }
        }

        String labelArg = null;
        if (ctx.idArg != null) {
            labelArg = ctx.idArg.getText();
        }

        String instr;
        if (ctx.mnemomicWith() == null) {
            instr = ctx.mnemomicWithout().getText();
        } else {
            instr = ctx.mnemomicWith().getText();
        }

        commands.add(new Command(curAddress, label, instr, labelArg, valueArg, ctx));
        curAddress ++;
        return null;
    }

    public List<Command> getCommands() {
        return commands;
    }
}
