# File system replication 



**Compilation**
- javac -Xlint:unchecked -d . **/*.java 
- javac Server/Main.java                
- javac Client/Main.java  

**Execution**
  java CS6378P3.Server.Main <servers_config_path>
  java CS6378P3.Client.Main <servers_config_path>  <clients_config_path>
**Example:**
- java CS6378P3.Server.Main Launcher/ServerConfigs/config0.txt
- java CS6378P3.Client.Main Launcher/ServerConfigs/config1.txt Launcher/ClientConfigs/config_0.txt

**contributors**
- PJ Avinash
- Evan Wright

Design document:
