#!/usr/bin/env python3
import csv
import sys

def main(argv):

        if len(sys.argv) < 2:
                sys.exit()

        termfile = sys.argv[1]
        term2termfile = sys.argv[2]

        relationshipmap={}

        reader = csv.reader(open(termfile),delimiter="\t",quoting=csv.QUOTE_NONE)
        
        for row in reader:
            
            relationshipmap[str(row[0])] = row[1]


        reader = csv.reader(open(term2termfile),delimiter="\t",quoting=csv.QUOTE_NONE)
        
        print( ":TYPE\tGO:START_ID\tGO:END_ID" )

        
        for row in reader:
                print( relationshipmap[ row[1] ] + "\t" + "GOID:" + row[2] + "\t" + "GOID:" + row[3] )
            


if __name__ == "__main__":
        main(sys.argv[1:])
