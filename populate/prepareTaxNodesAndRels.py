#!/usr/bin/env python3
import csv
import sys

# Hashes for storing stuff
parentid={}
scientific_list={}
names_list={}


def create_taxid(line):
	
		taxid = str(line[0]).strip()
		rank = line[2].strip()
		
		list_names =  "$".join( names_list[taxid] )
		
		if '"' in list_names :
			list_names = '"' + list_names + '"'
		
		# We assume always al params
		statement = [ "TAXID:"+taxid, taxid, rank, scientific_list[taxid], list_names]
		#print statement
		
		parentid[taxid] = str(line[1]).strip()
		
		return statement

def main(argv):

		if len(sys.argv) < 3:
				sys.exit()
	
		nodes = sys.argv[1]
		names = sys.argv[2]
		outdir = sys.argv[3]
	
		reader =  csv.reader(open(names),delimiter="|",quoting=csv.QUOTE_NONE)

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
		
		# Nodes: write each taxon straight out. create_taxid() also populates
		# parentid as a side effect, which the relationships pass below relies on.
		reader = csv.reader(open(nodes),delimiter="|",quoting=csv.QUOTE_NONE)

		fileout=open( outdir + "/taxnodes.csv", 'w+')
		fileout.write( "\t".join( [ "id:ID", "taxid:int", "rank", "scientific_name", "name:string[]" ] ) + "\n" )
		for row in reader:
				fileout.write( "\t".join( create_taxid(row) ) + "\n" )
		fileout.close()

		# Relationships: one has_parent edge per taxon.
		fileout=open( outdir + "/taxrels.csv", 'w+')
		fileout.write( "\t".join( [ "TAXID:START_ID", "TAXID:END_ID", ":TYPE" ] ) + "\n" )
		for key in parentid:
				parent_taxid = parentid[key]
				fileout.write( "\t".join( [ "TAXID:"+ key, "TAXID:" + parent_taxid, "has_parent" ] ) + "\n" )
		fileout.close()


if __name__ == "__main__":
        main(sys.argv[1:])

