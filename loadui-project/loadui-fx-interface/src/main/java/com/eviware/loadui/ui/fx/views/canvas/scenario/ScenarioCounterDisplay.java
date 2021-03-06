/*
 * Copyright 2013 SmartBear Software
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and limitations
 * under the Licence.
 */
package com.eviware.loadui.ui.fx.views.canvas.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;

import javax.annotation.Nonnull;

import com.eviware.loadui.ui.fx.views.canvas.CounterDisplay;
import com.eviware.loadui.util.StringUtils;

public class ScenarioCounterDisplay extends CounterDisplay
{
	public ScenarioCounterDisplay( @Nonnull String name, @Nonnull Formatting format )
	{
		this.formatting = format;

		numberDisplay = numberDisplay();
		numberDisplay.setAlignment( Pos.CENTER_RIGHT );
		
		Label label = label( name );

		HBox counterDisplay = HBoxBuilder
				.create()
				.children( numberDisplay )
				.alignment( Pos.CENTER_RIGHT )
				.style("-fx-background-color: linear-gradient(to bottom, #545454 0%, #000000 50%, #000000 100%); -fx-padding: 0 6 0 6; -fx-background-radius: 5; -fx-border-width: 1; -fx-border-color: #333333; -fx-border-radius: 4; " )
				.build();
		
		getChildren().setAll( counterDisplay, label );
		setSpacing( 0 );
		setAlignment( Pos.CENTER_LEFT );
		setMaxWidth( 34 );
	}

	public ScenarioCounterDisplay( String name )
	{
		this( name, Formatting.NONE );
	}

	@Override
	public void setValue( long value )
	{
		if( formatting == Formatting.TIME )
			numberDisplay.setText( StringUtils.toHhMmSs( value ) );
		else
			numberDisplay.setText( String.valueOf( value ) );
	}
}
