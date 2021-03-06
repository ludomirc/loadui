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
package com.eviware.loadui.impl.layout;

import java.util.Map;

import com.eviware.loadui.api.layout.SeparatorLayoutComponent;
import com.eviware.loadui.util.MapUtils;
import com.google.common.collect.ImmutableMap;

public class SeparatorLayoutComponentImpl extends LayoutComponentImpl implements SeparatorLayoutComponent
{
	public static final String VERTICAL = "vertical";

	public SeparatorLayoutComponentImpl( Map<String, ?> args )
	{
		super( args );
	}

	public SeparatorLayoutComponentImpl( boolean vertical, String constraints )
	{
		this( ImmutableMap.of( VERTICAL, vertical, CONSTRAINTS, constraints ) );
	}

	@Override
	public boolean isVertical()
	{
		return MapUtils.getOr( properties, VERTICAL, false );
	}

	@Override
	public String getConstraints()
	{
		return MapUtils.getOr( properties, CONSTRAINTS, isVertical() ? "growy" : "newline, growx, spanx" );
	}
}
