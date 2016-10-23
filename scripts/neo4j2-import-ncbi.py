#!/usr/bin/env python
import py2neo
from py2neo.packages.httpstream import http
from py2neo.cypher import cypher_escape
from multiprocessing import Pool

import httplib

import csv
import logging
import argparse
import sys
from pprint import pprint

httplib.HTTPConnection._http_vsn = 10
httplib.HTTPConnection._http_vsn_str = 'HTTP/1.0'

parser = argparse.ArgumentParser()
parser.add_argument("nodes",
                    help="The nodes.dmp file")
parser.add_argument("names",
                    help="The names.dmp file")

opts=parser.parse_args()

logging.basicConfig(level=logging.ERROR)

http.socket_timeout = 9999

numiter = 5000

graph = py2neo.Graph()
graph.bind("http://localhost:7474/db/data/")

label = "TAXID"

# Hashes for storing stuff
parentid={}
scientific_list={}
names_list={}

idxout = graph.cypher.execute("CREATE CONSTRAINT ON (n:"+label+") ASSERT n.id IS UNIQUE")

def process_statement( statements ):
    
    tx = graph.cypher.begin()

    #print statements
    logging.info('proc sent')

    for statement in statements:
        #print statement
        tx.append(statement)

    tx.process()
    tx.commit()


poolnum = 4;

p = Pool(poolnum)

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
res = p.map( process_statement, list_statements )

idxout = graph.cypher.execute("CREATE INDEX ON :"+label+"(rank)")

# We keep no pool for relationship
tx = graph.cypher.begin()

logging.info('adding relationships')
iter = 0

for key in parentid:

    parent_taxid = parentid[key]
    
    statement = "MATCH (c:"+label+" {id:"+str(key)+"}), (p:"+label+" {id:"+str(parent_taxid)+"}) CREATE (c)-[:has_parent]->(p)"
    #print statement

    tx.append(statement)

    iter = iter + 1
    if ( iter > numiter ):
        tx.process()
        tx.commit()
        tx = graph.cypher.begin()
        
        iter = 0

tx.process()
tx.commit()

idxout = graph.cypher.execute("CREATE INDEX ON :"+label+"(scientific_name)")
idxout = graph.cypher.execute("CREATE INDEX ON :"+label+"(name)")
