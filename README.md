# Slack homework by Sean Sitter
Thank you for taking the time to review my homework exercise. 

Per the homework requirements, I have implemented a memcache server supporting the text protocol
commands: get, gets, set, cas, and delete.

## Build
The project uses the gradle build system. 

To build the server binary:
```
$> ./gradlew buildSvr
```
The server binary will be copied into the bin/ directory as 'mcsvr'.

## Run
To run the server first build the project and then execute:
```
$> java -jar bin/mcsvr
```
The binary accepts a handful of options, to list them execute:
```
$ java -jar bin/mcsvr.jar -help
usage: mcsvr
 -help                  show help message
 -idleTimeout <arg>     number of seconds before idle connection is closed
 -lruRecoverPct <arg>   percent of max size to recover on lru sweep
 -maxCacheBytes <arg>   the max cache size in bytes
 -port <arg>            server port
 -reapInterval <arg>    number of seconds between reaper sweeps
 -serverTimeout <arg>   number of seconds before server response times out
```
### command options
* -idleTimeout &lt;seconds&gt; : The server supports persistent connections. 
The connection with timeout after the configurable seconds of inactivity. 
Specify a value of 0 for no timeout. Note, this may cause broken clients to hange.

## Test

## Architecture and Design