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
package com.eviware.loadui.impl.model.canvas;

import com.eviware.loadui.LoadUI;
import com.eviware.loadui.api.component.ComponentCreationException;
import com.eviware.loadui.api.component.ComponentDescriptor;
import com.eviware.loadui.api.component.ComponentRegistry;
import com.eviware.loadui.api.counter.Counter;
import com.eviware.loadui.api.counter.CounterHolder;
import com.eviware.loadui.api.events.*;
import com.eviware.loadui.api.execution.Phase;
import com.eviware.loadui.api.execution.TestExecution;
import com.eviware.loadui.api.execution.TestExecutionTask;
import com.eviware.loadui.api.execution.TestRunner;
import com.eviware.loadui.api.model.CanvasItem;
import com.eviware.loadui.api.model.CanvasObjectItem;
import com.eviware.loadui.api.model.ComponentItem;
import com.eviware.loadui.api.model.ProjectItem;
import com.eviware.loadui.api.property.Property;
import com.eviware.loadui.api.statistics.Statistic;
import com.eviware.loadui.api.statistics.StatisticVariable;
import com.eviware.loadui.api.summary.Summary;
import com.eviware.loadui.api.terminal.Connection;
import com.eviware.loadui.api.terminal.InputTerminal;
import com.eviware.loadui.api.terminal.OutputTerminal;
import com.eviware.loadui.config.CanvasItemConfig;
import com.eviware.loadui.config.ComponentItemConfig;
import com.eviware.loadui.config.ConnectionConfig;
import com.eviware.loadui.impl.counter.AggregatedCounterSupport;
import com.eviware.loadui.impl.counter.CounterSupport;
import com.eviware.loadui.impl.model.ComponentItemImpl;
import com.eviware.loadui.impl.model.ModelItemImpl;
import com.eviware.loadui.impl.statistics.CounterStatisticsWriter;
import com.eviware.loadui.impl.statistics.StatisticHolderSupport;
import com.eviware.loadui.impl.summary.SummaryCreator;
import com.eviware.loadui.impl.terminal.ConnectionImpl;
import com.eviware.loadui.util.BeanInjector;
import com.eviware.loadui.util.ReleasableUtils;
import com.eviware.loadui.util.collections.CollectionEventSupport;
import com.eviware.loadui.util.events.EventFuture;
import com.eviware.loadui.util.statistics.CounterStatisticSupport;
import com.eviware.loadui.util.statistics.StatisticDescriptorImpl;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public abstract class CanvasItemImpl<Config extends CanvasItemConfig> extends ModelItemImpl<Config> implements
		CanvasItem
{
	private static final Logger log = LoggerFactory.getLogger( CanvasItemImpl.class );

	private static final String LIMITS_ATTRIBUTE = "limits";
	private final Object datesLock = new Object();

	protected final CounterSupport counterSupport;
	private final CollectionEventSupport<ComponentItem, Void> componentList;
	protected final CollectionEventSupport<Connection, Void> connectionList;
	private final ComponentListener componentListener = new ComponentListener();
	private final ConnectionListener connectionListener = new ConnectionListener();
	private final CanvasTestExecutionTask executionTask = new CanvasTestExecutionTask();
	private final ComponentRegistry componentRegistry;
	protected final ScheduledExecutorService scheduler;
	protected final Counter timerCounter = new TimerCounter();
	protected final TestRunner testRunner = BeanInjector.getBean( TestRunner.class );
	private ScheduledFuture<?> timerFuture;
	private ScheduledFuture<?> timeLimitFuture;
	private long time = 0;
	protected Summary summary = null;
	@GuardedBy( value = "datesLock" )
	private Date startTime;
	@GuardedBy( value = "datesLock" )
	private Date endTime;
	private boolean hasStarted = false;
	private String lastSavedHash;
	private boolean loadingErrors = false;

	protected final Map<String, Long> limits = new HashMap<>();

	private boolean running = false;
	private boolean completed = false;

	private final Property<Boolean> abortOnFinish;

	// here keep all not loaded components and connections, remove them at the
	// end of init
	private final ArrayList<ComponentItemConfig> badComponents = new ArrayList<>();
	private final ArrayList<ConnectionConfig> badConnections = new ArrayList<>();

	private final StatisticHolderSupport statisticHolderSupport;
	private final CounterStatisticSupport counterStatisticSupport;

	public CanvasItemImpl( Config config, CounterSupport counterSupport )
	{
		super( config );

		lastSavedHash = DigestUtils.md5Hex( config.xmlText() );

		this.counterSupport = counterSupport;

		scheduler = BeanInjector.getBean( ScheduledExecutorService.class );
		componentRegistry = BeanInjector.getBean( ComponentRegistry.class );

		componentList = CollectionEventSupport.of( this, COMPONENTS );
		connectionList = CollectionEventSupport.of( this, CONNECTIONS );

		statisticHolderSupport = new StatisticHolderSupport( this );
		counterStatisticSupport = new CounterStatisticSupport( this );

		StatisticVariable.Mutable requestVariable = statisticHolderSupport.addStatisticVariable( REQUEST_VARIABLE );
		statisticHolderSupport.addStatisticsWriter( CounterStatisticsWriter.TYPE, requestVariable );
		counterStatisticSupport.addCounterVariable( REQUEST_COUNTER, requestVariable );

		StatisticVariable.Mutable failuresVariable = statisticHolderSupport.addStatisticVariable( FAILURE_VARIABLE );
		statisticHolderSupport.addStatisticsWriter( CounterStatisticsWriter.TYPE, failuresVariable );
		counterStatisticSupport.addCounterVariable( FAILURE_COUNTER, failuresVariable );

		StatisticVariable.Mutable assertionFailuresVariable = statisticHolderSupport
				.addStatisticVariable( ASSERTION_FAILURE_VARIABLE );
		statisticHolderSupport.addStatisticsWriter( CounterStatisticsWriter.TYPE, assertionFailuresVariable );
		counterStatisticSupport.addCounterVariable( ASSERTION_FAILURE_COUNTER, assertionFailuresVariable );

		StatisticVariable.Mutable requestFailuresVariable = statisticHolderSupport
				.addStatisticVariable( REQUEST_FAILURE_VARIABLE );
		statisticHolderSupport.addStatisticsWriter( CounterStatisticsWriter.TYPE, requestFailuresVariable );
		counterStatisticSupport.addCounterVariable( REQUEST_FAILURE_COUNTER, requestFailuresVariable );

		abortOnFinish = createProperty( ABORT_ON_FINISH_PROPERTY, Boolean.class, false );
	}

	@Override
	protected void init()
	{
		super.init();

		counterSupport.init( this );

		loadingErrors = false;

		String[] limitStrings = getAttribute( LIMITS_ATTRIBUTE, "" ).split( ";" );
		for( String limit : limitStrings )
		{
			String[] parts = limit.split( "=", 2 );
			try
			{
				if( parts.length == 2 )
					setLimit( parts[0], Long.parseLong( parts[1] ) );
			}
			catch( NumberFormatException e )
			{
				// Ignore
			}
		}

		createComponents();
		createConnections();
		removeBadComponents();
		removeBadConnections();

		addEventListener( BaseEvent.class, new ActionListener() );

		testRunner.registerTask( executionTask, Phase.START, Phase.PRE_STOP, Phase.STOP );

		// timer.scheduleAtFixedRate( timerTask, 1000, 1000 );

		statisticHolderSupport.init();
		counterStatisticSupport.init();
	}

	private void createComponents()
	{
		for( ComponentItemConfig componentConfig : getConfig().getComponentList() )
		{
			try
			{
				loadComponent( componentConfig );
			}
			catch( ComponentCreationException e )
			{
				log.error( "Unable to load component: ", e );
				loadingErrors = true;
			}
		}
	}

	private void createConnections()
	{
		for( ConnectionConfig connectionConfig : getConfig().getConnectionList() )
		{
			try
			{
				Connection connection = new ConnectionImpl( connectionConfig );
				connection.getOutputTerminal().addEventListener( TerminalConnectionEvent.class, connectionListener );
				connectionList.addItem( connection );
			}
			catch( Exception e )
			{
				badConnections.add( connectionConfig );
				log.error( "Unable to create connection between terminals " + connectionConfig.getInputTerminalId()
						+ " and " + connectionConfig.getOutputTerminalId(), e );
			}
		}
	}

	private void removeBadConnections()
	{
		for( ConnectionConfig badConnection : badConnections )
		{
			int cnt = 0;
			boolean found = false;
			for(; cnt < getConfig().getConnectionList().size(); cnt++ )
				if( getConfig().getConnectionArray( cnt ).equals( badConnection ) )
				{
					found = true;
					break;
				}
			if( found )
				getConfig().removeConnection( cnt );
		}
	}

	private void removeBadComponents()
	{
		for( ComponentItemConfig badComponent : badComponents )
		{
			int cnt = 0;
			boolean found = false;
			for(; cnt < getConfig().getComponentList().size(); cnt++ )
				if( getConfig().getComponentArray( cnt ).equals( badComponent ) )
				{
					found = true;
					break;
				}
			if( found )
				getConfig().removeComponent( cnt );
		}
	}

	@Override
	public Counter getCounter( String counterName )
	{
		if( TIMER_COUNTER.equals( counterName ) )
			return timerCounter;

		return counterSupport.getCounter( counterName );
	}

	@Override
	public Collection<String> getCounterNames()
	{
		return counterSupport.getCounterNames();
	}

	@Nonnull
	@Override
	public ComponentItem createComponent( @Nonnull String label, @Nonnull ComponentDescriptor descriptor )
			throws ComponentCreationException
	{
		Preconditions.checkNotNull( label, "label is null!" );
		Preconditions.checkNotNull( descriptor, "descriptor is null!" );

		ComponentItemConfig config = getConfig().addNewComponent();
		config.setType( descriptor.getType() );
		config.setLabel( label );
		ComponentItemImpl component = ComponentItemImpl.newInstance( this, config );
		component.setAttribute( ComponentItem.TYPE, descriptor.getLabel() );
		if( descriptor.getHelpUrl() != null )
			component.getContext().setHelpUrl( descriptor.getHelpUrl() );

		try
		{
			component.setBehavior( componentRegistry.createBehavior( descriptor, component.getContext() ) );
			component.addEventListener( BaseEvent.class, componentListener );
			if( counterSupport instanceof AggregatedCounterSupport )
				( ( AggregatedCounterSupport )counterSupport ).addChild( component );
			componentList.addItem( component );
		}
		catch( ComponentCreationException e )
		{
			component.delete();
			throw e;
		}

		return component;
	}

	protected abstract Connection createConnection( OutputTerminal output, InputTerminal input );

	private ComponentItemImpl loadComponent( ComponentItemConfig config ) throws ComponentCreationException
	{
		final ComponentItemImpl component = ComponentItemImpl.newInstance( this, config );
		try
		{
			component.setBehavior( componentRegistry.loadBehavior( config.getType(), component.getContext() ) );
			componentList.addItem( component, new Runnable()
			{
				@Override
				public void run()
				{
					component.addEventListener( BaseEvent.class, componentListener );
					if( counterSupport instanceof AggregatedCounterSupport )
						( ( AggregatedCounterSupport )counterSupport ).addChild( component );
				}
			} );
		}
		catch( ComponentCreationException e )
		{
			log.error( "Unable to load component: " + component, e );
			badComponents.add( config );
			component.release();
			throw e;
		}

		return component;
	}

	public ComponentItem injectComponent( ComponentItemConfig config ) throws ComponentCreationException
	{
		ComponentItemConfig componentConf = getConfig().addNewComponent();
		componentConf.set( config );
		try
		{
			return loadComponent( componentConf );
		}
		catch( ComponentCreationException e )
		{
			getConfig().removeComponent( getConfig().sizeOfComponentArray() - 1 );

			throw e;
		}
	}

	@Nonnull
	@Override
	public Collection<ComponentItem> getComponents()
	{
		return componentList.getItems();
	}

	@Override
	public ComponentItem getComponentByLabel( @Nonnull String label )
	{
		for( ComponentItem component : componentList.getItems() )
			if( component.getLabel().equals( label ) )
				return component;

		return null;
	}

	@Nonnull
	@Override
	public Collection<Connection> getConnections()
	{
		return connectionList.getItems();
	}

	@Nonnull
	@Override
	public Connection connect( @Nonnull OutputTerminal output, @Nonnull InputTerminal input )
	{
		// Locate the correct CanvasItem for the Connection.
		CanvasItem canvas = output.getTerminalHolder().getCanvas();
		if( canvas == input.getTerminalHolder().getCanvas() )
		{
			if( canvas != this )
				return canvas.connect( output, input );
		}
		// If the two Terminals are in separate CanvasItems, the connection should
		// be made in the ProjectItem.
		else if( !( this instanceof ProjectItem && canvas.getProject() == this ) )
			return canvas.getProject().connect( output, input );

		// Make sure an identical Connection doesn't already exist.
		for( Connection connection : output.getConnections() )
			if( connection.getInputTerminal().equals( input ) )
				return connection;

		// Create the Connection.
		final Connection connection = createConnection( output, input );
		connectionList.addItem( connection, new Runnable()
		{
			@Override
			public void run()
			{
				connection.getOutputTerminal().addEventListener( TerminalConnectionEvent.class, connectionListener );
			}
		} );

		return connection;
	}

	@Override
	public boolean isRunning()
	{
		return running;
	}

	@Override
	public boolean isCompleted()
	{
		return completed;
	}

	@Override
	public void release()
	{
		fireBaseEvent( RELEASED );

		testRunner.unregisterTask( executionTask, Phase.values() );
		ReleasableUtils.releaseAll( componentList, connectionList );
		summary = null;

		ReleasableUtils.releaseAll( counterStatisticSupport, statisticHolderSupport );

		super.release();
	}

	protected void disconnect( final Connection connection )
	{
		connectionList.removeItem( connection, new Runnable()
		{
			@Override
			public void run()
			{
				for( int i = getConfig().sizeOfConnectionArray() - 1; i >= 0; i-- )
				{
					ConnectionConfig connConfig = getConfig().getConnectionArray( i );
					if( connection.getOutputTerminal().getId().equals( connConfig.getOutputTerminalId() )
							&& connection.getInputTerminal().getId().equals( connConfig.getInputTerminalId() ) )
					{
						getConfig().removeConnection( i );
					}
				}
			}
		} );
	}

	@Override
	public long getLimit( @Nonnull String counterName )
	{
		return limits.containsKey( counterName ) ? limits.get( counterName ) : -1;
	}

	@Override
	public void setLimit( @Nonnull String counterName, long counterValue )
	{
		if( counterValue > 0 )
			limits.put( counterName, counterValue );
		else
			limits.remove( counterName );

		StringBuilder s = new StringBuilder();
		for( Entry<String, Long> e : limits.entrySet() )
			s.append( e.getKey() ).append( '=' ).append( e.getValue().toString() ).append( ';' );
		setAttribute( LIMITS_ATTRIBUTE, s.toString() );

		if( TIMER_COUNTER.equals( counterName ) )
			fixTimeLimit();
		fireBaseEvent( LIMITS );
	}

	@Override
	@Nullable
	public Summary getSummary()
	{
		synchronized( datesLock )
		{
			if( startTime != null && endTime != null )
			{
				log.info( "Generating summary report for CavasItem {}", getLabel() );
				Summary summary = getSummaryCreator().createSummary( startTime, endTime );
				fireBaseEvent( SUMMARY );
				return summary;
			}
			log.error( "Unable to create summary report: startTime: {}, endTime: {}", startTime, endTime );
		}
		return null;
	}

	@Nonnull
	public CanvasObjectItem duplicate( @Nonnull CanvasObjectItem obj )
	{
		if( !( obj instanceof ComponentItemImpl ) )
			throw new IllegalArgumentException( obj + " needs to be an instance of: " + ComponentItemImpl.class.getName() );

		ComponentItemConfig config = getConfig().addNewComponent();
		config.set( ( ( ComponentItemImpl )obj ).getConfig() );
		if( obj.getCanvas().equals( this ) )
			config.setLabel( "Copy of " + config.getLabel() );
		config.setId( addressableRegistry.generateId() );
		try
		{
			return loadComponent( config );
		}
		catch( ComponentCreationException e )
		{
			// Shouldn't happen...
			throw new RuntimeException( e );
		}
	}

	public abstract void onComplete( EventFirer source );

	public abstract SummaryCreator getSummaryCreator();

	private void fixTimeLimit()
	{
		if( timeLimitFuture != null )
			timeLimitFuture.cancel( true );

		if( running && limits.containsKey( TIMER_COUNTER ) )
		{
			long delay = limits.get( TIMER_COUNTER ) * 1000 - time;
			if( delay > 0 )
			{
				timeLimitFuture = scheduler.schedule( new TimeLimitTask(), delay, TimeUnit.MILLISECONDS );
			}
		}
	}

	protected void reset()
	{
		boolean isRunning = isRunning();
		synchronized( datesLock )
		{
			if( isRunning )
			{
				startTime = new Date();
				endTime = null;
			}
			else
			{
				startTime = null;
			}
		}
		hasStarted = isRunning;
		setTime( 0 );
		fixTimeLimit();
	}

	protected synchronized void setTime( long time )
	{
		this.time = time;
	}

	protected void setRunning( boolean running )
	{
		log.debug( "setRunning canvas: " + this.getClass().toString() );
		if( this.running != running )
		{
			this.running = running;
			if( running )
			{
				triggerAction( CanvasItem.COUNTER_RESET_ACTION );
				triggerAction( CanvasItem.START_ACTION );
			}
			fireBaseEvent( RUNNING );
		}
	}

	public void setCompleted( boolean completed )
	{
		if( this.completed != completed )
		{
			log.debug( "Canvas completed state changed to {}", completed );
			this.completed = completed;
			if( completed )
			{
				synchronized( datesLock )
				{
					endTime = new Date();
				}
			}
		}
		if( completed ) triggerAction( READY_ACTION );
	}

	public void markClean()
	{
		lastSavedHash = DigestUtils.md5Hex( getConfig().xmlText() );
	}

	@Override
	public boolean isDirty()
	{
		return !DigestUtils.md5Hex( getConfig().xmlText() ).equals( lastSavedHash );
	}

	@Override
	public boolean isStarted()
	{
		return hasStarted;
	}

	@Override
	public boolean isLoadingError()
	{
		return loadingErrors;
	}

	@Override
	public void cancelComponents()
	{
		for( ComponentItem component : getComponents() )
			if( component.isBusy() )
				component.triggerAction( ComponentItem.CANCEL_ACTION );
	}

	@Override
	public StatisticVariable getStatisticVariable( String statisticVariableName )
	{
		return statisticHolderSupport.getStatisticVariable( statisticVariableName );
	}

	@Nonnull
	@Override
	public Set<String> getStatisticVariableNames()
	{
		return statisticHolderSupport.getStatisticVariableNames();
	}

	@Nonnull
	@Override
	public Collection<? extends StatisticVariable> getStatisticVariables()
	{
		return statisticHolderSupport.getStatisticVariables();
	}

	@Override
	public boolean isAbortOnFinish()
	{
		return abortOnFinish.getValue();
	}

	@Override
	public Property<Boolean> abortOnFinishProperty()
	{
		return abortOnFinish;
	}

	@Override
	public void setAbortOnFinish( boolean abort )
	{
		abortOnFinish.setValue( abort );
	}

	@Nonnull
	@Override
	public Set<Statistic.Descriptor> getDefaultStatistics()
	{
		return ImmutableSet.<Statistic.Descriptor>of( new StatisticDescriptorImpl( this, REQUEST_VARIABLE, "PER_SECOND",
				StatisticVariable.MAIN_SOURCE ) );
	}

	private class ComponentListener implements EventHandler<BaseEvent>
	{
		@Override
		public void handleEvent( BaseEvent event )
		{
			if( event.getKey().equals( RELEASED ) && counterSupport instanceof AggregatedCounterSupport )
				( ( AggregatedCounterSupport )counterSupport ).removeChild( ( CounterHolder )event.getSource() );

			if( event.getKey().equals( DELETED ) )
			{
				final ComponentItem component = ( ComponentItem )event.getSource();
				componentList.removeItem( component, new Runnable()
				{
					@Override
					public void run()
					{
						for( int i = 0; i < getConfig().sizeOfComponentArray(); i++ )
						{
							if( component.getId().equals( getConfig().getComponentArray( i ).getId() ) )
							{
								getConfig().removeComponent( i );
								break;
							}
						}
					}
				} );
			}
		}
	}

	private class ConnectionListener implements EventHandler<TerminalConnectionEvent>
	{
		@Override
		public void handleEvent( TerminalConnectionEvent event )
		{
			if( event.getEvent() == TerminalConnectionEvent.Event.DISCONNECT )
				disconnect( event.getConnection() );
		}
	}

	private class ActionListener implements EventHandler<BaseEvent>
	{
		@Override
		public void handleEvent( BaseEvent event )
		{
			if( event instanceof ActionEvent )
			{
				if( CounterHolder.COUNTER_RESET_ACTION.equals( event.getKey() ) )
				{
					reset();
				}
			}
			else if( LoadUI.isController() && event instanceof CounterEvent && isRunning() )
			{
				CounterEvent cEvent = ( CounterEvent )event;
				long limit = getLimit( cEvent.getKey() );
				if( limit > 0 && limit <= cEvent.getSource().getCounter( cEvent.getKey() ).get() )
				{
					List<TestExecution> executions = testRunner.getExecutionQueue();

					triggerAction( STOP_ACTION );
					triggerAction( COMPLETE_ACTION );
					if( !executions.isEmpty() && executions.get( 0 ).getCanvas() == CanvasItemImpl.this )
					{
						executions.get( 0 ).complete();
					}
				}
			}
		}
	}

	private class TimeUpdateTask implements Runnable
	{
		private final long creationTime;
		private final long initialTime;

		public TimeUpdateTask()
		{
			creationTime = System.currentTimeMillis();
			initialTime = time;
		}

		@Override
		public void run()
		{
			final long timePassed = ( System.currentTimeMillis() - creationTime );
			setTime( initialTime + timePassed );
		}
	}

	private static final Function<ComponentItem, Future<BaseEvent>> busyComponentFuture = new Function<ComponentItem, Future<BaseEvent>>()
	{
		@Override
		public Future<BaseEvent> apply( ComponentItem component )
		{
			EventFuture<BaseEvent> busyEventFuture = EventFuture.forKey( component, ComponentItem.BUSY );
			return component.isBusy() ? busyEventFuture : Futures.<BaseEvent>immediateFuture( null );
		}
	};

	/**
	 * Called when an execution which affects this canvas changes phases.
	 * <br/>
	 * If this canvas is not affected by the execution <em>(eg. the project is started but this canvas is a
	 * scenario which is not linked to the project)</em> this method does not even get called!
	 *
	 * @param execution execution
	 * @param phase new phase
	 */
	protected void onExecutionTask( TestExecution execution, Phase phase )
	{
		if( execution.contains( this ) )
		{
			switch( phase )
			{
				case START:
					onStartExecution();
					break;
				case PRE_STOP:
					onPreStopExecution( execution );
					break;
				case STOP:
					onStopExecution();
					break;
			}
		}
	}

	private void onStartExecution()
	{
		log.debug( "Starting canvas {}", getLabel() );
		setRunning( true );
		setTime( 0 );
		synchronized( datesLock )
		{
			startTime = new Date();
			endTime = null;
		}
		timerFuture = scheduler.scheduleAtFixedRate( new TimeUpdateTask(), 250, 250, TimeUnit.MILLISECONDS );
		fixTimeLimit();
		hasStarted = true;
		setCompleted( false );
	}

	private void onPreStopExecution( TestExecution execution )
	{
		log.debug( "Pre-stopping canvas {}", getLabel() );
		hasStarted = false;
		if( timeLimitFuture != null )
			timeLimitFuture.cancel( true );

		if( isAbortOnFinish() )
		{
			log.debug( "Cancelling all components running on {}", this );
			cancelComponents();
		}
		else
		{
			log.debug( "Waiting for all components to complete on {}", this );
			waitForComponentsToComplete();
		}
		log.debug( "Calling onComplete on execution canvas" );
		onComplete( execution.getCanvas() );
	}

	private void onStopExecution()
	{
		log.debug( "Stopping canvas {}", getLabel() );
		if( timerFuture != null )
			timerFuture.cancel( true );
		setRunning( false );
	}

	private void waitForComponentsToComplete()
	{
		for( Future<BaseEvent> future : Iterables.transform( getComponents(), busyComponentFuture ) )
		{
			try
			{
				future.get( 1, TimeUnit.MINUTES );
			}
			catch( InterruptedException | ExecutionException | TimeoutException e )
			{
				log.error( "Failed waiting for a Component to complete", e );
			}
		}
		for( ComponentItem component : getComponents() )
		{
			component.setBusy( false );
		}
		log.debug( "All components completed in canvas {}", getLabel() );
	}

	private class TimeLimitTask implements Runnable
	{
		@Override
		public void run()
		{
			List<TestExecution> executions = testRunner.getExecutionQueue();

			setTime( getLimit( TIMER_COUNTER ) * 1000 );
			triggerAction( STOP_ACTION );
			triggerAction( COMPLETE_ACTION );

			if( !executions.isEmpty() && executions.get( 0 ).getCanvas() == CanvasItemImpl.this )
			{
				executions.get( 0 ).complete();
			}
		}
	}

	private class TimerCounter implements Counter
	{
		@Override
		public long get()
		{
			return time / 1000;
		}

		@Override
		public void increment()
		{
			increment( 1 );
		}

		@Override
		public void increment( long value )
		{
			throw new UnsupportedOperationException( "The timer counter cannot be manually incremented!" );
		}

		@Override
		public Class<Long> getType()
		{
			return Long.class;
		}

		@Override
		public Long getValue()
		{
			return time / 1000;
		}
	}

	private class CanvasTestExecutionTask implements TestExecutionTask
	{
		@Override
		public void invoke( TestExecution execution, Phase phase )
		{
			onExecutionTask( execution, phase );
		}
	}
}
