#!/usr/bin/env python3
import csv
import sys

def main(argv):

        if len(sys.argv) < 2:
                sys.exit()

        termfile = sys.argv[1]
        termdeffile = sys.argv[2]

        definition_list={}

        # QUOTE_NONE: these are TAB-delimited files whose fields (GO definitions)
        # can contain literal double quotes; treat them as plain text, not CSV quoting.
        reader = csv.reader(open(termdeffile),delimiter="\t",quoting=csv.QUOTE_NONE)
        
        for row in reader:
            
            definition = row[1]
            definition = definition.replace('"', '\\"')
            definition_list[str(row[0])] = definition


        reader = csv.reader(open(termfile),delimiter="\t",quoting=csv.QUOTE_NONE)
        
        print( "id:ID\tname\tterm_type\tacc\tdefinition" )

        
        for row in reader:
                print( "GOID:"+ row[0] + "\t" + row[1] + "\t" + row[2] + "\t" + row[3] + "\t", end='' )
                if row[0] in  definition_list :
                        print( definition_list[ row[0] ], end='' )
                print( "\n", end='' )
            


if __name__ == "__main__":
        main(sys.argv[1:])
