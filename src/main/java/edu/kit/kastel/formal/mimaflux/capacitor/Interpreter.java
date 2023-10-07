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

import edu.kit.kastel.formal.mimaflux.capacitor.generated.MimaAsmParser.FileContext;
import edu.kit.kastel.formal.mimaflux.capacitor.generated.MimaAsmParser;
import edu.kit.kastel.formal.mimaflux.capacitor.generated.MimaAsmLexer;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {
    private String fileContent;
    private Map<String, Integer> labelMap;
    private List<Command> commands;

    private Map<Integer, Integer> initialValues = new HashMap<>();

    public void parseFile(String fileName) throws IOException {
        String fileContent = Files.readString(Paths.get(fileName));
        parseString(fileContent);
    }

    public void parseString(String fileContent) {

        this.fileContent = fileContent;

        CharStream input = CharStreams.fromString(fileContent);
        MimaAsmLexer lexer = new MimaAsmLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MimaAsmParser parser = new MimaAsmParser(tokens);
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException("line " + line + ":" +
                        charPositionInLine + ": " + msg);
            }
        });
        FileContext content = parser.file();
        ProgramVisitor pv = new ProgramVisitor();
        content.accept(pv);

        this.commands = pv.getCommands();
        LabelResolver lr = new LabelResolver();
        lr.resolve(commands);
        labelMap = lr.getLabelMap();

    }

    public Timeline makeTimeline(Logger logger, int maxSteps, List<AddressRange> printRanges) {

        TimelineBuilder builder = new TimelineBuilder(fileContent, labelMap, commands, initialValues, logger);
        State state = builder.exposeState();

        logger.debug(" ---- initial state");
        logger.debug(state.stringRepresentation(labelMap, printRanges));

        int count = 0;
        loop:
        while (builder.size() < maxSteps) {
            int ir = state.get(state.get(State.IAR));
            int arg = ir & Constants.ADDRESS_MASK;
            int opcode = ir >> 20;
            int tmp;
            switch (opcode) {
                case 0x0:
                    builder.set(State.ACCU, ir);
                    builder.incIAR();
                    break;
                case 0x1:
                    builder.set(State.ACCU, state.get(arg));
                    builder.incIAR();
                    break;
                case 0x2:
                    builder.set(arg, state.get(State.ACCU));
                    builder.incIAR();
                    break;
                case 0x3:
                    op(builder, arg, Integer::sum);
                    break;
                case 0x4:
                    op(builder, arg, (x, y) -> x & y);
                    break;
                case 0x5:
                    op(builder, arg, (x, y) -> x | y);
                    break;
                case 0x6:
                    op(builder, arg, (x, y) -> x ^ y);
                    break;
                case 0x7:
                    op(builder, arg, (x, y) -> x == y ? -1 : 0);
                    break;
                case 0x8:
                    builder.set(State.IAR, arg);
                    break;
                case 0x9:
                    if ((state.get(State.ACCU) & Constants.SIGNBIT) != 0) {
                        builder.set(State.IAR, arg);
                    } else {
                        builder.incIAR();
                    }
                    break;
                case 0xa:
                    tmp = state.get(arg);
                    // throw away bits above address range ...
                    tmp = state.get(tmp & Constants.ADDRESS_MASK);
                    builder.set(State.ACCU, tmp);
                    builder.incIAR();
                    break;
                case 0xb:
                    tmp = state.get(arg);
                    builder.set(tmp & Constants.ADDRESS_MASK, state.get(State.ACCU));
                    builder.incIAR();
                    break;
                case 0xc:
                    builder.set(arg, state.get(State.IAR) + 1);
                    builder.set(State.IAR, (arg + 1) & Constants.ADDRESS_MASK);
                    break;
                case 0xd:
                    builder.set(State.IAR, state.get(arg) & Constants.ADDRESS_MASK);
                    break;

                case 0xf:
                    switch (arg) {
                        case 0x00000:
                            builder.commit();
                            break loop;
                        case 0x10000:
                            builder.set(State.ACCU, (~state.get(State.ACCU)) & Constants.VALUE_MASK);
                            builder.incIAR();
                            break;
                        case 0x20000:
                            tmp = state.get(State.ACCU);
                            tmp = (tmp >> 1) | ((tmp & 1) << (Constants.VALUE_WIDTH - 1));
                            builder.set(State.ACCU, tmp);
                            builder.incIAR();
                            break;
                        default:
                            builder.commit();
                            break loop;
                    }
                    break;
                default:
                    builder.commit();
                    break loop;
            }
            builder.commit();

            logger.debug(" ---- After step " + builder.size());
            logger.debug(state.stringRepresentation(labelMap));
        }

        logger.debug(" ---- Finished interpretation");

        return builder.build();
    }

    private void op(TimelineBuilder builder, int arg, BinaryIntFunction fun) {
        State state = builder.exposeState();
        int op1 = state.get(State.ACCU);
        int op2 = state.get(arg);
        int res = fun.apply(op1, op2) & Constants.VALUE_MASK;
        builder.set(State.ACCU, res);
        builder.incIAR();
    }

    public Map<String, Integer> getLabelMap() {
        return labelMap;
    }

    public void addPresetValue(Integer addr, Integer val) {
        initialValues.put(addr, val);
    }

    public void addLabelValue(String label, Integer val) {
        labelMap.put(label, val);
    }
}
