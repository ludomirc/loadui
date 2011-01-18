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
package com.eviware.loadui.impl.model;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.eviware.loadui.LoadUI;
import com.eviware.loadui.api.addressable.AddressableRegistry;
import com.eviware.loadui.api.component.categories.OnOffCategory;
import com.eviware.loadui.api.counter.CounterHolder;
import com.eviware.loadui.api.counter.CounterSynchronizer;
import com.eviware.loadui.api.events.ActionEvent;
import com.eviware.loadui.api.events.BaseEvent;
import com.eviware.loadui.api.events.CollectionEvent;
import com.eviware.loadui.api.events.EventFirer;
import com.eviware.loadui.api.events.EventHandler;
import com.eviware.loadui.api.events.PropertyEvent;
import com.eviware.loadui.api.events.RemoteActionEvent;
import com.eviware.loadui.api.events.TerminalEvent;
import com.eviware.loadui.api.events.TerminalMessageEvent;
import com.eviware.loadui.api.messaging.MessageEndpoint;
import com.eviware.loadui.api.messaging.SceneCommunication;
import com.eviware.loadui.api.model.AgentItem;
import com.eviware.loadui.api.model.CanvasItem;
import com.eviware.loadui.api.model.ComponentItem;
import com.eviware.loadui.api.model.ProjectItem;
import com.eviware.loadui.api.model.SceneItem;
import com.eviware.loadui.api.model.WorkspaceItem;
import com.eviware.loadui.api.property.Property;
import com.eviware.loadui.api.summary.MutableSummary;
import com.eviware.loadui.api.terminal.Connection;
import com.eviware.loadui.api.terminal.InputTerminal;
import com.eviware.loadui.api.terminal.OutputTerminal;
import com.eviware.loadui.api.terminal.Terminal;
import com.eviware.loadui.api.terminal.TerminalMessage;
import com.eviware.loadui.config.SceneItemConfig;
import com.eviware.loadui.impl.counter.CounterSupport;
import com.eviware.loadui.impl.counter.RemoteAggregatedCounterSupport;
import com.eviware.loadui.impl.summary.MutableChapterImpl;
import com.eviware.loadui.impl.summary.sections.TestCaseDataSection;
import com.eviware.loadui.impl.summary.sections.TestCaseDataSummarySection;
import com.eviware.loadui.impl.summary.sections.TestCaseExecutionDataSection;
import com.eviware.loadui.impl.summary.sections.TestCaseExecutionMetricsSection;
import com.eviware.loadui.impl.summary.sections.TestCaseExecutionNotablesSection;
import com.eviware.loadui.impl.terminal.ConnectionImpl;
import com.eviware.loadui.impl.terminal.TerminalHolderSupport;
import com.eviware.loadui.util.BeanInjector;

public class SceneItemImpl extends CanvasItemImpl<SceneItemConfig> implements SceneItem
{
	private final static String INCREMENT_VERSION = "incrementVersion";

	private final ProjectItem project;
	private final Set<OutputTerminal> exports = new HashSet<OutputTerminal>();
	private final ProjectListener projectListener;
	private final WorkspaceListener workspaceListener;
	private final TerminalHolderSupport terminalHolderSupport;
	private final InputTerminal stateTerminal;
	private final Map<AgentItem, Map<Object, Object>> remoteStatistics = new HashMap<AgentItem, Map<Object, Object>>();
	private long version;
	private boolean propagate = true;

	private MessageEndpoint messageEndpoint = null;

	private final Property<Boolean> followProject;

	// private final StatisticHolderSupport statisticHolderSupport;

	public SceneItemImpl( ProjectItem project, SceneItemConfig config )
	{
		super( config,
				LoadUI.CONTROLLER.equals( System.getProperty( LoadUI.INSTANCE ) ) ? new RemoteAggregatedCounterSupport(
						BeanInjector.getBean( CounterSynchronizer.class ) ) : new CounterSupport() );
		this.project = project;
		version = getConfig().getVersion().longValue();

		followProject = createProperty( FOLLOW_PROJECT_PROPERTY, Boolean.class, true );

		AddressableRegistry addressableRegistry = BeanInjector.getBean( AddressableRegistry.class );

		terminalHolderSupport = new TerminalHolderSupport( this );

		stateTerminal = terminalHolderSupport.createInput( OnOffCategory.STATE_TERMINAL, "Controls TestCase execution." );

		for( String exportId : getConfig().getExportedTerminalArray() )
			exports.add( ( OutputTerminal )addressableRegistry.lookup( exportId ) );

		workspaceListener = LoadUI.CONTROLLER.equals( System.getProperty( LoadUI.INSTANCE ) ) ? new WorkspaceListener()
				: null;

		projectListener = LoadUI.CONTROLLER.equals( System.getProperty( LoadUI.INSTANCE ) ) ? new ProjectListener()
				: null;

		// statisticHolderSupport = new StatisticHolderSupport( this );

	}

	@Override
	public void init()
	{
		super.init();
		addEventListener( BaseEvent.class, new SelfListener() );
		if( project != null )
		{
			project.addEventListener( ActionEvent.class, projectListener );
			project.getWorkspace().addEventListener( PropertyEvent.class, workspaceListener );
			propagate = project.getWorkspace().isLocalMode();
		}

		// statisticHolderSupport.init();
		//
		// MutableStatisticVariable rpsVariable =
		// statisticHolderSupport.addStatisticVariable( "RequestPerSecond" );
		// statisticHolderSupport.addStatisticsWriter( "PSWritter", rpsVariable );
	}

	@Override
	public ProjectItem getProject()
	{
		return project;
	}

	@Override
	public void exportTerminal( OutputTerminal terminal )
	{
		if( exports.add( terminal ) )
		{
			getConfig().addExportedTerminal( terminal.getId() );
			fireCollectionEvent( EXPORTS, CollectionEvent.Event.ADDED, terminal );
		}
	}

	@Override
	public void unexportTerminal( OutputTerminal terminal )
	{
		if( exports.remove( terminal ) )
		{
			for( int i = 0; i < getConfig().sizeOfExportedTerminalArray(); i++ )
			{
				if( terminal.getId().equals( getConfig().getExportedTerminalArray( i ) ) )
				{
					getConfig().removeExportedTerminal( i );
					break;
				}
			}
			fireCollectionEvent( EXPORTS, CollectionEvent.Event.REMOVED, terminal );
		}
	}

	@Override
	public Collection<OutputTerminal> getExportedTerminals()
	{
		return Collections.unmodifiableSet( exports );
	}

	@Override
	protected Connection createConnection( OutputTerminal output, InputTerminal input )
	{
		return new ConnectionImpl( getConfig().addNewConnection(), output, input );
	}

	@Override
	public long getVersion()
	{
		return version;
	}

	private void incrVersion()
	{
		getConfig().setVersion( BigInteger.valueOf( ++version ) );
	}

	@Override
	public void release()
	{
		if( project != null )
		{
			project.removeEventListener( ActionEvent.class, projectListener );
			project.getWorkspace().removeEventListener( PropertyEvent.class, workspaceListener );
		}
		terminalHolderSupport.release();

		super.release();
	}

	@Override
	public void delete()
	{
		for( Terminal terminal : getTerminals() )
			for( Connection connection : terminal.getConnections().toArray(
					new Connection[terminal.getConnections().size()] ) )
				connection.disconnect();

		super.delete();
	}

	@Override
	public boolean isFollowProject()
	{
		return followProject.getValue();
	}

	@Override
	public String getColor()
	{
		return "#4B89E0";
	}

	@Override
	public CanvasItem getCanvas()
	{
		return getProject();
	}

	@Override
	public Collection<Terminal> getTerminals()
	{
		return terminalHolderSupport.getTerminals();
	}

	@Override
	public Terminal getTerminalByLabel( String label )
	{
		return terminalHolderSupport.getTerminalByLabel( label );
	}

	@Override
	public void handleTerminalEvent( InputTerminal input, TerminalEvent event )
	{
		if( event instanceof TerminalMessageEvent && input == stateTerminal )
		{
			TerminalMessage message = ( ( TerminalMessageEvent )event ).getMessage();
			if( message.containsKey( OnOffCategory.ENABLED_MESSAGE_PARAM ) )
			{
				Object enabled = message.get( OnOffCategory.ENABLED_MESSAGE_PARAM );
				if( enabled instanceof Boolean )
					triggerAction( ( Boolean )enabled ? START_ACTION : STOP_ACTION );
			}
		}
	}

	@Override
	public void setFollowProject( boolean followProject )
	{
		this.followProject.setValue( followProject );
	}

	@Override
	public InputTerminal getStateTerminal()
	{
		return stateTerminal;
	}

	@Override
	public void broadcastMessage( String channel, Object data )
	{
		if( LoadUI.CONTROLLER.equals( System.getProperty( LoadUI.INSTANCE ) ) )
			getProject().broadcastMessage( this, channel, data );
		else if( messageEndpoint != null )
			messageEndpoint.sendMessage( channel, data );
	}

	@Override
	protected void onComplete( EventFirer source )
	{
		if( LoadUI.AGENT.equals( System.getProperty( LoadUI.INSTANCE ) ) )
		{
			// if on agent, application is running in distributed mode, so send
			// data
			// to the controller

			Map<String, Object> data = new HashMap<String, Object>();
			data.put( AgentItem.SCENE_ID, getId() );

			// send scene start and and time
			SimpleDateFormat sdf = ( SimpleDateFormat )SimpleDateFormat.getDateTimeInstance();
			sdf.setLenient( false );
			sdf.applyPattern( "yyyyMMddHHmmssSSS" );
			data.put( AgentItem.SCENE_START_TIME, sdf.format( startTime ) );
			data.put( AgentItem.SCENE_END_TIME, sdf.format( endTime ) );

			for( ComponentItem component : getComponents() )
				data.put( component.getId(), component.getBehavior().collectStatisticsData() );
			messageEndpoint.sendMessage( AgentItem.AGENT_CHANNEL, data );
			log.debug( "Sending statistics data from {}", this );

			// this is a hack, since sometimes time variable is not reset when
			// running multiple tests on agent one after another. So the result
			// time is actually the execution time is accumulated time from
			// previous
			// tests.
			setTime( 0 );
		}
		else if( project.getWorkspace().isLocalMode() )
		{
			if( source.equals( project ) )
			{
				// if source is project set completed to true to inform it (over
				// scene waiter) that this test case is finished. for distributed
				// mode this will be set to true when test case data is received on
				// controller.
				setCompleted( true );
			}
			else if( source.equals( this ) )
			{
				// if source is test case, just generate summary for it
				doGenerateSummary();
			}
		}
		else if( source.equals( this ) && !project.getWorkspace().isLocalMode() )
		{
			// distributed mode, and source is test case...add ON_COMPLETE_DONE
			// event listener to this test case to listen when it is completed
			addEventListener( BaseEvent.class, new EventHandler<BaseEvent>()
			{
				@Override
				public void handleEvent( BaseEvent event )
				{
					if( event.getKey().equals( ON_COMPLETE_DONE ) )
					{
						removeEventListener( BaseEvent.class, this );
						doGenerateSummary();
					}
				}
			} );
		}
	}

	@Override
	public void cancelComponents()
	{
		if( LoadUI.AGENT.equals( System.getProperty( LoadUI.INSTANCE ) ) )
		{
			// on agent. cancel components of this test case on current agent
			super.cancelComponents();
		}
		else
		{
			if( project.getWorkspace().isLocalMode() )
			{
				// local mode, simply cancel components
				super.cancelComponents();
			}
			else
			{
				if( project.getAgentsAssignedTo( this ).size() > 0 )
				{
					// test cases is deployed to one or more agents so send message
					// to all agents to cancel
					broadcastMessage( SceneCommunication.CHANNEL,
							Arrays.asList( getId(), Long.toString( getVersion() ), SceneCommunication.CANCEL_COMPONENTS ) );
				}
				else
				{
					// test case is not deployed to any agent, so cancel it locally
					// (in case non deployed test cases are executed locally. TODO
					// Are they?)
					super.cancelComponents();
				}
			}
		}
	}

	public boolean statisticsReady()
	{
		for( AgentItem agent : getActiveAgents() )
			if( !remoteStatistics.containsKey( agent ) )
				return false;
		return true;
	}

	public int getRemoteStatisticsCount()
	{
		return remoteStatistics.size();
	}

	public void handleStatisticsData( AgentItem source, Map<Object, Object> data )
	{
		remoteStatistics.put( source, data );
		log.debug( "{} got statistics data from {}", this, source );
		if( !statisticsReady() )
			return;

		for( ComponentItem component : getComponents() )
		{
			String cId = component.getId();
			Map<AgentItem, Object> cData = new HashMap<AgentItem, Object>();
			for( Entry<AgentItem, Map<Object, Object>> e : remoteStatistics.entrySet() )
				cData.put( e.getKey(), e.getValue().get( cId ) );
			component.getBehavior().handleStatisticsData( cData );
		}

		setCompleted( true );
	}

	@Override
	public void generateSummary( MutableSummary summary )
	{
		MutableChapterImpl chap = ( MutableChapterImpl )summary.addChapter( getLabel() );
		chap.addSection( new TestCaseDataSummarySection( this ) );
		chap.addSection( new TestCaseExecutionDataSection( this ) );
		chap.addSection( new TestCaseExecutionMetricsSection( this ) );
		chap.addSection( new TestCaseExecutionNotablesSection( this ) );
		chap.addSection( new TestCaseDataSection( this ) );
		chap.setDescription( getDescription() );
	}

	@Override
	protected void reset()
	{
		super.reset();
		remoteStatistics.clear();
	}

	public void setMessageEndpoint( MessageEndpoint messageEndpoint )
	{
		this.messageEndpoint = messageEndpoint;
	}

	@Override
	protected void setRunning( boolean running )
	{
		super.setRunning( running );

		fireBaseEvent( ACTIVITY );
	}

	@Override
	public boolean isActive()
	{
		return isRunning();
	}

	@Override
	public String getHelpUrl()
	{
		return "http://www.loadui.org/Working-with-loadUI/agents-and-testcases.html";
	}

	private Collection<AgentItem> getActiveAgents()
	{
		ArrayList<AgentItem> agents = new ArrayList<AgentItem>();
		for( AgentItem agent : getProject().getAgentsAssignedTo( this ) )
			if( agent.isReady() )
				agents.add( agent );
		return agents;
	}

	private class SelfListener implements EventHandler<BaseEvent>
	{
		@Override
		public void handleEvent( BaseEvent event )
		{
			// The reason we use a separate event to increment the version is to
			// ensure that all listeners of the other events get executed prior to
			// incrementing the version.
			if( event instanceof CollectionEvent )
				fireBaseEvent( INCREMENT_VERSION );
			else if( event instanceof ActionEvent && event.getSource() == SceneItemImpl.this && !propagate )
				fireEvent( new RemoteActionEvent( SceneItemImpl.this, ( ActionEvent )event ) );
			else if( LABEL.equals( event.getKey() ) )
				fireBaseEvent( INCREMENT_VERSION );
			else if( EXPORTS.equals( event.getKey() ) )
				fireBaseEvent( INCREMENT_VERSION );
			else if( INCREMENT_VERSION.equals( event.getKey() ) )
				incrVersion();
		}
	}

	private class ProjectListener implements EventHandler<ActionEvent>
	{
		@Override
		public void handleEvent( ActionEvent event )
		{
			if( !isFollowProject() && ( START_ACTION.equals( event.getKey() ) || STOP_ACTION.equals( event.getKey() ) ) )
				return;

			if( propagate && !CounterHolder.COUNTER_RESET_ACTION.equals( event.getKey() ) )
				fireEvent( event );
			else
			{
				fireEvent( new RemoteActionEvent( SceneItemImpl.this, event ) );
				// if( COUNTER_RESET_ACTION.equals( event.getKey() ) )
				fireEvent( event );
			}
		}
	}

	private class WorkspaceListener implements EventHandler<PropertyEvent>
	{
		@Override
		public void handleEvent( PropertyEvent event )
		{
			if( WorkspaceItem.LOCAL_MODE_PROPERTY.equals( event.getProperty().getKey() ) )
			{
				propagate = ( Boolean )event.getProperty().getValue();
				// if( isRunning() )
				// {
				// ActionEvent startAction = new ActionEvent( SceneItemImpl.this,
				// START_ACTION );
				// if( !propagate )
				// fireEvent( new RemoteActionEvent( SceneItemImpl.this, startAction
				// ) );
				// else
				// fireEvent( startAction );
				// }
			}
		}
	}

}