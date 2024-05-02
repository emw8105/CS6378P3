# Distributed File System for Fault tolerance (uses 2 Phase commit)


## Testing on Local
**Compilation**
```
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
