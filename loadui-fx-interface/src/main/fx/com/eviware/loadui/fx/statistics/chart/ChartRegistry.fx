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

import javafx.scene.Node;

import com.eviware.loadui.api.statistics.model.chart.*;

/**
 * Creates a Node for a specific ChartView
 */
public function createChart( chartView:ChartView, holder:ChartViewHolder ):BaseChart {
	if( chartView instanceof LineChartView ) {
		LineChart { chartView: chartView as LineChartView, holder: holder }
	} else {
		null
	}
}