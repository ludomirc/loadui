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
package com.eviware.loadui.ui.fx.util;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.eviware.loadui.api.base.OrderedCollection;
import com.eviware.loadui.api.events.BaseEvent;
import com.eviware.loadui.api.events.CollectionEvent;
import com.eviware.loadui.api.events.EventHandler;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

public class ObservableListsTest
{
	@Test
	public void getActuallyRemoved_test()
	{
		@SuppressWarnings( "unchecked" )
		ListChangeListener.Change<Integer> change = mock( ListChangeListener.Change.class );
		when( change.getRemoved() ).thenReturn( ImmutableList.of( 1, 2, 3 ) );
		when( change.getAddedSubList() ).thenReturn( ImmutableList.of( 3, 4 ) );
		when( change.wasRemoved() ).thenReturn( true );

		Set<Integer> actuallyRemoved = ObservableLists.getActuallyRemoved( change );

		assertTrue( actuallyRemoved.equals( ImmutableSet.of( 1, 2 ) ) );
	}

	@Test
	public void bindContentUnorderedCanBeRearangedAndStillStayInSync()
	{
		ObservableList<String> bindee = FXCollections.observableArrayList();
		bindee.addAll( "One", "Two", "Three", "Four", "Five" );
		ObservableList<String> binder = FXCollections.observableArrayList();
		ObservableLists.bindContentUnordered( binder, bindee );

		assertTrue( binder.containsAll( bindee ) );
		assertTrue( binder.equals( bindee ) );

		bindee.add( "Six" );
		bindee.remove( "Seven" );

		assertTrue( binder.containsAll( bindee ) );

		//Shuffle binder
		binder.add( binder.remove( 2 ) );
		binder.add( binder.remove( 4 ) );
		binder.add( binder.remove( 1 ) );

		bindee.add( "Eight" );
		bindee.remove( "Three" );

		assertTrue( binder.containsAll( bindee ) );
		assertFalse( binder.equals( bindee ) );
	}

	@Test
	@Ignore( "This is no longer the behavior, the callers MUST keep the references themselves." )
	public void bindContentUnorderedShouldKeepAStrongReferenceToTheBindee() throws InterruptedException
	{
		ObservableList<String> bindee = FXCollections.observableArrayList();
		WeakReference<ObservableList<String>> weakBindee = new WeakReference<>( bindee );

		ObservableList<String> binder = FXCollections.observableArrayList();
		ObservableLists.bindContentUnordered( binder, bindee );

		bindee = null;

		for( int i = 0; i < 5; i++ )
		{
			System.gc();
			Thread.sleep( 20 );
		}

		//There is no guarantee that the bindee is GCd even if there is no strong reference to it, but this test is better than nothing.
		assertThat( weakBindee.get(), notNullValue() );
	}

	@Test
	public void bindSortedReturnsAListThatIsAlwaysSorted()
	{
		ObservableList<Integer> unsorted = FXCollections.observableArrayList();
		ObservableList<Integer> sorted = FXCollections.observableArrayList();
		ObservableLists.bindSorted( sorted, unsorted, Ordering.natural() );

		unsorted.addAll( 3, 1, 4, 1, 5, 6, 9, 2 );
		assertThat( sorted, equalTo( Arrays.asList( 1, 1, 2, 3, 4, 5, 6, 9 ) ) );

		unsorted.removeAll( 2, 4 );
		unsorted.addAll( 7, 8 );
		assertThat( sorted, equalTo( Arrays.asList( 1, 1, 3, 5, 6, 7, 8, 9 ) ) );
	}

	@Test
	public void filterContainsOnlyValuesThatFulfilThePredicate()
	{
		ObservableList<Integer> allElements = FXCollections.observableArrayList();
		ObservableList<Integer> filteredElements = ObservableLists.filter( allElements, new Predicate<Integer>()
		{
			@Override
			public boolean apply( Integer input )
			{
				return input.intValue() % 2 == 0;
			}
		} );

		allElements.addAll( 1, 2, 3, 4 );
		allElements.addAll( 5, 6, 7 );
		assertThat( filteredElements, equalTo( asList( 2, 4, 6 ) ) );
	}

	@Test
	public void transformContainsTransformedValues()
	{
		ObservableList<Integer> allElements = FXCollections.observableArrayList();
		ObservableList<Integer> transformedElements = ObservableLists.transform( allElements,
				new Function<Integer, Integer>()
				{
					@Override
					public Integer apply( Integer input )
					{
						return input.intValue() * 2;
					}
				} );

		allElements.addAll( 1, 2, 3, 4 );
		allElements.addAll( 5, 6, 7 );
		assertThat( transformedElements, equalTo( asList( 2, 4, 6, 8, 10, 12, 14 ) ) );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Test
	public void ofCollection_should_reflectAnOrderedCollection()
	{
		// Setup
		ArgumentCaptor<EventHandler> eventHandlerArgument = ArgumentCaptor.forClass( EventHandler.class );
		OrderedCollection.Mutable<Integer> collection = mock( OrderedCollection.Mutable.class );
		when( collection.getChildren() ).thenReturn( newArrayList( 1, 2, 3 ) );

		ObservableList<Integer> observableList = ObservableLists.ofCollection( collection );

		// [initial state verification]
		verify( collection ).addEventListener( any( Class.class ), eventHandlerArgument.capture() );
		assertThat( observableList, equalTo( asList( 1, 2, 3 ) ) );

		// collection.addChild( 4 );
		when( collection.getChildren() ).thenReturn( newArrayList( 1, 2, 3, 4 ) );
		EventHandler eventhandler = eventHandlerArgument.getValue();
		eventhandler.handleEvent( new CollectionEvent( collection, OrderedCollection.CHILDREN,
				CollectionEvent.Event.ADDED, 4 ) );

		assertThat( observableList, equalTo( asList( 1, 2, 3, 4 ) ) );

		// collection.removeChild( 2 );
		when( collection.getChildren() ).thenReturn( newArrayList( 1, 3, 4 ) );
		eventhandler.handleEvent( new CollectionEvent( collection, OrderedCollection.CHILDREN,
				CollectionEvent.Event.REMOVED, 2 ) );

		assertThat( observableList, equalTo( asList( 1, 3, 4 ) ) );

		// collection.moveChild( 1, 2 );
		when( collection.getChildren() ).thenReturn( newArrayList( 3, 4, 1 ) );
		eventhandler.handleEvent( new BaseEvent( collection, OrderedCollection.CHILD_ORDER ) );

		assertThat( observableList, equalTo( asList( 3, 4, 1 ) ) );

	}

	@Test
	public void concatTest()
	{
		ObservableList<Integer> list1 = FXCollections.observableArrayList();
		list1.addAll( 1, 2, 3 );

		ObservableList<Integer> list2 = FXCollections.observableArrayList();
		list2.addAll( 4, 5 );

		ObservableList<Integer> list3 = FXCollections.observableArrayList();
		list3.addAll( 6, 7, 8, 9 );

		ObservableList<Integer> list1list2 = ObservableLists.concat( list1, list2 );

		assertThat( list1list2, equalTo( asList( 1, 2, 3, 4, 5 ) ) );

		list1.remove( new Integer( 1 ) );
		list2.remove( new Integer( 4 ) );

		assertThat( list1list2, equalTo( asList( 2, 3, 5 ) ) );

		ObservableList<Integer> list1list2list3 = ObservableLists.concat( list1, list2, list3 );

		assertThat( list1list2list3, equalTo( asList( 2, 3, 5, 6, 7, 8, 9 ) ) );

		list1.add( new Integer( 1 ) );
		list2.add( new Integer( 4 ) );

		assertThat( list1list2list3, equalTo( asList( 2, 3, 1, 5, 4, 6, 7, 8, 9 ) ) );

		list3.remove( new Integer( 8 ) );

		assertThat( list1list2list3, equalTo( asList( 2, 3, 1, 5, 4, 6, 7, 9 ) ) );
	}

}
