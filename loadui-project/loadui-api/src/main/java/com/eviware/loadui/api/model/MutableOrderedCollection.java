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
package com.eviware.loadui.api.model;

/**
 * Mutable version of OrderedCollection, which allows adding, removing and
 * reordering of children.
 * 
 * @author dain.nilsson
 * 
 * @param <ChildType>
 */
public interface MutableOrderedCollection<ChildType> extends OrderedCollection<ChildType>
{
	/**
	 * Moved a contained child to a new position, as defined by the given
	 * positional index.
	 * 
	 * @param child
	 * @param index
	 */
	public void moveChild( ChildType child, int index );

	/**
	 * Appends a new child to the end of the collection.
	 * 
	 * @param child
	 */
	public void addChild( ChildType child );

	/**
	 * Removes a child from the OrderedCollection.
	 * 
	 * @param child
	 */
	public void removeChild( ChildType child );
}