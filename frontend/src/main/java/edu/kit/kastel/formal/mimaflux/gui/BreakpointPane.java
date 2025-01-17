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

// Original header:

/*
 * This file is part of
 *    ivil - Interactive Verification on Intermediate Language
 *
 * Copyright (C) 2009-2012 Karlsruhe Institute of Technology
 *
 * The system is protected by the GNU General Public License.
 * See LICENSE.TXT (distributed with this file) for details.
 */
package edu.kit.kastel.formal.mimaflux.gui;

import edu.kit.kastel.formal.mimaflux.capacitor.Logger;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

/**
 * BreakpointPane is a specialised TextArea which allows to:
 * <ul>
 * <li>set breakpoints
 * <li>display breakpoints
 * </ul> 
 * 
 * It is connected to a {@link BreakpointManager}.
 */
public class BreakpointPane extends BracketMatchingTextArea implements Observer {
    @Serial
    private static final long serialVersionUID = -5566042549810690095L;

    private static final Font FONT = new Font(Font.MONOSPACED, Font.BOLD, 16);

    private static final Color HIGHLIGHT_COLOR = new Color(0x60c060);

    private static final Icon BULLET_ICON = new BulletIcon();
    private static final HighlightPainter BAR_PAINTER = 
            new BarHighlightPainter(HIGHLIGHT_COLOR);

    private final BreakpointManager breakpointManager;
    private Object breakPointResource;
    private final List<Object> lineHighlights = new ArrayList<>();
    private final Logger logger;
    
    public BreakpointPane(BreakpointManager breakpointManager,
            boolean showLineNumbers, Logger logger) {
        super();
        this.breakpointManager = breakpointManager;
        init(showLineNumbers);
        Caret newCaret = new NotScrollingCaret();
        setCaret(newCaret);
        this.logger = logger;
    }

    private void init(boolean showLineNumbers) {
        {
            // Borders
            BulletBorder breakpointBorder = new BulletBorder();
            Border secondBorder;
            if(showLineNumbers) {
                secondBorder = new LineNrBorder(Color.lightGray);
            } else {
                secondBorder = new EmptyBorder(0, BULLET_ICON.getIconWidth(), 0, 0);
            }

            setBorder(new CompoundBorder(breakpointBorder, secondBorder));
        }

        if(FONT != null)
            setFont(FONT);
        
        setEditable(false);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }
        });
        
        this.breakpointManager.addObserver(this);
    }
    
    private void showPopup(MouseEvent e) {
        
        if(breakPointResource == null)
            return;
        
        int offset = viewToModel2D(e.getPoint());
        final int line;
        try {
            line = getLineOfOffset(offset);
        } catch (BadLocationException ex) {
            throw new Error(ex);
        }
        
        boolean hasBreakPointHere = breakpointManager.hasBreakpoint(breakPointResource, line);

        JMenuItem item = getBreakpointAction(hasBreakPointHere, line);

        JPopupMenu popup = new JPopupMenu();
        popup.add(item);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private JMenuItem getBreakpointAction(boolean hasBreakPointHere, int line) {
        JMenuItem item;
        if (hasBreakPointHere) {
            item = new JMenuItem("Remove this breakpoint");
            item.addActionListener(e -> {
                breakpointManager.removeBreakpoint(breakPointResource, line);
                repaint();
            });
        } else {
            item = new JMenuItem("Set breakpoint here");
            item.addActionListener(e -> {
                breakpointManager.addBreakpoint(breakPointResource, line);
                repaint();
            });
        }
        return item;
    }

    public void removeHighlights() {
        Highlighter highlighter = getHighlighter();
        for (Object hl : lineHighlights) {
            highlighter.removeHighlight(hl);
        }
        lineHighlights.clear();
        repaint();
    }

    /**
     * Adds a highlighted line to the display.
     * 
     * @param line
     *            the line to highlight in the display
     */
    public void addHighlight(int line) {

        try {
            int begin = getLineStartOffset(line);
            Object tag = getHighlighter().addHighlight(begin, begin, BAR_PAINTER);
            lineHighlights.add(tag);
            
            // make this line visible
            Rectangle point = modelToView(begin);
            if(point != null)
                scrollRectToVisible(point);
            repaint();
        } catch (BadLocationException e) {
            // throw new Error(e);
            logger.debug("Illegal line number " + line
                    + " referenced for " + getBreakPointResource());
            logger.logStacktrace(e);
        }
    }
    
    private class BulletBorder extends AbstractBorder {
        
        @Serial
        private static final long serialVersionUID = 487188734129249672L;
        
        public void paintBorder(Component c, Graphics g, int x, int y, int width,
                int height) {

            if(breakPointResource == null)
                return;

            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            FontMetrics fm = g.getFontMetrics();
            int step = fm.getHeight();
            int offset = (step - BULLET_ICON.getIconHeight()) / 2;

            for (int line : breakpointManager.getBreakpoints(breakPointResource)) {
                BULLET_ICON.paintIcon(c, g, x, step*line + offset);
            }
        }
        
    }

    @Override 
    public void update(Observable o, Object arg) {
        if(Objects.equals(arg, breakPointResource)) {
            repaint();
        }
    }

    public Object getBreakPointResource() {
        return breakPointResource;
    }

    public void setBreakPointResource(Object breakPointResource) {
        this.breakPointResource = breakPointResource;
        repaint();
    }

    
}
