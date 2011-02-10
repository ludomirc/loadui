/* 
 * Copyright 2011 eviware software ab
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
/*
*TriggerComponentNode.fx
*
*Created on apr 14, 2010, 16:28:12 em
*/

package com.eviware.loadui.fx.widgets.canvas;

import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.control.Separator;
import javafx.scene.layout.LayoutInfo;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.geometry.Insets;

import com.eviware.loadui.fx.FxUtils;
import com.eviware.loadui.fx.ui.layout.widgets.OnOffSwitch;
import com.eviware.loadui.fx.ui.resources.ResizablePath;

import com.eviware.loadui.api.property.Property;
import com.eviware.loadui.api.events.PropertyEvent;
import com.eviware.loadui.api.component.categories.OnOffCategory;

import java.util.EventObject;

public class OnOffComponentNode extends ComponentNode {
	
	override var roundedFrame = OnOffFrame {
		fill: roundedFrameFill
		stroke: roundedFrameStroke
		layoutInfo: LayoutInfo { height: 50, vfill: true, hfill: true }
	}
	
	var stateProperty:Property;
	var onState:Boolean on replace oldVal {
		stateProperty.setValue( onState );
	}
	
	override var component on replace {
		stateProperty = (component.getBehavior() as OnOffCategory).getStateProperty();
		onState = stateProperty.getValue() as Boolean;
	}
	
	override function create():Node {
		def dialog = super.create();
		insert [
			OnOffSwitch {
				state: bind onState with inverse
				layoutInfo: LayoutInfo { margin: Insets { bottom: -8, left: -5, right: -2 } }
			}, Separator {
				vertical: true
			}
		] before toolbar.content[0];
		
		dialog;
	}
	
	override function handleEvent( e:EventObject ) {
		super.handleEvent( e );
		if( e instanceof PropertyEvent ) {
			def event = e as PropertyEvent;
			if( event.getEvent() == PropertyEvent.Event.VALUE and event.getProperty() == stateProperty ) {
				FxUtils.runInFxThread( function():Void { onState = stateProperty.getValue() as Boolean } );
			}
		}
	}
}

class OnOffFrame extends ResizablePath {
	override function calculatePath() {
		if( compact ) {
			[
				MoveTo { x: 7, y: 0 },
				ArcTo { x: 0, y: 7, radiusX: 7, radiusY: 7 },
				LineTo { x: 0, y: height - 7 },
				ArcTo { x: 7, y: height, radiusX: 7, radiusY: 7 },
				LineTo { x: width - 7, y: height },
				ArcTo { x: width, y: height - 7, radiusX: 7, radiusY: 7 },
				LineTo { x: width, y: 7 },
				ArcTo { x: width - 7, y: 0, radiusX: 7, radiusY: 7 },
				ClosePath {}
			]
		} else {
			[
				MoveTo { y: height - 7 },
				ArcTo { x: 7, y: height, radiusX: 7, radiusY: 7 },
				LineTo { x: width - 7, y: height },
				ArcTo { x: width, y: height - 7, radiusX: 7, radiusY: 7 },
				LineTo { x: width, y: 7 },
				ArcTo { x: width - 7, radiusX: 7, radiusY: 7 },
				LineTo { x: 50 },
				ArcTo { x: 45, y: 5, radiusX: 5, radiusY: 5 },
				ArcTo { x: 40, y: 10, radiusX: 5, radiusY: 5, sweepFlag: true },
				LineTo { x: 7, y: 10 },
				ArcTo { x: 0, y: 17, radiusX: 7, radiusY: 7 },
				ClosePath {}
			]
		}
	}
}