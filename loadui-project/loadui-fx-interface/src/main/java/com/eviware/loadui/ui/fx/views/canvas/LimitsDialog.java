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
package com.eviware.loadui.ui.fx.views.canvas;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import javax.annotation.Nonnull;

import com.eviware.loadui.api.model.CanvasItem;
import com.eviware.loadui.ui.fx.control.ConfirmationDialog;
import com.eviware.loadui.ui.fx.control.SettingsDialog;
import com.eviware.loadui.ui.fx.control.fields.ValidatableLongField;

import static com.eviware.loadui.ui.fx.control.fields.ValidatableLongField.EMPTY_TO_NEGATIVE_ONE;
import static com.eviware.loadui.ui.fx.control.fields.ValidatableLongField.IS_EMPTY;
import static com.eviware.loadui.ui.fx.control.fields.ValidatableLongField.CONVERTABLE_TO_LONG;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class LimitsDialog extends ConfirmationDialog
{
	private static final Predicate<String> EMPTY_OR_CONVERTABLE_TO_LONG = Predicates.or( IS_EMPTY, CONVERTABLE_TO_LONG );

	public LimitsDialog( @Nonnull Node owner, @Nonnull final CanvasItem canvas )
	{
		super( owner, "Limits", "Save" );

		VBox vBox = new VBox( SettingsDialog.VERTICAL_SPACING );

		final ValidatableLongField timeField = ValidatableLongField.Builder.create()
				.text( longToString( canvas.getLimit( CanvasItem.TIMER_COUNTER ) ) ).id( "time-limit" )
				.stringConstraint( EMPTY_OR_CONVERTABLE_TO_LONG ).convertFunction( EMPTY_TO_NEGATIVE_ONE ).build();

		final ValidatableLongField requestField = ValidatableLongField.Builder.create()
				.text( longToString( canvas.getLimit( CanvasItem.REQUEST_COUNTER ) ) ).id( "request-limit" )
				.stringConstraint( EMPTY_OR_CONVERTABLE_TO_LONG ).convertFunction( EMPTY_TO_NEGATIVE_ONE ).build();

		final ValidatableLongField failureField = ValidatableLongField.Builder.create()
				.text( longToString( canvas.getLimit( CanvasItem.FAILURE_COUNTER ) ) ).id( "failure-limit" )
				.stringConstraint( EMPTY_OR_CONVERTABLE_TO_LONG ).convertFunction( EMPTY_TO_NEGATIVE_ONE ).build();

		vBox.getChildren().addAll( new Label( "Time limit :" ), timeField, new Label( "Request limit :" ), requestField,
				new Label( "Failure limit :" ), failureField );

		getItems().add( vBox );

		setOnConfirm( new EventHandler<ActionEvent>()
		{
			@Override
			public void handle( ActionEvent arg0 )
			{
				if( timeField.validate() && requestField.validate() && failureField.validate() )
				{
					canvas.setLimit( CanvasItem.TIMER_COUNTER, timeField.getFieldValue() );
					canvas.setLimit( CanvasItem.REQUEST_COUNTER, requestField.getFieldValue() );
					canvas.setLimit( CanvasItem.FAILURE_COUNTER, failureField.getFieldValue() );
					close();
				}
			}
		} );
	}

	private static String longToString( long value )
	{
		if( value == -1 )
			return "";
		return Long.toString( value );
	}
}
