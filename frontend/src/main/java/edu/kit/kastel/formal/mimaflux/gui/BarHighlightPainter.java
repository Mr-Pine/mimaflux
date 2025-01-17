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

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.JTextComponent;
import java.awt.*;


public class BarHighlightPainter implements HighlightPainter {
    
    private final Color color;
    
    public BarHighlightPainter(Color color) {
        super();
        this.color = color;
    }

    public void paint(Graphics g, int offs0, int offs1, Shape bounds,
            JTextComponent c) {
        g = g.create();
        try {
            
            if(offs0 > offs1)
                return;
            
            // --- determine locations ---
            TextUI mapper = c.getUI();
            Rectangle p0 = mapper.modelToView(c, offs0);
            Rectangle p1 = mapper.modelToView(c, offs1);
            
            Insets insets = c.getInsets();

            // --- render ---
            g.setColor(color);
            g.fillRect(insets.left, p0.y, 
                    c.getWidth() - insets.left - insets.right, 
                    p1.y-p0.y+p0.height);
            
        } catch (BadLocationException e) {
            // can't render
        }
    }

}
