// 
// Copyright 2011 eviware software ab
// 
// Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
// versions of the EUPL (the "Licence");
// You may not use this work except in compliance with the Licence.
// You may obtain a copy of the Licence at:
// 
// http://ec.europa.eu/idabc/eupl5
// 
// Unless required by applicable law or agreed to in writing, software distributed under the Licence is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
// express or implied. See the Licence for the specific language governing permissions and limitations
// under the Licence.
// 

/**
 * Checks for errors and increases global assertion count
 * 
 * @id com.eviware.Assertion
 * @help http://www.loadui.org/Analysis/assertion-component.html
 * @category analysis
 * @nonBlocking true
 */

import com.eviware.loadui.api.model.CanvasItem;
import com.eviware.loadui.impl.layout.OptionsProviderImpl
import com.eviware.loadui.util.layout.DelayedFormattedString
import com.eviware.loadui.util.statistics.CounterStatisticSupport

import org.codehaus.groovy.runtime.typehandling.GroovyCastException

createOutput( 'output', 'Failed messages', 'Messages that did not pass the assertion are outputted here.' )

def componentSignature = [
		"Assert" : String.class,
		"Min" : Long.class,
		"Value" : Long.class,
		"Max" : Long.class,
		"Period" : Long.class,
		"Tolerance" : Long.class,
		"Timestamp" : Long.class
		]
setSignature(output, componentSignature)

failureCounter = counters[CanvasItem.FAILURE_COUNTER]
assertionFailureCounter = counters[CanvasItem.ASSERTION_FAILURE_COUNTER]
totalCounter = counters[CanvasItem.ASSERTION_COUNTER]

// Expose statistics
counterStatisticSupport = new CounterStatisticSupport( component )
assertionFailuresVariable = addStatisticVariable( "Assertion Failures", "COUNTER" )
counterStatisticSupport.addCounterVariable( CanvasItem.ASSERTION_FAILURE_COUNTER, assertionFailuresVariable )
counterStatisticSupport.init()

//Properties
createProperty( 'value', String, "Select value" ) { value ->
	valueToAssert = value
	resetComponent()
}
createProperty( 'max', Long, 1000 )
createProperty( 'min', Long, 0 ) { value ->
	if(max.value < value ) {
		max.value = value
	}
}
createProperty( 'tolerance', Long, 1 )
createProperty( 'period', Long, 0 )

createProperty( 'sampleId', String, "" )
createProperty( 'failOnMissingID', Boolean, false )
createProperty( 'failOnMissingValue', Boolean, false )
createProperty( 'includeAssertedMessage', Boolean, false )

String valueToAssert = value.value

buffer = [] as LinkedList

outMsg = newMessage()

OptionsProviderImpl provider = new OptionsProviderImpl([])

assertedResetValue = 0
failedResetValue = 0

assertedDisplay = new DelayedFormattedString( '%d', 500, value { totalCounter.get()-assertedResetValue } )
failedDisplay = new DelayedFormattedString( '%d', 500, value { assertionFailureCounter.get()-failedResetValue } )
valueDisplay = new DelayedFormattedString( '%s', 500, value { value.value } )
minDisplay = new DelayedFormattedString( '%d', 500, value { min.value } )
maxDisplay = new DelayedFormattedString( '%d', 500, value { max.value } )

onConnect = { outgoing, incoming ->
	if( incoming == inputTerminal ){
		redraw()
	}
}

onDisconnect = { outgoing, incoming ->
	if( incoming == inputTerminal ){
		redraw()
	}
}

onSignature = { outgoing, signature ->
	redraw()
}

analyze = { message ->
	try{
		long timestamp = System.currentTimeMillis()
		
		if(!message.containsKey(value.value)) {
			if( failOnMissingValue.value ) {
				raiseFailure(message, timestamp, null)
				totalCounter.increment()
			}
			return
		}
		
		double val = message[value.value]
		
		if( sampleId.value ?: "" != "" && message[SampleCategory.SAMPLE_ID] != sampleId.value ) {
			if( failOnMissingID.value ) {
				raiseFailure(message, timestamp, val)
				totalCounter.increment()
			}
			return
		}
		
		if( val < min.value || val > max.value ) {
			synchronized( buffer ) {
				buffer.addLast( timestamp )
				if( buffer.size() >= tolerance.value ) {
					def lit = buffer.listIterator()
					//Remove old entries
					if( period.value > 0 ) {
						long oldest = timestamp - period.value * 1000
						while( lit.hasNext() ) if( lit.next() < oldest ) lit.remove()
					}
				}
				
				if( buffer.size() >= tolerance.value ) {
					raiseFailure(message, timestamp, val)
					buffer.clear()
				}
			}
		}
		
		totalCounter.increment()
	}
	catch(GroovyCastException e){
		log.debug("The Assertion component expects integers in Input Data, but it got something else.")
	}
	catch(Exception e){
		ex(e, "Assertion -> analyze")
	}
}

raiseFailure = {message, timestamp, value ->
	assertionFailureCounter.increment()
	failureCounter.increment()
	
	m = includeAssertedMessage.value ? message : outMsg
	
	m["Assert"] = valueToAssert
	m["Min"] = min.value
	m["Value"] = value
	m["Max"] = max.value
	m["Period"] = period.value
	m["Tolerance"] = tolerance.value
	m["Timestamp"] = timestamp
	send(output, m)
}

onRelease = { 
	ReleasableUtils.releaseAll( counterStatisticSupport, failedDisplay, assertedDisplay )
}

resetComponent = {
	buffer.clear()
	assertedResetValue = 0
	failedResetValue = 0
}

onAction( "RESET" ) { ->
	resetComponent()
}

ex = {t, m ->
	println("-------------------------------")
	println("exception in ${m} method in groovy")
	println("type: ${t}")
	println("message: ${t.getMessage()}")
	println("stacktrace:")
	int tCnt = 0
	for(item in t.getStackTrace()){
		if(tCnt == 0){
			println("${item}")
		}
		else{
			println("\t${item}")
		}
		tCnt++
	}
	println("-------------------------------")
}

def redraw()
{
	def options = []
	for( conn in inputTerminal.connections ) {
		for( key in conn.outputTerminal.messageSignature.keySet() )
			if( !options.contains(key))
				options += key
	}

	provider = new OptionsProviderImpl([])
	provider.options = options
	
	if( !options.contains(value.value) )
		value.value = 'Select value'
		
	//Layout
	layout {
		property( property: value, widget: 'comboBox', label: 'Value', options: provider, constraints: 'w 100!' )
		separator( vertical: true )
	//	box {
			property( property: min, label: 'Min', min: 0 )
			property( property: max, label: 'Max', min: 0 )
	//	}
		separator( vertical: true )
	//	box {
	//		property( property: tolerance, label: 'Tolerance', min: 1, constraints:'wrap 1')
	//		property( property: period, label: 'Period', min: 0 )
	//	}
	//	separator( vertical: true )
		box(layout:'wrap, ins 0') {
			box( widget:'display', layout:'wrap 2') {
				node( label:'Asserted', fString: assertedDisplay, constraints:'w 50!' )
				node( label:'Failed', fString: failedDisplay, constraints:'w 50!' )
				node( label:'Min', fString: minDisplay, constraints:'w 50!' )
				node( label:'Max', fString: maxDisplay, constraints:'w 50!' )
			}
			action( 
				label: 'Reset', 
				action: {
					buffer.clear()
					assertedResetValue = totalCounter.get()
					failedResetValue = assertionFailureCounter.get()
				}, 
				constraints:'align right'
			)
		}
	}
}

redraw()

//Compact Layout
compactLayout {
	box( widget:'display' ) {
		node( label:'Value', fString: valueDisplay, constraints:'w 60!' )
		node( label:'Min', fString: minDisplay, constraints:'w 60!' )
		node( label:'Max', fString: maxDisplay, constraints:'w 60!' )
		node( label:'Asserted', fString: assertedDisplay )
		node( label:'Failed', fString: failedDisplay )
	}
}

settings( label: "General" ) {
	property( property: tolerance, label: 'Tolerance', min: 1 )
	property( property: period, label: 'Period' )
	property( property: failOnMissingValue, label: 'Fail on missing value' )
	property( property: sampleId, label: 'Sample ID' )
	property( property: failOnMissingID, label: 'Fail on mismatching ID' )
	property( property: includeAssertedMessage, label: 'Include original message in failure messages' )
}