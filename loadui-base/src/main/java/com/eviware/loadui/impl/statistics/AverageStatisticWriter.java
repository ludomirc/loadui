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
package com.eviware.loadui.impl.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.eviware.loadui.api.statistics.StatisticVariable;
import com.eviware.loadui.api.statistics.StatisticsManager;
import com.eviware.loadui.api.statistics.StatisticsWriter;
import com.eviware.loadui.api.statistics.StatisticsWriterFactory;
import com.eviware.loadui.api.statistics.store.Entry;

import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StatisticsWriter for calculating the average of given values.
 * 
 * @author dain.nilsson
 */
public class AverageStatisticWriter extends AbstractStatisticsWriter
{
	public static final String TYPE = "AVERAGE";

	private Percentile perc = new Percentile( 90 );
	private Percentile medianPercentile = new Percentile( 50 );

	private Logger log = LoggerFactory.getLogger( AverageStatisticWriter.class );

	public enum Stats
	{
		AVERAGE, AVERAGE_COUNT, AVERAGE_SUM, STD_DEV, STD_DEV_SUM, PERCENTILE, MEDIAN;
	}

	/**
	 * Average = Average_Sum / Average_Count
	 * 
	 * Where is: * Average_Sum is sum of all requests times ( total or range ) *
	 * Average_Count is total number of requests ( total or range )
	 * 
	 * Standard_Deviation = Square_Sum / Average_Count Where
	 * 
	 * Where is: *Square_Sum =Math.pow( timeTaken - Average_Sum, 2 ) *timeTaken
	 * is last request time taken
	 * 
	 * For calculating 90 percentile it needed to remember data received. To do
	 * this is defined buffer which hold last n values. Default value is 1000,
	 * but this can be changed by set/getPercentileBufferSize. Since percentile
	 * is expensive operation, specially when buffer is large, it should be
	 * calculated just before it should be written to database.
	 */

	double average = 0.0;
	double avgSum = 0.0;
	long avgCnt = 0L;
	double stdDev = 0.0;
	double sumTotalSquare = 0.0;
	double percentile = 0;
	double median = 0;

	protected ArrayList<Double> values = new ArrayList<Double>();

	public AverageStatisticWriter( StatisticsManager statisticsManager, StatisticVariable variable,
			Map<String, Class<? extends Number>> trackStructure )
	{
		super( statisticsManager, variable, trackStructure );
	}

	@Override
	public String getType()
	{
		return TYPE;
	}

	@Override
	public int getValueCount()
	{
		return 1;
	}

	/**
	 * values : [ value:long ]
	 */
	@Override
	public void update( long timestamp, Number value )
	{
		synchronized( this )
		{
			double doubleValue = value.doubleValue();
			this.values.add( doubleValue );
			avgSum += doubleValue;
			avgCnt++ ;
			if( lastTimeFlushed + delay <= System.currentTimeMillis() )
			{
				flush();
			}
		}
	}

	public Entry output()
	{
		if( avgCnt == 0 )
		{
			return null;
		}
		else
		{
			average = avgSum / avgCnt;
			double[] pValues = new double[values.size()];
			sumTotalSquare = 0;
			for( int cnt = 0; cnt < values.size(); cnt++ )
			{
				pValues[cnt] = values.get( cnt ).longValue();
				sumTotalSquare += Math.pow( pValues[cnt] - average, 2 );
			}
			stdDev = Math.sqrt( sumTotalSquare / avgCnt );
			percentile = perc.evaluate( pValues );
			median = medianPercentile.evaluate( pValues );
			lastTimeFlushed = System.currentTimeMillis();
			
			Entry e = at( lastTimeFlushed ).put( Stats.AVERAGE.name(), average ).put( Stats.AVERAGE_COUNT.name(), avgCnt ).put(
					Stats.AVERAGE_SUM.name(), avgSum ).put( Stats.STD_DEV_SUM.name(), sumTotalSquare ).put(
							Stats.STD_DEV.name(), stdDev ).put( Stats.PERCENTILE.name(), percentile ).put( Stats.MEDIAN.name(), median )
							.build();
			
			// reset counters
			avgSum = 0;
			avgCnt = 0;
			values.clear();
			return e;
		}
	}
	
	/**
	 * Aggregates a list of Entrys.
	 * 
	 * Note that we are using Population based Standard deviation,
	 * as opposed to Sample based Standard deviation.
	 * 
	 * @author henrik.olsson
	 */
	public Entry aggregate(List<Entry> entries)
	{
		if( entries.size() == 0)
			return null;
		if( entries.size() == 1)
			return entries.get( 0 );
		
		Entry lastEntry = entries.get( entries.size() - 1);

		double totalSum = 0;
		long totalCnt = 0;
		double stddev_partA = 0;
		
		for( Entry e : entries )
		{
			long count = e.getValue( Stats.AVERAGE_COUNT.name() ).longValue();
			double average = e.getValue( Stats.AVERAGE.name() ).doubleValue();
			
			// average
			totalSum += count * average;
			totalCnt += count;
			
			// stddev (population based), implements http://en.wikipedia.org/wiki/Standard_deviation#Combining_standard_deviations
			stddev_partA += count * ( Math.pow( e.getValue( Stats.STD_DEV.name() ).doubleValue(), 2) + Math.pow( average, 2 ) );
			
			// http://davidmlane.com/hyperstat/A79567.html
		}
		double totalAverage = totalSum / totalCnt;
		long timestamp = lastEntry.getTimestamp();
		double stddev = Math.sqrt( stddev_partA / totalCnt - Math.pow( totalAverage, 2 ) );
		
		return at( timestamp ).put( Stats.AVERAGE.name(), totalAverage ).put( Stats.AVERAGE_COUNT.name(), totalCnt ).put( Stats.STD_DEV.name(), stddev ).build(false);
	}
	
	@Override
	protected void reset()
	{
		average = 0L;
		avgSum = 0L;
		avgCnt = 0;
		stdDev = 0.0;
		sumTotalSquare = 0.0;
		percentile = 0;
		median = 0;
	}

	/**
	 * Factory for instantiating AverageStatisticWriters.
	 * 
	 * @author dain.nilsson
	 * 
	 *         Define what Statistics this writer should write in Track.
	 * 
	 */
	public static class Factory implements StatisticsWriterFactory
	{
		@Override
		public String getType()
		{
			return TYPE;
		}

		@Override
		public StatisticsWriter createStatisticsWriter( StatisticsManager statisticsManager, StatisticVariable variable )
		{
			Map<String, Class<? extends Number>> trackStructure = new TreeMap<String, Class<? extends Number>>();

			// init statistics
			trackStructure.put( Stats.AVERAGE.name(), Double.class );
			trackStructure.put( Stats.AVERAGE_COUNT.name(), Double.class );
			trackStructure.put( Stats.AVERAGE_SUM.name(), Double.class );
			trackStructure.put( Stats.STD_DEV.name(), Double.class );
			trackStructure.put( Stats.STD_DEV_SUM.name(), Double.class );
			trackStructure.put( Stats.PERCENTILE.name(), Double.class );
			trackStructure.put( Stats.MEDIAN.name(), Double.class );

			return new AverageStatisticWriter( statisticsManager, variable, trackStructure );
		}
	}

}