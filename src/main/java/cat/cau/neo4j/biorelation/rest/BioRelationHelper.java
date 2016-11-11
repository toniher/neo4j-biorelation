package cat.cau.neo4j.biorelation.rest;

import org.neo4j.graphdb.GraphDatabaseService;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.DynamicLabel;

import org.neo4j.graphdb.Transaction;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;


public class BioRelationHelper {

	JsonObject InJsonRoot;
	String InValue;

	public JsonArray arrayListNodes2JSON( ArrayList<Node> arrayNodes, GraphDatabaseService db ) {
		
		JsonArray jsonArray = new JsonArray();
		
		Iterator<Node> nodeIterator = arrayNodes.iterator();
		while(nodeIterator.hasNext()){
			
			Node lNode = nodeIterator.next();

			JsonObject jsonObject = new JsonObject();

			try (Transaction tx = db.beginTx()) {

				Iterable<String> lNodeProps = lNode.getPropertyKeys();
				Iterator<String> itrProp = lNodeProps.iterator();
				while ( itrProp.hasNext() ) {

					String prop = itrProp.next();

					jsonObject = this.addValueFromProp( jsonObject, lNode, prop );

				}

				tx.success();
			}

			jsonArray.add( jsonObject );

		}
		
		return jsonArray;
	}

	public JsonObject arrayListNodes2JSONkeyRoot( ArrayList<Node> arrayNodes, String keyRoot, GraphDatabaseService db ) {
		
		JsonObject jsonRoot = new JsonObject();
		
		Iterator<Node> nodeIterator = arrayNodes.iterator();
		while(nodeIterator.hasNext()){
			
			Node lNode = nodeIterator.next();
			String prop;
			String propValue = "undefined"; // In case something is wrong

			JsonObject jsonObject = new JsonObject();

			try (Transaction tx = db.beginTx()) {

				Iterable<String> lNodeProps = lNode.getPropertyKeys();
				Iterator<String> itrProp = lNodeProps.iterator();
				while ( itrProp.hasNext() ) {

					prop = itrProp.next();

					jsonObject = this.addValueFromProp( jsonObject, lNode, prop );

					if ( prop.equals( keyRoot ) ) {

						this.addValueFromProp2Root( jsonRoot, lNode, prop );
						propValue = this.InValue;
						jsonRoot = this.InJsonRoot;

					}

				}

				tx.success();

			}

			if ( jsonRoot.get(propValue) != null ) {
				JsonArray tempJsonArray = jsonRoot.get(propValue).asArray();
				tempJsonArray.add( jsonObject );
				jsonRoot.set( propValue, tempJsonArray );
			}


		}
	
	return jsonRoot;

	}


	public JsonArray arrayListNodes2JSONextraInt( ArrayList<Node> arrayNodes, ArrayList<Integer> arrayInteger, String extra, GraphDatabaseService db ) {
		
		JsonArray jsonArray = new JsonArray();
		
		Integer intIter = 0;
		
		Iterator<Node> nodeIterator = arrayNodes.iterator();
		while(nodeIterator.hasNext()){
			
			Node lNode = nodeIterator.next();
	
			JsonObject jsonObject = new JsonObject();
	
			try (Transaction tx = db.beginTx()) {
	
				Iterable<String> lNodeProps = lNode.getPropertyKeys();
				Iterator<String> itrProp = lNodeProps.iterator();
				while ( itrProp.hasNext() ) {
	
						String prop = itrProp.next();

						jsonObject = this.addValueFromProp( jsonObject, lNode, prop );
						
						// Adding extra arrayvalue
						jsonObject.add( extra, arrayInteger.get( intIter ) );
						
				}
	
				tx.success();
			}
	
			jsonArray.add( jsonObject );
			intIter++;
	
		}
		
		return jsonArray;
	}

	private JsonObject addValueFromProp( JsonObject jsonObject, Node lNode, String prop ) {

		String value;

		Object propValue = lNode.getProperty( prop, null );

		if ( propValue != null ) {

			if ( propValue.getClass().isArray() ) {
				JsonArray jsonArray = new JsonArray();

				// Let's cast to String[]
				for ( String s: (String[]) propValue ) {
					jsonArray = this.addValue2JsonArray( jsonArray, s );
				}

				jsonObject.add( prop, jsonArray );

			} else {

				value = propValue.toString();

				jsonObject = this.addValue2JsonObject( jsonObject, prop, value ); 

			}

		}

		return jsonObject;
	}

	private JsonObject addValue2JsonObject( JsonObject jsonObject, String prop, String value ) {

		if ( StringUtils.isNumeric( value ) ) {
			int valueInt = Integer.parseInt( value );
			double valueFloat = Float.parseFloat( value );
			
			if ( valueInt == valueFloat ) {
				jsonObject.add( prop, valueInt );
			} else {
				jsonObject.add( prop, valueFloat );
			}
		} else {
	
			jsonObject.add( prop, value );
		}

		return jsonObject;
	}


	private JsonArray addValue2JsonArray( JsonArray jsonArray, String value ) {

		if ( StringUtils.isNumeric( value ) ) {
			int valueInt = Integer.parseInt( value );
			double valueFloat = Float.parseFloat( value );
			
			if ( valueInt == valueFloat ) {
				jsonArray.add( valueInt );
			} else {
				jsonArray.add( valueFloat );
			}
		} else {
	
			jsonArray.add( value );
		}

		return jsonArray;
	}

	private void addValueFromProp2Root( JsonObject jsonRoot, Node lNode, String prop ) {

		String value;

		value = lNode.getProperty( prop ).toString();

		if ( jsonRoot.get(value) == null ) {
			jsonRoot.add( value, new JsonArray() );
		}

		this.InJsonRoot = jsonRoot;
		this.InValue = value;

	}

	public JsonObject hashMapNodes2JSON( Hashtable<String, ArrayList<Node>> hashTableNodes, GraphDatabaseService db ) {

		JsonObject jsonObject = new JsonObject();
	
		Set<String> keys = hashTableNodes.keySet();
		for(String key: keys){
				ArrayList<Node> arrayNodes = hashTableNodes.get( key );
				JsonArray jsonArray = arrayListNodes2JSON( arrayNodes, db );
				jsonObject.add( key, jsonArray );
		}
	
		return jsonObject;
	}

	public static boolean allElementsTheSame(String[] array) {
	
		if (array.length == 0) {
			return true;
		} else {
			String first = array[0];
			for (String element : array) {
			if (!element.equals(first)) {
					return false;
				}
			}
			
			return true;
	
		}
	}
	
	public ArrayList<Long> nodes2Array( Iterable<Node> ListNodes ) {
		
		ArrayList<Long> pathNodes = new ArrayList<Long>();
		
		Iterator<Node> itr = ListNodes.iterator();
		while ( itr.hasNext() ) {
			
			pathNodes.add( itr.next().getId() ); 
		}
		
		return pathNodes;
	}
	
	public ArrayList<Node> nonRedundantArrayNodeList( ArrayList<Node> inputList ) {

		Set<Integer> listNodeIds = new HashSet<Integer>();
		ArrayList<Node> outputList = new ArrayList<Node>();

		Iterator<Node> nodeIterator = inputList.iterator();
		while(nodeIterator.hasNext()){
			
			Node lNode = nodeIterator.next();

			Iterable<String> lNodeProps = lNode.getPropertyKeys();
			Iterator<String> itrProp = lNodeProps.iterator();
			while ( itrProp.hasNext() ) {

				String prop = itrProp.next();

				if ( prop.equals("id") ) {
					String value = lNode.getProperty( prop ).toString();
					if ( StringUtils.isNumeric( value ) ) {
						int valueInt = Integer.parseInt( value );

						if ( ! listNodeIds.contains( valueInt ) ) {
							listNodeIds.add( valueInt );
							outputList.add( lNode );
						}
					} 
				}
			}
		}

		return( outputList );

	}
}


