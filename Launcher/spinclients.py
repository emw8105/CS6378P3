import argparse
import subprocess
import re
import socket
from multiprocessing import Process
import os

currentHost = socket.gethostname()
current_directory = os.getcwd()

# Define argument parser to accept config file
parser = argparse.ArgumentParser()
parser.add_argument("userid", help = "name of user(netid)")
parser.add_argument("config", help="Path to configuration file")

args = parser.parse_args()

# Open and read the config file
with open(args.config, "r") as f:
    lines = f.readlines()
    
hosts = {}
# Loop through the lines and extract the hostname and port number using regex
for line in lines:
    match = re.match(r'^\d+ (\S+) (\d+)$', line)
    if match:
        hostname = match.group(1)
        port = int(match.group(2))
        hosts[hostname] = port

# Loop through each host and SSH into it to execute command
remotehosts = list(hosts.keys())
for host in remotehosts:
    if currentHost == host:
        # command = f"cd .. &&java Main Launcher/{args.config_file}"
        # need to cd to the server directory to run the server programs
        command = f"java CS6378P3.Server.Main Launcher/{args.config}"
    else:
        command = ( "ssh -f " + args.userid + "@" + host + " 'cd " + current_directory.removesuffix("Launcher")+ " && java CS6378P3.Server.Main Launcher/"+ args.config+ "'")
    print(command)
    process = subprocess.Popen(command, shell=True)
    process.wait()