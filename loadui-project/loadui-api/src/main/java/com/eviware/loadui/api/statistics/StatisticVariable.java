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
package com.eviware.loadui.api.statistics;

import java.util.Set;

import com.eviware.loadui.api.traits.Describable;

/**
 * A Statistical Variable containing several Statistics, for several instances.
 * 
 * @author dain.nilsson
 */
public interface StatisticVariable extends Describable.Mutable
{
	/**
	 * When writing to the local, or main source, use this String as the source
	 * identifier.
	 */
	public static final String MAIN_SOURCE = "main";

	/**
	 * Gets the name of the StatisticVariable.
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Gets the StatisticHolder which this StatisticVariable belongs to.
	 * 
	 * @return
	 */
	public StatisticHolder getStatisticHolder();

	/**
	 * Gets the available sources of the StatisticVariable.
	 * 
	 * @return
	 */
	public Set<String> getSources();

	/**
	 * Gets the available Statistic names for the StatisticVariable.
	 * 
	 * @return
	 */
	public Set<String> getStatisticNames();

	/**
	 * Gets the Statistic corresponding to the given statistic name and source.
	 * 
	 * @param statisticName
	 * @param source
	 * @return
	 */
	public Statistic<?> getStatistic( String statisticName, String source );

	/**
	 * Gets all writers assigned to this variable
	 * 
	 * @return
	 */
	public Set<StatisticsWriter> getWriters();

	/**
	 * Mutable version of a StatisticVariable which is used to provide data to
	 * its writers.
	 * 
	 * @author dain.nilsson
	 */
	public interface Mutable extends StatisticVariable
	{
		/**
		 * Updates the StatisticVariable.Mutable with new data, which will be
		 * passed to the attached StatisticsWriters.
		 * 
		 * @param timestamp
		 * @param value
		 */
		public void update( long timestamp, Number value );
	}
}
