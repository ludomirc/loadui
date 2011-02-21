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
package com.eviware.loadui.fx.statistics.manager;

import javafx.scene.Node;
import javafx.scene.layout.LayoutInfo;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Stack;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Label;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.geometry.HPos;
import com.javafx.preview.control.MenuButton;
import com.javafx.preview.control.MenuItem;

import com.sun.javafx.scene.layout.Region;

import com.eviware.loadui.fx.AppState;
import com.eviware.loadui.fx.ui.ActivityLed;
import com.eviware.loadui.fx.ui.node.BaseNode;
import com.eviware.loadui.fx.ui.resources.DialogPanel;
import com.eviware.loadui.fx.statistics.StatisticsWindow;

import com.eviware.loadui.api.statistics.store.Execution;

public class ResultNodeBase extends BaseNode {
	public var execution:Execution;
	
	public var label:String = "Execution";
	
	public var active = false;
	
	protected var menuButton:MenuButton;
	
	override var styleClass = "result-node-base";
	
	init {
		addMouseHandler( MOUSE_PRIMARY_CLICKED, function( e:MouseEvent ):Void {
			if( e.clickCount == 2 ) {
				StatisticsWindow.execution = execution;
				AppState.byName( "STATISTICS" ).transitionTo( StatisticsWindow.STATISTICS_VIEW, AppState.ZOOM_WIPE );
			}
		} );
	}
	
	override function create():Node {
		DialogPanel {
			layoutInfo: LayoutInfo { width: 155, height: 108 }
			body: VBox {
				padding: Insets { left: 8, right: 8, top: 5, bottom: 8 }
				spacing: 10
				content: [
					menuButton = MenuButton {
						styleClass: bind if( menuButton.showing ) "menu-button-showing" else "menu-button"
						graphic: ActivityLed { active: bind active }
						text: bind label.toUpperCase();
						items: MenuItem {
							text: ##[OPEN]"Open"
							action: function() {
								StatisticsWindow.execution = execution;
								AppState.byName( "STATISTICS" ).transitionTo( StatisticsWindow.STATISTICS_VIEW, AppState.ZOOM_WIPE );
							}
						}
					}, Stack {
						content: [
							Region {
								styleClass: "graph"
							}, SVGPath {
								layoutInfo: LayoutInfo {
									margin: Insets { left: 5, top: 5 }
									vpos: VPos.TOP
									hpos: HPos.LEFT
								}
								fill: Color.rgb( 255,255,255,0.15 )
								content: "m 242.71845,190.87351 -92.23301,0 c -1.807,0.24262 -2.54911,1.55014 -3.07444,3.07443 l 0,43.52751 c 0.37619,1.83599 2.42728,3.12823 4.69256,0 12.25299,-33.96285 30.46724,-37.6855 90.61489,-46.60194 z"
							},
						]
					}
				]
			}
		}
	}
	
	override function toString():String {
		label
	}
}