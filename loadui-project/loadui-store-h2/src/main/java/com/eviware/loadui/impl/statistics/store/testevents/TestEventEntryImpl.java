/* 
 * Copyright 2011 SmartBear Software
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
package com.eviware.loadui.impl.statistics.store.testevents;

import com.eviware.loadui.api.testevents.TestEvent;

public class TestEventEntryImpl implements TestEvent.Entry
{
	private final TestEvent testEvent;
	private final String sourceLabel;
	private final String typeLabel;

	public TestEventEntryImpl( TestEvent testEvent, String sourceLabel, String typeLabel )
	{
		this.testEvent = testEvent;
		this.sourceLabel = sourceLabel;
		this.typeLabel = typeLabel;
	}

	@Override
	public TestEvent getTestEvent()
	{
		return testEvent;
	}

	@Override
	public String getTypeLabel()
	{
		return typeLabel;
	}

	@Override
	public String getSourceLabel()
	{
		return sourceLabel;
	}

}