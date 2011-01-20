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
/*
*ToolbarItemGroup.fx
*
*Created on mar 15, 2010, 10:53:55 fm
*/

package com.eviware.loadui.fx.ui.toolbar;


import javafx.scene.CustomNode;
import javafx.scene.Group;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.TextOrigin;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Polygon;
import javafx.scene.control.Button;
import javafx.scene.layout.LayoutInfo;

import org.jfxtras.scene.shape.MultiRoundRectangle;

/**
 * Graphical node used by the Toolbar to represent a group of ToolbarItems. 
 *
 * @author dain.nilsson
 */
public class ToolbarItemGroup extends CustomNode {

	override var styleClass = "toolbar-item-group";
	
	/**
	 * The background color of expander button 
	 */
	public var expanderButtonBackgroundFill: Paint = Color.rgb( 0x60, 0x60, 0x60 );
	
	/**
	 * The background color of expander button arrow
	 */
	public var expanderButtonArrowFill: Paint = Color.rgb( 0x22, 0x22, 0x22 );
	
	/**
	 * Text color 
	 */
	public var textFill: Paint = Color.rgb( 0x9c, 0x9c, 0x9c );
	
	public var font:Font;
	
	/**
	 * Become true when mouse enters expanderButton and false when exits it. 
	 * This is used to change style on hover.
	 */
	public var expanderButtonHover: Boolean = false;
	
	/**
	 * A ToolbarExpander used to place the ToolbarItems in this ToolbarGroup into when in an expanded state.
	 */
	public-init var expandedGroup:ToolbarExpander;

	def frame = ToolbarItemFrame {}
	
	/**
	 * The ToolbarItems contained in this ToolbarGroup.
	 */
	public var items: ToolbarItem[] on replace {
		if( sizeof items == 0 ) {
			clearFrame();
		} else {
			setupFrame();
		}
	}
	
	/**
	 * The category of the ToolbarItems in this ToolbarItemGroup.
	 */
	public-init var category:String;
	var label:String;
	
	function expand():Void {
		delete collapsedGroup from group.content;
		clearFrame();
		expandedGroup.group = this;
	}
	
	package function collapse():Void {
		setupFrame();
		insert collapsedGroup into group.content;
	}
	
	function clearFrame() {
		frame.item = null;
	}
	
	function setupFrame() {
		def item = items[0];
		label = item.label;
		frame.item = item;
	}
	
	def expanderButton:Group = Group {
		layoutX: 85
		layoutY: Toolbar.GROUP_HEIGHT / 2 - 20
		visible: bind sizeof items > 1
		onMouseClicked: function( e:MouseEvent ) {
			expand();
		}
		onMouseEntered: function(e: MouseEvent):Void {
        	expanderButtonHover = true;
	    }
	    onMouseExited: function(e: MouseEvent):Void {
	        expanderButtonHover = false;
	    }
		content: [
			MultiRoundRectangle {
				width: 24
				height: 35
				topLeftHeight: 3
				topLeftWidth: 4
				bottomLeftHeight: 3
				bottomLeftWidth: 4
				fill: bind expanderButtonBackgroundFill
				stroke: null
			}, Polygon {
				fill: bind expanderButtonArrowFill
				layoutX: 10
				layoutY: 13
				points: [
					0, 0,
					4, 4,
					4, 6,
					0, 10
				]
			}
		]
	}
	var btn:Button;
	def collapsedGroup:Group = Group {
		content: [
			Text {
				x: 13
				y: 12
				content: category
				textOrigin: TextOrigin.TOP
				font: bind font
				fill: bind textFill
			}, frame, btn = Button {
				styleClass: "expander-button"
				graphic: Polygon {
					fill: bind if(btn.hover) Color.web("#222222") else Color.web("#4b4b4b")
					points: [
						0, 0,
						4, 4,
						4, 6,
						0, 10
					]
				}
				layoutInfo: LayoutInfo { width: 24, height: 35 }
				layoutX: 73
				layoutY: Toolbar.GROUP_HEIGHT / 2 - 20
				visible: bind sizeof items > 1
				action: expand
			} // expanderButton
		]
	}
	
	var group:Group;
	
	override function create() {
		group = Group {
			layoutY: -12
			content: collapsedGroup
		}
	}
	
	override function toString():String {
		category
	}

}