#!/usr/bin/env python3
import csv
import sys

def main(argv):
    
    if len(sys.argv) < 2:
            sys.exit()

    isoformfile = sys.argv[1]
    
    reader = csv.reader(open(isoformfile),delimiter="\t",quoting=csv.QUOTE_NONE)
    
    listiso = []
    listpre = None
    l = 0
    
    for row in reader:
        
        parts = row[0].split("-")
                
        if l > 0 and listpre != parts[0] :
            
            preparts = listiso[0].split("-")
            
            if listiso[0] != preparts[0]+"-1"+"\t"+preparts[0]:
                listiso = [preparts[0]+"-1"+"\t"+preparts[0]] + listiso
            
            print( "\n".join( listiso ) )
            listpre = parts[0]
            listiso = []

        listiso.append( row[0]+"\t"+parts[0] )
        
        l = l + 1 
    
    preparts = listiso[0].split("-")
    if listiso[0] != preparts[0]+"-1"+"\t"+preparts[0]:
        listiso = [preparts[0]+"-1"+"\t"+preparts[0]] + listiso
            
    print( "\n".join( listiso ) ) 
    
if __name__ == "__main__":
    main(sys.argv[1:])