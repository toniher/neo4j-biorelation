INSTALLATION

This installs the basic software. Data population happens in another step

docker build -t neo4j-biorelation-install .

docker run -d -p 1337:1337 -p 7474:7474 -p 7687:7687 -p 7473:7473 --name myneo4j -v /path/neo4j-data:/data -v /path/to/scratch:/scratch neo4j-biorelation-install
