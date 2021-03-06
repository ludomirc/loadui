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
package com.eviware.loadui.ui.fx.input;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.loadui.ui.fx.api.input.DraggableEvent;
import com.eviware.loadui.ui.fx.api.input.Movable;
import com.eviware.loadui.ui.fx.api.input.Selectable;
import com.google.common.base.Preconditions;

public class MultiMovable
{
	protected static final Logger log = LoggerFactory.getLogger( MultiMovable.class );

	private static final String MOVA_ALONG_NODES_HANDLER_PROP_KEY = MultiMovable.class.getName()
			+ "MOVA_ALONG_NODES_HANDLER";

	private static final EventHandler<MouseEvent> UPDATE_COORDINATES_HANDLER = new EventHandler<MouseEvent>()
	{
		@Override
		public void handle( MouseEvent event )
		{
			for( Selectable s : SelectableImpl.getSelected() )
			{
				Node selectedNode = s.getNode();
				selectedNode.setLayoutX( selectedNode.getLayoutX() + selectedNode.getTranslateX() );
				selectedNode.setLayoutY( selectedNode.getLayoutY() + selectedNode.getTranslateY() );
				selectedNode.setTranslateX( 0 );
				selectedNode.setTranslateY( 0 );
			}
		}
	};

	private static final class MoveAlongNodesHandler implements EventHandler<DraggableEvent>
	{
		private final Movable movable;

		private MoveAlongNodesHandler( @Nonnull Movable movable )
		{
			this.movable = movable;
		}

		@Override
		public void handle( DraggableEvent event )
		{
			Node movedNode = movable.getNode();
			if( movable.isDragging() && event.getDraggable() == movable )
			{
				double translateX = movedNode.getTranslateX();
				double translateY = movedNode.getTranslateY();
				for( Selectable s : SelectableImpl.getSelected() )
				{
					Node selectedNode = s.getNode();
					if( MovableImpl.isMovable( selectedNode ) && MovableImpl.getMovable( selectedNode ) != event.getDraggable() )
					{
						selectedNode.setTranslateX( translateX );
						selectedNode.setTranslateY( translateY );
						selectedNode.fireEvent( event );
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param selectionArea
	 *           Selection area that must already be installed using Selectable.
	 * @param movable
	 *           The node must already be Movable and Selectable.
	 */

	public static void install( @Nonnull final Region selectionArea, @Nonnull final Node node )
	{
		Preconditions.checkArgument( SelectableImpl.isSelectable( node ), "The node must already be Selectable." );
		Preconditions.checkArgument( MovableImpl.isMovable( node ), "The node must already be Movable." );

		final Movable movable = MovableImpl.getMovable( node );
		MoveAlongNodesHandler handler = new MoveAlongNodesHandler( movable );
		node.getProperties().put( MOVA_ALONG_NODES_HANDLER_PROP_KEY, handler );
		node.addEventHandler( DraggableEvent.DRAGGABLE_DRAGGED, handler );
		node.addEventHandler( MouseEvent.MOUSE_RELEASED, UPDATE_COORDINATES_HANDLER );
	}

	@SuppressWarnings( "unchecked" )
	public static void uninstall( @Nonnull final Region selectionArea, @Nonnull final Node node )
	{
		log.debug( "uninstall multimovable" );
		if( MovableImpl.isMovable( node ) )
		{
			node.removeEventHandler( DraggableEvent.DRAGGABLE_DRAGGED, ( EventHandler<DraggableEvent> )node
					.getProperties().get( MOVA_ALONG_NODES_HANDLER_PROP_KEY ) );
		}
		node.removeEventHandler( MouseEvent.MOUSE_RELEASED, UPDATE_COORDINATES_HANDLER );
	}
}
