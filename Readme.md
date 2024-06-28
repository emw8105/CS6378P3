# Distributed File System for Fault tolerance (uses 2 Phase commit)


## Testing on Local
**Compilation**
```
cd CS6378P3
javac -Xlint:unchecked -d . **/*.java 
javac Server/Main.java  
javac Client/Main.java 
``` 

**Execution**
- java CS6378P3.Server.Main <servers_config_path>
- java CS6378P3.Client.Main <servers_config_path>  <clients_config_path>
**Example:**
- java CS6378P3.Server.Main Launcher/ServerConfigs/config0.txt
- java CS6378P3.Client.Main Launcher/ServerConfigs/config1.txt Launcher/ClientConfigs/config_0.txt



## Testing on Cluster
**Setup**
```
git clone https://github.com/PJAvinash/CS6378P3.git
```
```
cd CS6378P3/Launcher
```

**First spin-up servers**
```
python3 spinservers.py <netid> ServerConfigs/utdcluster0.txt
```
**Spin up clients**
Open a new terminal and Spin clients 
- python3 spinclients.py <netid> <server_config_path> <client_config_path>
- server_config_path &  client_config_path are relative paths from the Launcher folder
```
 python3 spinclients.py <netid> ServerConfigs/utdcluster0.txt  ClientConfigs/utdcluster_0.txt
```
**free resources after testing**
- python3 freeports.py <netid> <client_config_path>
- python3 freeports.py <netid> <server_config_path>

**tested on Linux and mac**

- java version "1.8.0_341"
- Java(TM) SE Runtime Environment (build 1.8.0_341-b10)
- Java HotSpot(TM) 64-Bit Server VM (build 25.341-b10, mixed mode)

- openjdk version "21.0.2" 2024-01-16
- OpenJDK Runtime Environment Homebrew (build 21.0.2)
- OpenJDK 64-Bit Server VM Homebrew (build 21.0.2, mixed mode, sharing)


## Contributors
- PJ Avinash
- Evan Wright

## Design document:

Design Specifics:
- There are 7 servers each connected to the other, and N clients that make connections to a specific server and then close the connection when done
Each client determines which server to send a request to based on H(Ok), where H is a hash function applied to the desired object Ok, where the hash function returns a value between 0-6 corresponding to a server
- The servers keep replicas of the objects between themselves such that each object is stored in 3 locations, H(Ok), H(Ok) + 2 mod 7, and H(Ok) + 4 mod 7
- When a client requests to read a specific object, it hashes to find the server to request to
- When the server receives the request, it will reply with the current iteration of the object that is stored
- If the server is unavailable, the client can send a request to the next server in the hash sequence (ex: H(Ok) → H(Ok) + 2 mod 7 → H(Ok) + 4 mod 7)
- When a client requests to write to a specific object, it hashes to find the server to request to.
- When the server receives the write request, it performs write by executing a 2-phase commit protocol
- If 2 of the 3 total replicas agree to perform the write, then the write operation is considered a success and the response is returned to the client
- Should multiple clients attempt to write to the same object simultaneously, all the writes are serialized using locks for those partitions( each object copy sits in an associated partition to increase the throughput)
- If a Network partition occurs, each partition may still accept update requests, however, only the partition containing the non-single node (2 vs 1) can accept the requests
This is because a partition requires that 2/3 of the replicas are updated from any given update request, such that a server cannot commit an update by itself, therefore the singular process cannot process any updates
- In the meantime, clients will attempt to write to a server, if the server declares that it is unavailable due to the partitioning separating it from making a successful update, then the client can attempt to access server H(Ok)+2 modulo 7, which would land it in the partition containing the other 2 servers
- After a partition is merged, the changes between the two become synced. However, because the singular separated server in the partition was not able to make updates, it will simply accept the updates from the other side of the partition

 


 


 






 


 


 






 


 


 
 




 


 




 




 


 
 
 
 




 




 


 




 
















