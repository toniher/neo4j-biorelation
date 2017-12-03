#!/usr/bin/env python

import csv
import pprint
import sys

def main(argv):
                if len(sys.argv) < 2:
                                sys.exit()
                
                pre = ""
                chunk = 1000
                string = ""
                count = 0

                if len( sys.argv ) > 1:
                
                        with open(sys.argv[1],'r') as f:
                                        reader=csv.reader(f,delimiter='\t')
                                        for row in reader:
                        
                                                        
                                                        if row[0] != pre :
                                                                pre = row[0]
                                                                string = string + row[0]+"\t"+"UniProtKB-AC"+"\t"+row[0]+"\n"
                                                                string = string + "\t".join( row )+"\n"
                                                        else :
                                                                string = string + "\t".join( row )+"\n"                                        
                                                        count = count + 1
                                                        if count >= chunk :
                                                                count = 0
                                                                print string # Print if chunk size higger
                                                                string = ""
                        print string

if __name__ == "__main__":
        main(sys.argv[1:])
