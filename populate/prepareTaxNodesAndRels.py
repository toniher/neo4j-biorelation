#!/usr/bin/env python
from __future__ import print_function
import csv
import sys
import pprint

pp = pprint.PrettyPrinter(indent=4)


# Hashes for storing stuff
parentid={}
scientific_list={}
names_list={}

numiter = 5000

def process_statement( statements, fileout ):
		
		for statement in statements:
			fileout.write( "\t".join( statement ) + "\n" )
		
		return True


def create_taxid(line, number):
	
		taxid = str(line[0]).strip()
		rank = line[2].strip()
		
		# We assume always al params
		statement = [ taxid, rank, scientific_list[taxid], "$".join( names_list[taxid] ) ]
		#print statement
		
		parentid[taxid] = str(line[1]).strip()
		
		return statement

def main(argv):

		if len(sys.argv) < 3:
				sys.exit()
	
		nodes = sys.argv[1]
		names = sys.argv[2]
		outdir = sys.argv[3]
	
		reader =  csv.reader(open(names),delimiter="|")
		
		iter = 0
		taxidsave = 1
		scientific = ''
		names = []
		
		for row in reader:
				taxid = int(row[0])
				#print taxid
				#Escaping names
				namentry = str(row[1]).strip()
				namentry = str(row[1]).strip().replace('"', '""')
				# Following this: https://neo4j.com/developer/kb/how-do-i-use-load-csv-with-data-including-quotes/
				#print namentry
				
				if '"' in namentry:
					namentry = '"' + namentry + '"'
				
				# If different, let's save
				if taxid != taxidsave :
						# namestr = ""
						
						#for i in xrange( 0 ,len(names)):
						#	names[i] = '"' + names[i] + '"'
					
						#namestr = "[" + ",".join(names) + "]"
						# Escaping scientific
						# scientific = scientific.replace("'", "\\'")
						
						scientific_list[str(taxidsave)] = scientific
						names_list[str(taxidsave)] = names
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
		names_list[str(taxidsave)] = names
		
		reader = csv.reader(open(nodes),delimiter="|")
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
		
		print( len( list_statements ) )
		
		fileout=open( outdir + "/taxnodes.csv", 'w+')
		fileout.write( "\t".join( [ "id:ID", "rank", "scientific_name", "name" ] ) + "\n" )

		for statements in list_statements :
				process_statement( statements, fileout )
		fileout.close()
	
		iter = 0
		
		list_statements =  []
		statements = []
		
		for key in parentid:
		
				parent_taxid = parentid[key]
				
				
				statement = [ key, parent_taxid, "has_parent" ]
				statements.append( statement )
				
				iter = iter + 1
				if ( iter > numiter ):
						list_statements.append( statements )
						iter = 0
						statements = []
		
		list_statements.append( statements )
		
		fileout=open( outdir + "/taxrels.csv", 'w+')
		
		fileout.write( "\t".join( [ "TAXID:START_ID", "TAXID:END_ID", ":TYPE" ] ) + "\n" )
		for statements in list_statements :
				process_statement( statements, fileout )
		fileout.close()


if __name__ == "__main__":
        main(sys.argv[1:])

