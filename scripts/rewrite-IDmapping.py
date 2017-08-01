#!/usr/bin/env python

import csv
import pprint
import sys

def main(argv):
		if len(sys.argv) < 2:
				sys.exit()

		blackIds = dict()
		pre = ""


		if len( sys.argv ) > 2:

			with open(sys.argv[2],'r') as f:
					reader=csv.reader(f,delimiter='\t')
					for row in reader:
						blackIds[row[0]] = 1

		with open(sys.argv[1],'r') as f:
				reader=csv.reader(f,delimiter='\t')
				for row in reader:

                                        if row[0] not in blackIds :
                                        
                                                if row[0] != pre :
                                                        pre = row[0]
                                                        print row[0]+"\t"+"UniProtKB-AC"+"\t"+row[0]
                                                print "\t".join( row )
                                                

if __name__ == "__main__":
        main(sys.argv[1:])
