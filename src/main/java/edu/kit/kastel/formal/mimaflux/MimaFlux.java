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
package edu.kit.kastel.formal.mimaflux;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import edu.kit.kastel.formal.mimaflux.capacitor.Interpreter;
import edu.kit.kastel.formal.mimaflux.capacitor.Logger;
import edu.kit.kastel.formal.mimaflux.capacitor.MimaException;
import edu.kit.kastel.formal.mimaflux.capacitor.MimaVerification;
import edu.kit.kastel.formal.mimaflux.capacitor.State;
import edu.kit.kastel.formal.mimaflux.capacitor.Timeline;
import edu.kit.kastel.formal.mimaflux.gui.GUI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.List;

public class MimaFlux {

    public static final String VERSION;

    static {
        URL u = MimaFlux.class.getResource("/VERSION");
        String version = "<unknown>";
        try {
            assert u != null;
            try (InputStream in = u.openStream()) {
                version = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        VERSION = version;
    }

    public static MimaFluxArgs mmargs;

    public static void main(String[] args) throws Exception {
        try {
            Timeline timeline = null;
            mmargs = new MimaFluxArgs();
            JCommander jc = JCommander.newBuilder()
                    .addObject(mmargs)
                    .build();
            jc.parse(args);

            logger.info("Mima Flux Capacitor " + VERSION);

            if (mmargs.help) {
                jc.usage();
                System.exit(0);
            }

            if (mmargs.verifyFile != null) {
                if (mmargs.fileName == null) {
                    throw new MimaException("A filename must be provided in -verify mode.");
                }
                MimaVerification verification = new MimaVerification(logger, mmargs.maxSteps, mmargs.printRanges);
                int res = verification.verify(mmargs.verifyFile, mmargs.fileName);
                System.exit(res);
            }

            if (mmargs.fileName == null) {
                if (mmargs.autoRun) {
                    throw new MimaException("A filename must be provided in -run mode.");
                }
            } else {
                Interpreter interpreter = new Interpreter();
                interpreter.parseFile(mmargs.fileName);
                loadTestCaseInitialValues(mmargs.loadTest, interpreter);
                setInitialValues(mmargs.assignments, interpreter);
                timeline = interpreter.makeTimeline(logger, mmargs.maxSteps, mmargs.printRanges);
            }

            if (mmargs.autoRun) {
                assert timeline != null;
                timeline.setPosition(timeline.countStates() - 1);
                timeline.exposeState().stringRepresentation(timeline.getLabelMap(), mmargs.printRanges);
                ensureTests(timeline);
                System.exit(0);
            } else {
                GUI gui = new GUI(timeline, mmargs.fileName, logger, mmargs.maxSteps, mmargs.printRanges);
                gui.setVisible(true);
            }
        } catch (NoSuchFileException ex) {
            exit(new IOException("File not found: " + ex.getMessage(), ex));
        } catch (ParameterException parameterException) {
            logger.error(parameterException.getMessage());
            parameterException.usage();
        } catch (Exception ex) {
            exit(ex);
        }
    }

    private static void loadTestCaseInitialValues(String loadTest, Interpreter interpreter) throws IOException, MimaException {
        if (loadTest == null) {
            return;
        }

        int hash = loadTest.lastIndexOf('#');
        String file = loadTest.substring(0, hash);
        String testcase = loadTest.substring(hash + 1);
        MimaVerification mv = new MimaVerification(logger, mmargs.maxSteps, mmargs.printRanges);
        mv.setInitialValues(file, testcase, interpreter);
    }

    private static void ensureTests(Timeline timeline) {
        if (mmargs.tests == null) {
            return;
        }

        for (String test : mmargs.tests) {
            try {
                logger.error("Checking " + test);
                String[] parts = test.trim().split(" *= *");
                if (parts.length != 2) {
                    throw new IllegalArgumentException();
                }
                Integer resolved = timeline.getLabelMap().get(parts[0]);
                if (resolved == null) {
                    resolved = Integer.decode(parts[0]);
                }
                Integer val = Integer.decode(parts[1]);

                int observed = timeline.exposeState().get(resolved);
                if (observed != val) {
                    logger.error(" ... violated. Expected value %d (0x%x) at address %s, but observed %d (0x%x).".formatted(
                            val, val, parts[0], observed, observed));
                    throw new MimaException("Test failed.");
                } else {
                    logger.error(" ... checked.");
                }
            } catch (Exception exception) {
                logger.logStacktrace(exception);
                throw new IllegalArgumentException("Wrong test specification: " + test);
            }
        }
    }

    private static void setInitialValues(List<String> assignments, Interpreter interpreter) {

        // Make _accu and _iar available.
        interpreter.getLabelMap().put("_accu", State.ACCU);
        interpreter.getLabelMap().put("_iar", State.IAR);

        if (assignments == null) {
            return;
        }
        for (String assignment : assignments) {
            try {
                String[] parts = assignment.trim().split(" *= *");
                if (parts.length != 2) {
                    throw new IllegalArgumentException();
                }
                Integer resolved = interpreter.getLabelMap().get(parts[0]);
                if (resolved == null) {
                    resolved = Integer.decode(parts[0]);
                }
                Integer val = Integer.decode(parts[1]);
                interpreter.addPresetValue(resolved, val);
            } catch (Exception exception) {
                logger.logStacktrace(exception);
                throw new IllegalArgumentException("Wrong set specification: " + assignment);
            }
        }
    }

    static Logger logger = new Logger() {
        @Override
        public void log(String message, Level level) {
            switch (level) {
                case DEBUG -> {
                    if (mmargs.verbose) {
                        System.out.println(message);
                    }
                }
                case INFO -> {
                    System.out.println(message);
                }
                case ERROR -> {
                    System.err.println(message);
                }
            }
        }
    };

    static void exit(Exception exception) {
        logger.error(exception.getMessage());
        logger.logStacktrace(exception);
        System.exit(1);
    }
}
