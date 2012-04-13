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
package com.eviware.loadui.impl.reporting;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;

import com.eviware.loadui.api.reporting.ReportingManager;
import com.eviware.loadui.api.statistics.model.StatisticPage;
import com.eviware.loadui.api.statistics.store.Execution;
import com.eviware.loadui.api.summary.Summary;
import com.eviware.loadui.impl.reporting.statistics.ExecutionDataSource;
import com.eviware.loadui.impl.reporting.summary.SummaryDataSource;

public class ReportingManagerImpl implements ReportingManager
{
	private final ReportEngine reportEngine = new ReportEngine();

	@Override
	public void createReport( Summary summary )
	{
		try
		{
			reportEngine.generateJasperReport( new SummaryDataSource( summary ), "SummaryReport", summary.getChapters()
					.keySet().iterator().next() );
		}
		catch( JRException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void createReport( Summary summary, File file, String format )
	{
		try
		{
			reportEngine.generateJasperReport( new SummaryDataSource( summary ), "SummaryReport", file, format );
		}
		catch( JRException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public void createReport( String label, Execution execution, Collection<StatisticPage> pages,
			Map<Object, Image> charts )
	{
		try
		{
			reportEngine.generateJasperReport( new ExecutionDataSource( label, execution, pages, charts ),
					"ResultsReport", execution.getLabel() );
		}
		catch( JRException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public void createReport( String label, Execution execution, Collection<StatisticPage> pages,
			Map<Object, Image> charts, File file, String format )
	{
		try
		{
			reportEngine.generateJasperReport( new ExecutionDataSource( label, execution, pages, charts ),
					"ResultsReport", file, format );
		}
		catch( JRException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void createReport( String label, Execution execution, Collection<StatisticPage> pages,
			Map<Object, Image> charts, File jpFileToPrepend )
	{
		createReport( label, execution, pages, charts, getJpFromFile( jpFileToPrepend ) );
	}

	public void createReport( String label, Execution execution, Collection<StatisticPage> pages,
			Map<Object, Image> charts, JasperPrint jpToPrepend )
	{
		try
		{
			reportEngine.generateJasperReport( new ExecutionDataSource( label, execution, pages, charts ),
					"ResultsReport", execution.getLabel(), jpToPrepend );
		}
		catch( JRException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public void createReport( String label, Execution execution, Collection<StatisticPage> pages,
			Map<Object, Image> charts, File file, String format, File jpFileToPrepend )
	{
		createReport( label, execution, pages, charts, file, format, getJpFromFile( jpFileToPrepend ) );
	}

	public void createReport( String label, Execution execution, Collection<StatisticPage> pages,
			Map<Object, Image> charts, File file, String format, JasperPrint jpToPrepend )
	{
		try
		{
			reportEngine.generateJasperReport( new ExecutionDataSource( label, execution, pages, charts ),
					"ResultsReport", file, format, jpToPrepend );
		}
		catch( JRException e )
		{
			e.printStackTrace();
		}
	}

	private JasperPrint getJpFromFile( File file )
	{
		ObjectInputStream ois = null;
		try
		{
			ois = new ObjectInputStream( new FileInputStream( file ) );
			Object o = ois.readObject();
			return ( JasperPrint )o;
		}
		catch( IOException e )
		{
			return null;
		}
		catch( ClassNotFoundException e )
		{
			return null;
		}
		finally
		{
			if( ois != null )
			{
				try
				{
					ois.close();
				}
				catch( IOException e )
				{
					// ignore
				}
			}
		}
	}
}