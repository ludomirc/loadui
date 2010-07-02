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
*TestCaseMenu.fx
*
*Created on jun 1, 2010, 13:25:45 em
*/

package com.eviware.loadui.fx.ui.menu;

import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.geometry.VPos;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.layout.LayoutInfo;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import com.eviware.loadui.fx.MainWindow;
import com.eviware.loadui.fx.AppState;
import com.eviware.loadui.fx.ui.dialogs.*;
import com.eviware.loadui.fx.dialogs.*;
import com.eviware.loadui.fx.ui.menu.button.*;
import com.eviware.loadui.fx.widgets.TrashHole;
import com.eviware.loadui.fx.FxUtils.*;
import com.eviware.loadui.fx.ui.popup.Menu;
import com.eviware.loadui.fx.ui.popup.ActionMenuItem;
import com.eviware.loadui.fx.ui.popup.SeparatorMenuItem;
import com.eviware.loadui.fx.ui.popup.PopupMenu;
import com.eviware.loadui.fx.ui.popup.SubMenuItem;
import com.eviware.loadui.fx.ui.resources.Paints;
import com.eviware.loadui.fx.ui.resources.MenuArrow;
import com.eviware.loadui.fx.widgets.MiniRunController;
import com.eviware.loadui.fx.widgets.RunController;
import com.eviware.loadui.fx.summary.SummaryReport;

import com.eviware.loadui.api.model.ModelItem;
import com.eviware.loadui.api.model.CanvasItem;
import com.eviware.loadui.api.model.ProjectItem;
import com.eviware.loadui.api.model.SceneItem;
import com.eviware.loadui.api.model.CanvasObjectItem;
import com.eviware.loadui.api.events.EventHandler;
import com.eviware.loadui.api.events.BaseEvent;

import java.util.EventObject;

import com.eviware.loadui.api.counter.CounterHolder;

public class TestCaseMenu extends HBox {
	def listener = new SummaryListener();

	public var testCase: SceneItem on replace oldtestCase = newTestCase {
		//workspaceLabel = project.getWorkspace().getLabel();
		projectLabel = testCase.getProject().getLabel();
		testCaseLabel = testCase.getLabel();
		summaryEnabled = false;
		
		if( oldtestCase != null )
			oldtestCase.removeEventListener( BaseEvent.class, listener );
		if( newTestCase != null )
			newTestCase.addEventListener( BaseEvent.class, listener );
	}
	
	var workspaceLabel:String = "Workspace";
	var projectLabel:String;
	var testCaseLabel:String;
	
	override var spacing = 3;
	override var nodeVPos = VPos.CENTER;
	
	public var tcMenuFill: Paint = Color.TRANSPARENT;
	public var tcMenuClosedTextFill: Paint = Color.web("#666666");
	public var tcMenuOpenedTextFill: Paint = Color.web("#4d4d4d");
	public var tcMenuClosedArrowFill: Paint = Color.web("#666666");
	public var tcMenuOpenedArrowFill: Paint = Color.web("#4D4D4D");
	public var tcMenuClosedFont: Font = Font{name:"Arial", size:10};
	public var tcMenuOpenedFont: Font = Font{name:"Arial", size:18};
	
	public var projectMenuClosedTextFill: Paint = Color.web("#666666");
	public var projectMenuClosedArrowFill: Paint = Color.web("#666666");
	public var projectMenuClosedFont: Font = Font{name:"Arial", size:10};
	public var workspaceMenuClosedTextFill: Paint = Color.web("#666666");
	public var workspaceMenuClosedArrowFill: Paint = Color.web("#666666");
	public var workspaceMenuClosedFont: Font = Font{name:"Arial", size:10};
	
	override var layoutInfo = LayoutInfo {
		hgrow: Priority.ALWAYS
		vgrow: Priority.NEVER
		hfill: true
		vfill: false
		height: 90
	}
	
	var popup:PopupMenu;
	var summaryEnabled = false;
	
	var testCaseLabelTruncated: Boolean = false on replace {
		if(not testCaseLabelTruncated){
			tcMenuClosedTextFill = Color.web("#666666");
			tcMenuOpenedTextFill = Color.web("#4d4d4d");
		}
		else{
			tcMenuClosedTextFill = LinearGradient {
				endY: 0
				stops: [
					Stop { offset: 0, color: Color.rgb( 0x66, 0x66, 0x66, 1.0 ) },
					Stop { offset: 0.8, color: Color.rgb( 0x66, 0x66, 0x66, 1.0 ) },
					Stop { offset: 0.9, color: Color.rgb( 0x66, 0x66, 0x66, 0.7 ) },
					Stop { offset: 1.0, color: Color.rgb( 0x66, 0x66, 0x66, 0.0 ) },
				]
			}
			tcMenuOpenedTextFill = LinearGradient {
				endY: 0
				stops: [
					Stop { offset: 0, color: Color.rgb( 0x4d, 0x4d, 0x4d, 1.0 ) },
					Stop { offset: 0.8, color: Color.rgb( 0x4d, 0x4d, 0x4d, 1.0 ) },
					Stop { offset: 0.9, color: Color.rgb( 0x66, 0x66, 0x66, 0.7 ) },
					Stop { offset: 1.0, color: Color.rgb( 0x4d, 0x4d, 0x4d, 0.0 ) },
				]
			};
		}
	};
	
	var projectLabelTruncated: Boolean = false on replace {
		if(not projectLabelTruncated){
			projectMenuClosedTextFill = Color.web("#666666");
		}
		else{
			projectMenuClosedTextFill = LinearGradient {
				endY: 0
				stops: [
					Stop { offset: 0, color: Color.rgb( 0x66, 0x66, 0x66, 1.0 ) },
					Stop { offset: 0.75, color: Color.rgb( 0x66, 0x66, 0x66, 1.0 ) },
					Stop { offset: 0.85, color: Color.rgb( 0x66, 0x66, 0x66, 0.75 ) },
					Stop { offset: 1.0, color: Color.rgb( 0x66, 0x66, 0x66, 0.0 ) },
				]
			}
		}
	}
	
	var truncTestCaseLabel: String;
	var truncProjectLabel: String;
	var t: String = bind testCaseLabel on replace {
		var tcWidth: Number = 0;
		testCaseLabelTruncated = false;
		var tmp: String = "";
		for(i in [0..testCaseLabel.length()-1]){
			tmp = "{tmp}{testCaseLabel.substring(i, i + 1)}";
			var tmpText: Text = Text {
				content: tmp
			}
			def tcPrevWidth: Number = tcWidth;
			tcWidth = tmpText.boundsInLocal.width * 18/12;
			if(tcWidth > 120){
				truncTestCaseLabel = tmp.substring(0, tmp.length() - 1);
				testCaseLabelTruncated = true;
				tcWidth = tcPrevWidth;
				break;
			}
		}
		if(not testCaseLabelTruncated){
			truncTestCaseLabel = testCaseLabel;
		}
		
		tmp = "";
		for(i in [0..projectLabel.length()-1]){
			tmp = "{tmp}{projectLabel.substring(i, i + 1)}";
			var tmpText: Text = Text {
				content: tmp
			}
			if(tmpText.boundsInLocal.width > 10 + tcWidth * 12 / 10){
				truncProjectLabel = tmp.substring(0, tmp.length() - 1);
				projectLabelTruncated = true;
				break;
			}
		}
		if(not projectLabelTruncated){
			truncProjectLabel = projectLabel;
		}
	}
	
	var menu:Menu;
	
	init {
		var menuContent:Node;
		
		content = [
			ImageView {
				image: Image {
					url: "{__ROOT__}images/png/toolbar-background.png"
					width: width
				}
				fitWidth: bind width
				managed: false
			}, Rectangle {
				width: 78
				height: 93
				fill: Color.rgb( 0, 0, 0, 0.1 )
				managed: false
			}, Rectangle {
				width: bind width
				height: 20
				fill: Color.TRANSPARENT
				managed: false
				onMousePressed: function ( e:MouseEvent ) { 
					// This is disabled until issue LUCO-620 is fixed.
					//AppState.instance.displayWorkspace(); 
				}
			}, Rectangle {
				width: bind width
				height: 20
				layoutY: 20
				fill: Color.TRANSPARENT
				managed: false
				onMousePressed: function ( e:MouseEvent ) {
				   // This is disabled until issue LUCO-620 is fixed.
					//AppState.instance.setActiveCanvas( testCase.getProject() );
				}
			}, Label {
				layoutInfo: LayoutInfo {
					width: 95
				}
			}, VBox {
				content: [
					Label {
						text: bind workspaceLabel
						textFill: bind workspaceMenuClosedTextFill
						font: bind workspaceMenuClosedFont
						layoutInfo: LayoutInfo {
							height: 20
							margin: Insets { left: 3 }
						}
					}, HBox {
						nodeVPos: VPos.CENTER;
						content: [
							Label {
								text: bind truncProjectLabel
								textFill: bind projectMenuClosedTextFill
								font: bind projectMenuClosedFont
								layoutInfo: LayoutInfo {
									height: 20
									margin: Insets { left: 3 }
									width: bind menu.layoutBounds.width
								}
							}, MiniRunController {
								canvas: bind testCase.getProject()
							}
						]
					}, HBox {
						layoutInfo: LayoutInfo {
							hgrow: Priority.ALWAYS
							vgrow: Priority.NEVER
							hfill: true
							vfill: false
							height: 50
						}
						spacing: 3
						nodeVPos: VPos.CENTER
						content: [
							menu = Menu {
								contentNode: Group {
									content: [
										Rectangle {
											width: bind menuContent.boundsInLocal.width + 6
											height: bind menuContent.boundsInLocal.height + 6
											fill: bind tcMenuFill
										}, menuContent = HBox {
											layoutX: 3
											layoutY: 3
											nodeVPos: VPos.CENTER
											spacing: 5 
											content: [
												Label {
													textFill: bind if( popup.isOpen ) tcMenuOpenedTextFill else tcMenuClosedTextFill
													text: bind truncTestCaseLabel                      
													font: bind tcMenuOpenedFont
												} 
												MenuArrow { 
													fill: bind if( popup.isOpen ) tcMenuOpenedArrowFill else tcMenuClosedArrowFill
													rotate: 90
												}
											]
										}
									]
								}
								menu: popup = PopupMenu {
									items: [
										ActionMenuItem {
											text: "Rename"
											action: function() { 
												RenameModelItemDialog { modelItem: MainWindow.instance.testcaseCanvas.canvasItem as SceneItem }
											}
										}
										ActionMenuItem {
											text: "Clone"
											action: function() {
												def copy = testCase.getCanvas().duplicate( testCase ) as SceneItem;
												def layoutX = Integer.parseInt( testCase.getAttribute( "gui.layoutX", "0" ) ) + 50;
												def layoutY = Integer.parseInt( testCase.getAttribute( "gui.layoutY", "0" ) ) + 50;
												copy.setAttribute( "gui.layoutX", "{ layoutX as Integer }" );
												copy.setAttribute( "gui.layoutY", "{ layoutY as Integer }" );
												AppState.instance.setActiveCanvas( copy );
											}
										}
										ActionMenuItem {
											text: "Delete"
											action: function() { 
												DeleteModelItemDialog { 
													modelItem: MainWindow.instance.testcaseCanvas.canvasItem as SceneItem
													onOk: function(): Void {
														AppState.instance.setActiveCanvas( testCase.getProject() );
													} 
												} 
											}
										}
										SeparatorMenuItem{}
										ActionMenuItem {
											text: "Settings"
											action: function() { 
												new SettingsDialog().show(testCase); 
											}
										}
										SeparatorMenuItem{}
										ActionMenuItem {
											text: "Close"
											action: function() {
												AppState.instance.setActiveCanvas( testCase.getProject() );
											}
										}
									]
								}
							}, RunController {
								testcase: true
								canvas: bind testCase
							}, Label {
								layoutInfo: LayoutInfo {
									hgrow: Priority.ALWAYS
									hfill: true 
								}
							}, SeparatorButton {
								height: bind height;
							}, TrashHole {
							}, SeparatorButton {
								height: bind height;
							}, MenubarButton {
								shape: "M0,0 L0,12 10,12, 10,0 0,0 M4,13 L4,16 14,16 14,4 11,4 11,13 4,13"
								tooltip: Tooltip { text: ##[WRENCH]"Summary Report" }
								action: function() {
									if( testCase.getSummary() != null ) {
										println("Viewing TestCase summary");
										SummaryReport{ select: testCase.getLabel(), summary: testCase.getSummary() }
									} else if( testCase.getProject().getSummary() != null ) {
										println("Viewing Project summary");
										SummaryReport{ select: testCase.getProject().getLabel(), summary: testCase.getProject().getSummary() }
									} else {
										println("No summary available");
									}
								}
								disable: bind not summaryEnabled
							}, MenubarButton {
								shape: "M14.00,12.06 L7.50,5.59 C7.74,5.08 7.88,4.53 7.88,3.93 C7.88,1.76 6.12,0.00 3.94,0.00 C3.36,0.00 2.80,0.14 2.31,0.36 L4.83,2.88 L2.89,4.82 L0.36,2.30 C0.13,2.80 -0.00,3.35 -0.00,3.93 C-0.00,6.10 1.76,7.86 3.94,7.86 C4.52,7.86 5.06,7.73 5.55,7.51 L12.06,14.00 Z"
								tooltip: Tooltip { text: ##[SETTINGS]"Settings" }
								action: function():Void { new SettingsDialog().show(testCase) }
							}, MenubarButton {
								shape: "M2.46,10.21 C2.46,9.69 2.49,9.23 2.54,8.83 C2.59,8.43 2.69,8.07 2.82,7.75 C2.95,7.43 3.12,7.15 3.34,6.89 C3.55,6.63 3.82,6.39 4.15,6.15 C4.44,5.93 4.70,5.73 4.92,5.55 C5.14,5.36 5.32,5.18 5.47,4.99 C5.62,4.81 5.73,4.62 5.80,4.43 C5.87,4.25 5.91,4.03 5.91,3.80 C5.91,3.57 5.86,3.37 5.77,3.18 C5.67,2.99 5.54,2.82 5.36,2.68 C5.19,2.55 4.98,2.44 4.73,2.36 C4.48,2.29 4.21,2.25 3.90,2.25 C3.57,2.25 3.26,2.28 2.96,2.32 C2.67,2.37 2.39,2.44 2.12,2.52 C1.84,2.60 1.58,2.69 1.33,2.80 C1.08,2.90 0.83,3.01 0.58,3.13 L-0.00,1.18 C0.22,1.05 0.49,0.91 0.79,0.77 C1.10,0.63 1.45,0.50 1.83,0.39 C2.22,0.28 2.64,0.19 3.09,0.11 C3.55,0.04 4.04,0.00 4.56,0.00 C5.21,0.00 5.80,0.08 6.33,0.25 C6.86,0.42 7.32,0.65 7.70,0.96 C8.08,1.27 8.38,1.64 8.59,2.08 C8.80,2.52 8.90,3.02 8.90,3.57 C8.90,4.07 8.82,4.52 8.66,4.91 C8.50,5.30 8.29,5.66 8.03,5.99 C7.77,6.31 7.49,6.61 7.17,6.88 C6.85,7.15 6.53,7.41 6.21,7.67 C6.04,7.83 5.90,7.98 5.77,8.13 C5.64,8.28 5.53,8.46 5.45,8.65 C5.37,8.84 5.30,9.06 5.26,9.31 C5.22,9.56 5.20,9.86 5.20,10.21 Z M2.48,11.85 L5.25,11.85 L5.25,14.00 L2.48,14.00 Z"
								tooltip: Tooltip { text: ##[HELP]"Help Page" }
								action: function():Void { openURL("http://www.loadui.org/interface/project-view.html") }
							}, MenubarButton {
								shape: "M14.00,2.00 L12.00,0.00 7.00,5.00 2.00,0.00 0.00,2.00 5.00,7.00 0.00,12.00 2.00,14.00 7.00,9.00 12.00,14.00 14.00,12.00 9.00,7.00 Z"
								tooltip: Tooltip { text: ##[CLOSE_PROJECT]"Close Project" }
								action: function():Void { AppState.instance.setActiveCanvas( testCase.getProject() ) }
				         }, Label {
								layoutInfo: LayoutInfo {
									width: 10
								}
							}
						]
					}
				]
			}
		];
	}
}

class SummaryListener extends EventHandler {
	override function handleEvent( e:EventObject ) { 
    	var event = e as BaseEvent;
    	if(event.getKey().equals(CanvasItem.START_ACTION)){
    		runInFxThread( function(): Void { summaryEnabled = false } );
    	} 
    	else if(event.getKey().equals(CanvasItem.SUMMARY)){
    		runInFxThread( function(): Void { summaryEnabled = testCase.getSummary() != null } );
    	}
    	else if(event.getKey().equals(ModelItem.LABEL)){
			runInFxThread( function(): Void {
		 		if(event.getSource() instanceof SceneItem){
		 			testCaseLabel = testCase.getLabel();
		 		}
			});
		}
		else if (event.getKey().equals(CounterHolder.COUNTER_RESET_ACTION)){
			runInFxThread( function(): Void { summaryEnabled = false } );
		}
	}
}