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
package com.eviware.loadui.ui.fx.control;

import com.eviware.loadui.ui.fx.api.LoaduiFXConstants;
import com.eviware.loadui.ui.fx.api.intent.AbortableBlockingTask;
import com.eviware.loadui.ui.fx.api.intent.BlockingTask;
import com.eviware.loadui.ui.fx.api.intent.IntentEvent;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.Tab;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.WindowEvent;

public class DetachableTab extends Tab
{
	private final BooleanProperty detachedProperty = new SimpleBooleanProperty( false );
	private int detachedId;

	public final BooleanProperty detachedProperty()
	{
		return detachedProperty;
	}

	public final boolean isDetached()
	{
		return detachedProperty.get();
	}

	public final void setDetached( boolean detached )
	{
		detachedProperty.set( detached );
	}

	private final ObjectProperty<Pane> detachableContentProperty = new SimpleObjectProperty<>();

	public final ObjectProperty<? extends Pane> detachableContentProperty()
	{
		return detachableContentProperty;
	}

	public final Pane getDetachableContent()
	{
		return detachableContentProperty.get();
	}

	public final void setDetachableContent( Node eventFirer, Pane detachableContent )
	{
		detachableContentProperty.set( detachableContent );
		this.eventFirer = eventFirer;
	}

	private Stage detachedStage;
	private Scene scene;
	private Node eventFirer;
	private final DetachedTabsHolder tabRefs;

	public DetachableTab()
	{
		this( null, DetachedTabsHolder.get() );
	}

	public DetachableTab( String label, DetachedTabsHolder tabRefs )
	{
		super( label );
		this.tabRefs = tabRefs;

		getStyleClass().add( "detachable-tab" );

		contentProperty().bind(
				Bindings
						.when( detachedProperty )
						.<Pane>then(
								PaneBuilder.create().id( "placeholder" ).style( "-fx-background-color: darkgrey;" ).build() )
						.otherwise( detachableContentProperty ) );

		detachedProperty.addListener( new ChangeListener<Boolean>()
		{

			@Override
			public void changed( ObservableValue<? extends Boolean> _, Boolean oldValue, Boolean hasToDetach )
			{
				if( hasToDetach )
					doDetach();
				else
					doReattach();
			}
		} );

		Button detachButton = ButtonBuilder.create().id( "detachButton" ).graphic( RegionBuilder.create().styleClass( "graphic" ).build() )
				.onAction( new EventHandler<ActionEvent>()
				{
					@Override
					public void handle( ActionEvent arg0 )
					{
						setDetached( !isDetached() );
					}
				} ).build();

		//detachButton.visibleProperty().bind( selectedProperty() );
		detachButton.setVisible( false ); /* For now, detachable tabs have too many problems. See LOADUI-869. */

		setGraphic( detachButton );
	}

	private void doDetach()
	{
		final StackPane detachedTabContainer;
		final Node detachableContent = getDetachableContent();
		detachedStage = StageBuilder
				.create()
				.icons( ((Stage) getTabPane().getScene().getWindow()).getIcons() )
				.title( getText() )
				.width( getTabPane().getWidth() )
				.height( getTabPane().getHeight() )
				.scene(
						scene = SceneBuilder
								.create()
								.root(
										detachedTabContainer = StackPaneBuilder.create().children( detachableContent )
												.styleClass( "detached-content" ).build() )
								.stylesheets( LoaduiFXConstants.getLoaduiStylesheets() ).build() ).build();
		detachableContent.setVisible( true );
		detachedStage.setOnHidden( new EventHandler<WindowEvent>()
		{
			@Override
			public void handle( WindowEvent event )
			{
				setDetached( false );
			}
		} );

		BlockingTask.install( scene );
		AbortableBlockingTask.install( scene );
		detachedId = tabRefs.add( detachedTabContainer );

		final EventHandler<Event> intentHandler = new EventHandler<Event>()
		{

			@Override
			public void handle( Event event )
			{
				if( !event.isConsumed() )
					eventFirer.fireEvent( event );
			}
		};
		detachedStage.addEventHandler( IntentEvent.ANY, intentHandler );

		detachedStage.show();
	}

	private void doReattach()
	{
		if( detachedStage != null )
		{
			tabRefs.remove( detachedId );
			detachedStage.setOnHidden( null );
			detachedStage.close();
			detachedStage = null;
		}
		if( scene != null )
		{
			BlockingTask.uninstall( scene );
			AbortableBlockingTask.uninstall( scene );
			scene = null;
		}
	}

}
