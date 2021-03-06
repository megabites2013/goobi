package de.sub.goobi.converter;
/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - http://www.intranda.com
 *          - http://digiverso.com 
 *          - http://www.goobi.org
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
import static org.junit.Assert.*;

import org.goobi.production.flow.statistics.enums.CalculationUnit;
import org.junit.Test;

public class StatisticsCalculationUnitConverterTest {

    @Test
    public void testGetAsObject() {
        StatisticsCalculationUnitConverter conv = new StatisticsCalculationUnitConverter();
        assertEquals(CalculationUnit.volumes, conv.getAsObject(null, null, null));

        assertEquals(CalculationUnit.volumes, conv.getAsObject(null, null, "1"));
        assertEquals(CalculationUnit.pages, conv.getAsObject(null, null, "2"));
        assertEquals(CalculationUnit.volumesAndPages, conv.getAsObject(null, null, "3"));
        assertEquals(CalculationUnit.volumes, conv.getAsObject(null, null, "4"));
    }

    @Test
    public void testGetAsString() {
        StatisticsCalculationUnitConverter conv = new StatisticsCalculationUnitConverter();
        assertEquals(CalculationUnit.volumes.getId(), conv.getAsString(null, null, null));
        assertEquals(CalculationUnit.volumes.getId(), conv.getAsString(null, null, 42));
        assertEquals(CalculationUnit.volumes.getId(), conv.getAsString(null, null, CalculationUnit.volumes));
        assertEquals(CalculationUnit.pages.getId(), conv.getAsString(null, null, CalculationUnit.pages));
        assertEquals(CalculationUnit.volumesAndPages.getId(), conv.getAsString(null, null, CalculationUnit.volumesAndPages));
        
    }

}
