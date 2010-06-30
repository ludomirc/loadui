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
package com.eviware.loadui.launcher;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.eviware.loadui.launcher.api.GroovyCommand;
import com.eviware.loadui.launcher.impl.FileGroovyCommand;
import com.eviware.loadui.launcher.impl.ResourceGroovyCommand;

public class LoadUICommandLineLauncher extends LoadUILauncher
{
	private static final String LOCAL_OPTION = "l";
	private static final String FILE_OPTION = "f";
	private static final String AGENT_OPTION = "a";
	private static final String LIMITS_OPTION = "L";
	private static final String TESTCASE_OPTION = "t";
	private static final String PROJECT_OPTION = "p";
	private static final String WORKSPACE_OPTION = "w";

	public static void main( String[] args )
	{
		System.setSecurityManager( null );

		LoadUICommandLineLauncher launcher = new LoadUICommandLineLauncher( args );
		launcher.init();
		launcher.start();
	}

	private GroovyCommand command;

	public LoadUICommandLineLauncher( String[] args )
	{
		super( args );
	}

	@Override
	@SuppressWarnings( "static-access" )
	protected Options createOptions()
	{
		Options options = super.createOptions();
		options.addOption( WORKSPACE_OPTION, "workspace", true, "Sets the Workspace file to load" );
		options.addOption( PROJECT_OPTION, "project", true, "Sets the Project file to run" );
		options.addOption( TESTCASE_OPTION, "testcase", true,
				"Sets which TestCase to run (leave blank to run the entire Project)" );
		options.addOption( LIMITS_OPTION, "limits", true, "Sets the limits for the execution (e.g. -L 60:0:200 )" );
		options.addOption( OptionBuilder.withLongOpt( "agents" ).withDescription(
				"Sets the agents to use for the test ( usage -" + AGENT_OPTION
						+ " <ip>[:<port>][=<testCase>[,<testCase>] ...] )" ).hasArgs().create( AGENT_OPTION ) );
		options.addOption( FILE_OPTION, "file", true, "Executes the specified Groovy script file" );
		options.addOption( LOCAL_OPTION, "local", false, "Executes TestCases in local mode" );

		return options;
	}

	@Override
	protected void processCommandLine( CommandLine cmd )
	{
		super.processCommandLine( cmd );

		Map<String, Object> attributes = new HashMap<String, Object>();

		if( cmd.hasOption( PROJECT_OPTION ) )
		{
			attributes.put( "workspaceFile", cmd.hasOption( WORKSPACE_OPTION ) ? new File( cmd
					.getOptionValue( WORKSPACE_OPTION ) ) : null );
			attributes.put( "projectFile",
					cmd.hasOption( PROJECT_OPTION ) ? new File( cmd.getOptionValue( PROJECT_OPTION ) ) : null );
			attributes.put( "testCase", cmd.getOptionValue( TESTCASE_OPTION ) );
			attributes.put( "limits", cmd.hasOption( LIMITS_OPTION ) ? cmd.getOptionValue( LIMITS_OPTION ).split( ":" )
					: null );
			attributes.put( "localMode", cmd.hasOption( LOCAL_OPTION ) );
			Map<String, String[]> agents = null;
			if( cmd.hasOption( AGENT_OPTION ) )
			{
				agents = new HashMap<String, String[]>();
				for( String option : cmd.getOptionValues( AGENT_OPTION ) )
				{
					int ix = option.indexOf( "=" );
					if( ix != -1 )
						agents.put( option.substring( 0, ix ), option.substring( ix + 1 ).split( "," ) );
					else
						agents.put( option, null );
				}
			}
			attributes.put( "agents", agents );

			command = new ResourceGroovyCommand( "/RunTest.groovy", attributes );
		}
		else if( cmd.hasOption( FILE_OPTION ) )
		{
			command = new FileGroovyCommand( new File( cmd.getOptionValue( FILE_OPTION ) ), attributes );
		}
		else
		{
			printUsageAndQuit();
		}
	}

	@Override
	protected void start()
	{
		super.start();

		if( command != null )
			framework.getBundleContext().registerService( GroovyCommand.class.getName(), command, null );
	}
}
