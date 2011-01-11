/* 
 * Copyright 2010 eviware software ab
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and limitations
 * under the Licence.
 */
package com.eviware.loadui.fx.statistics.chart;

import java.util.HashMap;
import java.awt.Color;
import java.awt.BasicStroke;

import com.jidesoft.chart.Chart;
import com.jidesoft.chart.style.ChartStyle;

public def chartBackgroundColor = new Color( 0x1a, 0x1a, 0x1a );
public def chartForegroundColor = new Color( 0xcd, 0xcd, 0xcd );
public def lineColor = Color.red;

public def solidStroke = new BasicStroke( 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, [ 1.0 ], 0 );
public def dashedStroke = new BasicStroke( 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, [ 8.0, 8.0 ], 0 );
public def dottedStroke = new BasicStroke( 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, [ 2.0, 2.0 ], 0 );

public function styleChart( chart:Chart ):Void {
	chart.setPanelBackground( chartBackgroundColor );
	chart.setChartBackground( chartBackgroundColor );
	chart.setLabelColor( chartForegroundColor );
	chart.setGridColor( chartForegroundColor );
	chart.setTickColor( chartForegroundColor );
	
	chart.setVerticalGridLinesVisible( false );
	chart.setHorizontalGridLinesVisible( false );
}

public function styleChartStyle( chartStyle:ChartStyle ):Void {
	chartStyle.setLineColor( lineColor );
	chartStyle.setLineStroke( solidStroke );
	
	chartStyle.setPointsVisible( false );
}


var statColorMap: HashMap;

public function getColor(statistic: String) {
    
}

/**
 * Provides default colors for statistics
 *
 * @author predrag.vucetic
 * @author dain.nilsson
 */
public class DefaultChartStyles {
}