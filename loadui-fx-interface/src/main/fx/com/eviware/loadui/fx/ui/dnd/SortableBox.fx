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
package com.eviware.loadui.fx.ui.dnd;

import javafx.scene.Node;
import javafx.scene.layout.Resizable;
import javafx.scene.layout.Container;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.geometry.Insets;
import javafx.util.Sequences;

import com.eviware.loadui.fx.ui.node.BaseNode;

/**
 * Acts as a HBox or VBox, but allows the user to reorder the items using drag and drop.
 *
 * @author dain.nilsson
 */
public class SortableBox extends BaseNode, Resizable {
	var box:Container;
	
	public-init var vertical = false on replace {
		box = if( vertical ) VBox {
			width: bind width
			height: bind height
			fillWidth: fillWidth
			padding: padding
			spacing: spacing
			nodeHPos: nodeHPos
			nodeVPos: nodeVPos
			hpos: hpos
			vpos: vpos
		} else HBox {
			width: bind width
			height: bind height
			fillHeight: fillHeight
			padding: padding
			spacing: spacing
			nodeHPos: nodeHPos
			nodeVPos: nodeVPos
			hpos: hpos
			vpos: vpos
		};
		box.content = buildContent();
	}
	
	public var padding:Insets on replace {
		if( vertical ) {
			(box as VBox).padding = padding;
		} else {
			(box as HBox).padding = padding;
		}
	}
	
	public var spacing = 0 on replace {
		if( vertical ) {
			(box as VBox).spacing = spacing;
		} else {
			(box as HBox).spacing = spacing;
		}
	}
	
	public var nodeHPos = HPos.LEFT on replace {
		if( vertical ) {
			(box as VBox).nodeHPos = nodeHPos;
		} else {
			(box as HBox).nodeHPos = nodeHPos;
		}
	}
	
	public var nodeVPos = VPos.TOP on replace {
		if( vertical ) {
			(box as VBox).nodeVPos = nodeVPos;
		} else {
			(box as HBox).nodeVPos = nodeVPos;
		}
	}
	
	public var hpos = HPos.LEFT on replace {
		if( vertical ) {
			(box as VBox).hpos = hpos;
		} else {
			(box as HBox).hpos = hpos;
		}
	}
	
	public var vpos = VPos.TOP on replace {
		if( vertical ) {
			(box as VBox).vpos = vpos;
		} else {
			(box as HBox).vpos = vpos;
		}
	}
	
	public var fillWidth = true on replace {
		if( vertical ) {
			(box as VBox).fillWidth = fillWidth;
		}
	}
	
	public var fillHeight = true on replace {
		if( not vertical ) {
			(box as HBox).fillHeight = fillHeight;
		}
	}
	
	public var content:Node[] on replace {
		box.content = buildContent();
	}
	
	public var onMoved: function( node:Node, fromIndex:Integer, toIndex:Integer ):Void;
	
	function buildContent():Node[] {
		for( child in content ) {
			var draggable:DraggableNode;
			var offset = 0.0;
			
			def frame:DraggableFrame = DraggableFrame {
				draggable: draggable = DraggableNode {
					revert: false
					contentNode: child
					containment: bind localToScene( layoutBounds )
					onDragging: function():Void {
						def index = Sequences.indexByIdentity( box.content, frame );
						var pos:Number;
						var nextPos:Number;
						var prevPos:Number;
						if( vertical ) {
							pos = draggable.translateY;
							prevPos = if( index > 0 ) -(spacing + box.content[index-1].layoutBounds.height / 2) else Integer.MIN_VALUE;
							nextPos = if( index < sizeof content-1 ) spacing + box.content[index+1].layoutBounds.height / 2 else Integer.MAX_VALUE;
						} else {
							pos = draggable.translateX;
							prevPos = if( index > 0 ) -(spacing + box.content[index-1].layoutBounds.width / 2) else Integer.MIN_VALUE;
							nextPos = if( index < sizeof content-1 ) spacing + box.content[index+1].layoutBounds.width / 2 else Integer.MAX_VALUE;
						}
						
						if( pos + offset < prevPos or pos + offset > nextPos ) {
							def moveIndex = if( pos + offset < prevPos ) index-1 else index+1;
							var delta = if(vertical) box.content[moveIndex].layoutY else box.content[moveIndex].layoutX;
							delete box.content[index];
							insert frame before box.content[moveIndex];
							delta -= if(vertical) box.content[moveIndex].layoutY else box.content[moveIndex].layoutX;
							offset -= delta;
						}
					}
					onRelease: function():Void {
						def index = Sequences.indexByIdentity( box.content, frame );
						def oldIndex = Sequences.indexByIdentity( content, child );
						var newContent = content;
						delete child from newContent;
						insert child before newContent[index];
						content = newContent;
						offset = 0.0;
						onMoved( child, oldIndex, index );
					}
				}
			}
		}
	}
	
	
	override function create():Node {
		box
	}
	
	override function getPrefHeight( width:Number ):Number {
		box.getPrefHeight( width )
	}
	
	override function getPrefWidth( height:Number ):Number {
		box.getPrefWidth( height )
	}
}