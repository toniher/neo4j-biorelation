#!/usr/bin/env python

import networkx
import obonet
import sys
import logging
import re
import argparse
from py2neo import Graph, Node, Relationship

parser = argparse.ArgumentParser()
parser.add_argument("miontology",
                    help="The mi.owl file")
parser.add_argument("config",
                    help="JSON configuration file", default=None, nargs='*')

opts=parser.parse_args()

conf = {}
conf["host"] = "localhost"
conf["port"] = 7687
conf["protocol"] = "bolt"

if len( opts.config ) > 0:	
		with open(opts.config) as json_data_file:
				data = json.load(json_data_file)
				
				if data.has_key("neo4j"):
								if data["neo4j"].has_key("host"):
												conf["host"] = data["neo4j"]["host"]
								if data["neo4j"].has_key("protocol"):
												conf["protocol"] = data["neo4j"]["protocol"]
								if data["neo4j"].has_key("port"):
												conf["port"] = data["neo4j"]["port"]

server = conf["protocol"]+"://"+conf["host"]+":"+str( conf["port"] )



obograph = obonet.read_obo(opts.miontology)


exclude = [ 'subset', 'xref', 'relationship', 'is_a' ]
relate = [ 'relationship', 'is_a' ]
nodes = []
edges = []

# print( len( graph ) )

keys = []

# ['name', 'def', 'subset', 'synonym', 'relationship', 'is_a', 'comment', 'xref', 'alt_id', 'created_by', 'creation_date']

def process_string( prestr ):

	# We are rming info from PMID and so...
	prestr = prestr.replace( "\\\"", "#@" )
	parts = prestr.split( "\"" )
	
	if len( parts ) > 1 :
		parts[1] = parts[1].replace( "#@", "\\\"" )
		return parts[1]
		
	return prestr

def process_edges( node1, data, typerel ):
	
	edges = []
	
	for d in data :
		
		edge = []
		edge.append( node1 )
		
		if typerel == "relationship" and d.startswith( "part_of" ) :
			edge.append( d.replace( "part_of ", "" ) )
			edge.append( "part_of" )		
		else :
			edge.append( d )
			edge.append( typerel )

		edges.append( edge )
		
	return edges

for n in obograph.nodes(data=True):
	node = {}
	node["id"] = n[0]
	
	for k in list( n[1].keys() ) :
		
		if not k in exclude :
					
			if isinstance( n[1][k], list ):
				
				node[k] = []
				for v in n[1][k]:
					node[k].append( process_string( v ) )
					
					# node[k] = n[1][k]
			else :
							
				node[k] = process_string( n[1][k] )
			
			
		if k in relate :
			
			# Handle edges
			edges.extend( process_edges( n[0], n[1][k], k ) )
			#print( n[1][k] )
			
	
	#print( n[1] )
	# print( node )
	nodes.append( node )




def process_statement( statement, graph ):
    
    tx = graph.begin()

    #print statements
    logging.info('proc sent')

    #print statement
    tx.run(statement)

    tx.process()
    tx.commit()

def process_relationship( label, statement, graph ):
	
	tx = graph.begin()
	
	#print statements
	logging.info('proc sent')

	start = graph.nodes.match(label, id=statement[0] ).first()
	end = graph.nodes.match(label, id=statement[1] ).first()
	rel = Relationship( start, statement[2], end )
	
	tx.create( rel )
	
	tx.process()
	tx.commit()

def create_cyper_keys( node ) :
	
	cyphArr = []
	
	for k in node :
		
		cyphStr = ""
		if isinstance( node[k], list ) :
			cyphStr = k + ": [" + ", ".join( map( lambda x: "'" + re.sub( r"\'", "\\'", x ) + "'", node[k] ) ) + "]"
		else :
			cyphStr = k + ": '" + re.sub( r"\'", "\\'", node[k] ) + "'"
			
		cyphArr.append( cyphStr )
		
		
	return ", ".join( cyphArr )
	

graph = Graph(server)

label = "MI"

idxout = graph.run("CREATE CONSTRAINT ON (n:"+label+") ASSERT n.id IS UNIQUE")

for n in nodes :
	
	node_keys = create_cyper_keys( n )
	statement = "CREATE (n:"+label+" { "+node_keys+" })"
	
	process_statement( statement, graph )
	
for e in edges :
	
	#print( e )
	process_relationship( label, e, graph )
	