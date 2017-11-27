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
$> java -jar bin/mcsvr.jar -help
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
Default is 0.
* -maxCacheBytes &lt;int&gt; : The maximum sum of the sizes of items in the cache before
the lru advises the cache to delete items. Default is 2,147,483,647.
* -lruRecoverPct &lt;int&gt : Percent of the cache bytes to recover by the lru when cache size 
exceeds maxCacheBytes.
* -port &lt;int&gt; : Port the server is run on.
* -reapInterval &lt;int&gt; : Expired items in the cache are removed by a reaper thread. This 
specifies the number of seconds between sweeps by that reaper. Lower numbers will incur a 
performance penalty as the entire cache is write-locked during a sweep.
* -serverTimeout &lt;seconds&gt; : If the server is very busy, disconnects closes the client 
connection after a number of seconds without a write. This may help load-shedding on a busy 
server

## Test

## Architecture and Design