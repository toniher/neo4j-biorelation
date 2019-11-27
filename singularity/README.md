# Build and run

    sudo singularity build neo4j-3.5.12.sif Singularity

    singularity instance start -B $(pwd)/scratch:/scratch -B $(pwd)/neo4j.conf:/var/lib/neo4j/conf/neo4j.conf -B $(pwd)/db:/data -B $(pwd)/logs:/logs -B $(pwd)/certificates:/certificates neo4j-3.5.12.sif neo4j

    nohup singularity run instance://neo4j &> log  &
