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
package com.eviware.loadui.impl.component.categories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.eviware.loadui.api.component.ComponentContext;
import com.eviware.loadui.api.component.categories.FlowCategory;
import com.eviware.loadui.api.terminal.Connection;
import com.eviware.loadui.api.terminal.InputTerminal;
import com.eviware.loadui.api.terminal.OutputTerminal;

import com.eviware.loadui.api.terminal.TerminalMessage;

import com.eviware.loadui.impl.component.ActivityStrategies;
import com.eviware.loadui.util.BeanInjector;

/**
 * Base class for flow components which defines base behavior which can be
 * extended to fully implement a flow ComponentBehavior.
 * 
 * @author dain.nilsson
 */
public abstract class FlowBase extends BaseCategory implements FlowCategory
{
	private static final int BLINK_TIME = 1000;

	private final InputTerminal incomingTerminal;
	private final List<OutputTerminal> outgoingTerminals = new ArrayList<OutputTerminal>();
	private Map<String, Class<?>> inputSignature = Collections.emptyMap();

	private final ScheduledExecutorService executor;
	private final Runnable activityRunnable;
	private long lastMsg;
	private ScheduledFuture<?> activityFuture;

	/**
	 * Constructs a FlowBase.
	 * 
	 * @param context
	 *           A ComponentContext to bind the FlowBase to.
	 */
	public FlowBase( ComponentContext context )
	{
		super( context );
		executor = BeanInjector.getBean( ScheduledExecutorService.class );

		getContext().setActivityStrategy( ActivityStrategies.ON );
		incomingTerminal = context.createInput( INCOMING_TERMINAL, "Incoming Data" );

		activityRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				long now = System.currentTimeMillis();
				if( lastMsg + BLINK_TIME <= now )
				{
					getContext().setActivityStrategy( ActivityStrategies.ON );
					activityFuture = null;
				}
				else
				{
					activityFuture = executor.schedule( activityRunnable, lastMsg + BLINK_TIME, TimeUnit.MILLISECONDS );
				}
			}
		};
	}

	/**
	 * Creates an additional OutputTerminal and appends it to the
	 * outgoingTerminals List.
	 * 
	 * @return
	 */
	final public OutputTerminal createOutgoing()
	{
		OutputTerminal output = getContext().createOutput( OUTGOING_TERMINAL + " " + ( outgoingTerminals.size() + 1 ),
				"Output Terminal " + " " + ( outgoingTerminals.size() + 1 ) );
		getContext().setSignature( output, inputSignature );
		outgoingTerminals.add( output );

		return output;
	}

	/**
	 * Deletes the OutputTerminal in the outgoingTerminals List with the highest
	 * numbering (the last one to be added). If no OutputTerminals exist in this
	 * list, nothing will happen.
	 */
	final public void deleteOutgoing()
	{
		if( outgoingTerminals.size() > 0 )
			getContext().deleteTerminal( outgoingTerminals.remove( outgoingTerminals.size() - 1 ) );
	}

	@Override
	public void onTerminalConnect( OutputTerminal output, InputTerminal input )
	{
		super.onTerminalConnect( output, input );

		updateSignature();
	}

	@Override
	public void onTerminalDisconnect( OutputTerminal output, InputTerminal input )
	{
		super.onTerminalDisconnect( output, input );

		updateSignature();
	}

	@Override
	public void onTerminalSignatureChange( OutputTerminal output, Map<String, Class<?>> signature )
	{
		super.onTerminalSignatureChange( output, signature );

		updateSignature();
	}

	@Override
	final public InputTerminal getIncomingTerminal()
	{
		return incomingTerminal;
	}

	@Override
	final public List<OutputTerminal> getOutgoingTerminalList()
	{
		return Collections.unmodifiableList( outgoingTerminals );
	}

	@Override
	final public String getCategory()
	{
		return CATEGORY;
	}

	@Override
	final public String getColor()
	{
		return COLOR;
	}

	protected void updateSignature()
	{
		Map<String, Class<?>> newSig = new HashMap<String, Class<?>>();
		for( Connection connection : incomingTerminal.getConnections() )
			for( Entry<String, Class<?>> entry : connection.getOutputTerminal().getMessageSignature().entrySet() )
				newSig.put( entry.getKey(), entry.getValue() );
		inputSignature = newSig;

		for( OutputTerminal output : getOutgoingTerminalList() )
			getContext().setSignature( output, inputSignature );
	}

	@Override
	public void onTerminalMessage( OutputTerminal output, InputTerminal input, TerminalMessage message )
	{
		if( input == incomingTerminal )
		{
			lastMsg = System.currentTimeMillis();
			if( activityFuture == null )
			{
				getContext().setActivityStrategy( ActivityStrategies.BLINKING );
				activityFuture = executor.schedule( activityRunnable, BLINK_TIME, TimeUnit.MILLISECONDS );
			}
		}
	}
}
