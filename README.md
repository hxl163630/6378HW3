Jajodia-Mutchler voting algorithm.

Let there be eight servers, A, B, C, D, E, F, G, and H. There is only one data object X, replicated across the eight servers, that is subject to writes. Initially, the version number (VN), replicas update (RU) and distinguished site (DS) values for X at all servers are 1, 8, and A, respectively. The rule for selection of distinguished site favors the server that is alphabetically smallest among the candidates. Refer to Figure 1 for the sequence of network partitioning/merges that the system should go through.

Figure 1: Sequence of network state transitions.
Each server runs on a different machine. Initially, all the servers have reliable socket connections with each other
forming one connected component as represented by the oval at the top of the figure. Subsequently, partitions and mergers happen as shown. In order to emulate partitioning and mergers, all socket connections between servers in the same partition are maintained/created, while all socket connections between servers in different partition(s) are severed.
1. You must attemp at least two writes in each of the network components show in Figure 1. A write can originate
at any of the servers in the corresponding partition.
2. After each write attempt, successful or unsuccessful, output the VN, RU, and DS values for each server.
