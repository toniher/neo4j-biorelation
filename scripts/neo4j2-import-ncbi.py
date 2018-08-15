#!/usr/bin/env python
from py2neo import Graph, Node, Relationship

import json
import csv
import logging
import argparse
import sys
from pprint import pprint

parser = argparse.ArgumentParser()
parser.add_argument("nodes",
                    help="The nodes.dmp file")
parser.add_argument("names",
                    help="The names.dmp file")
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

logging.basicConfig(level=logging.ERROR)

numiter = 10000

graph = Graph(server)

label = "TAXID"

# Hashes for storing stuff
parentid={}
scientific_list={}
names_list={}

idxout = graph.run("CREATE CONSTRAINT ON (n:"+label+") ASSERT n.id IS UNIQUE")

def process_relationship( statements, graph ):
	
	tx = graph.begin()
	
	#print statements
	logging.info('proc sent')
	
	for statement in statements:
		#print statement
		start = graph.nodes.match(statement[0], id=int( statement[1] )).first()
		end = graph.nodes.match(statement[0], id=int( statement[2] )).first()
		rel = Relationship( start, statement[3], end )
		
		tx.create( rel )
	
	tx.process()
	tx.commit()


def process_statement( statements, graph ):
    
    tx = graph.begin()

    #print statements
    logging.info('proc sent')

    for statement in statements:
        #print statement
        tx.run(statement)

    tx.process()
    tx.commit()


def create_taxid(line, number):
    taxid = str(line[0]).strip()
    rank = line[2].strip()
    
    # We assume always al params
    statement = "CREATE (n:"+label+" { id : "+taxid+", rank: \""+rank+"\", scientific_name:'"+scientific_list[taxid]+"', name:"+names_list[taxid]+" })"
    #print statement
    
    parentid[taxid] = str(line[1]).strip()
    
    return statement


logging.info('storing name info')
reader =  csv.reader(open(opts.names),delimiter="|")

iter = 0
taxidsave = 1
scientific = ''
names = []

for row in reader:
    taxid = int(row[0])
    #print taxid
    #Escaping names
    namentry = str(row[1]).strip().replace('"', '\\"')
    #print namentry
    
    # If different, let's save
    if taxid != taxidsave :
        namestr = ""
        
        for i in xrange( 0 ,len(names)):
            names[i] = '"' + names[i] + '"'
    
        namestr = "[" + ",".join(names) + "]"
        # Escaping scientific
        scientific = scientific.replace("'", "\\'")
        
        scientific_list[str(taxidsave)] = scientific
        names_list[str(taxidsave)] = namestr
        #statement = "MATCH (n { id: "+str(taxidsave)+" }) SET n.scientific_name = '"+scientific+"', n.name = "+namestr+" RETURN 1"
        #print statement
        #statements.append( statement )
    
        # Empty
        names = []
        scientific = ''
        taxidsave = taxid
            
    names.append( namentry )
    if ( row[3] ).strip() == 'scientific name' :
        scientific = namentry

#Adding last one!
scientific_list[str(taxidsave)] = scientific
names_list[str(taxidsave)] = namestr

logging.info('creating nodes')
reader = csv.reader(open(opts.nodes),delimiter="|")
iter = 0

list_statements =  []
statements = []

for row in reader:
	statement = create_taxid(row, iter)
	statements.append( statement )
	iter = iter + 1
	if ( iter > numiter ):
		list_statements.append( statements )
		iter = 0
		statements = []

list_statements.append( statements )

print len( list_statements )

for statements in list_statements :
	process_statement( statements, graph )

# p.map( process_statement, list_statements )

idxout = graph.run("CREATE INDEX ON :"+label+"(rank)")

# We keep no pool for relationship

logging.info('adding relationships')
iter = 0

list_statements =  []
statements = []

for key in parentid:

    parent_taxid = parentid[key]
    
    
    statement = [ label, key, parent_taxid, "has_parent" ]
    statements.append( statement )
    
    iter = iter + 1
    if ( iter > numiter ):
        list_statements.append( statements )
        iter = 0
        statements = []

list_statements.append( statements )


#for statements in list_statements :
#	process_relationship( statements, graph )

#idxout = graph.run("CREATE INDEX ON :"+label+"(scientific_name)")
#idxout = graph.run("CREATE INDEX ON :"+label+"(name)")
