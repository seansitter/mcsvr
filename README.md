# Memcache implementation by Sean Sitter
Thank you for taking the time to review my Slack homework exercise!

Per the homework requirements, I have implemented a memcache server supporting the text protocol
commands: get, gets, set, cas, and delete.

## Build
The project uses the gradle build system. 

To build the server binary:
```shell
$> ./gradlew buildSvr
```
The server binary will be copied into the bin/ directory as 'mcsvr'.

## Running
To run the server first build the project and then execute:
```shell
$> java -jar bin/mcsvr
```
The binary accepts a handful of options, to list them execute:
```shell
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

## Testing
The project features unit tests and functional tests.
#### Unit Tests
The project includes unit tests and functional tests. Strategy for unit tests was to 
test all critical classes. Simple POJOS and other data transfer classes may not have coverage. 
In a production project they *should* have coverage.

To run the unit tests:
```shell
$> ./gradlew test
```

#### Functional Tests
The functional tests are driven by python scripts in the test/ directory. 
```shell
$> ls test/
smallmaxsz_test.py	stdsvrcfg_test.py
```
These scripts run/kill a real server for each test case, and run live commands against it with 
a python client. The different scripts have different configurations of the server. 
* smallmaxsz_test.py : configures a server with a small maxCacheBytes for testing the lru
* stdsvrcfg_test.py : standard configuration of the server for testing basic operations.

These tests are executable. The scripts require the pymemcache python library.
```shell
$> pip install pymemcache
```

To execute a test client, first make sure no server is already running, then ex:
```shell
$> ./test/stdsvrcfg_test.py
```

## Architecture and Design
#### Dependency Injection / Guice
The project uses Guice for dependency injection to isolate components for testing and reduce boilerplate code.
The DI configuration is in the McServerConfig class.

#### Networking
The networking frontend implemented as a netty pipeline. Netty is a networking framework which simplifies 
creation of asynchronous non-blocking IO. Asynchronous non-blocking IO is event based, with a limited 
number of threads that execute an event loop and dispatch data as it becomes available. Contrasted with
the thread per connection synchronous blocking model, the non-blocking model can typically handle a much 
larger number of clients and handle them more efficiently. 

The pipeline is configured in the McServer class. Handlers live in the 'handler' package.
Key pipeline handlers include:
* IdleStateHandler : handles server and client timeouts
* McTextDecoder : decodes memcache text protocol requests
* McTextEncoder : encodes memcache text protocol responses
* CommandHandler : accepts an inbound command from the decoder and calls ApiCacheCommandExecutor 
to dispatch it to the backing cache.
* InboundErrorHandler : handles exceptions and errors

#### Command Ordering
It is important that commands issued on a single client connection are totally ordered. 

The server does not otherwise guarantee the order of commands issued on separate commands. Though 
generally write commands are executed in arrival order relative to each other, their order is not 
guaranteed relative to read commands issued by different connection.

To satisfy the requirement of per-connection command ordering, and to ensure the netty IO thread is 
not blocked, the CommandHandler executes its commands in a dedicated thread. Currently this happens 
in a new per-request ExecutorService for simplicity. In a production system I would consider a thread pooled 
implementation.

#### Backing Cache
The backing cache can be found in the CacheImpl class. The cache uses a ReadWriteLock to protect a HashMap. 
Where possible, a read lock is used to check pre-conditions prior to entering an expensive write lock. The
CAS unique value is implemented as an AtomicLong.

#### Cache Event Listeners
The backing cache uses a listener architecture to broadcast cache events (hit, miss, put, update, delete, destroy).
The LRU and metrics managers are implemented as cache event listeners. This helps keep the backing cache design
simple and focused on managing the cache itself. In the case of the LRU manager, it also ensures the backing
cache does not need to take out a writelock on reads to update the LRU list.

The BroadcastCacheEventListener receives cache events and re-broadcasts them to a list of listeners. The broadcast
listener is configured in McServerConfig.

#### LRU Manager
The LRU manager listener is implemented in the LRUManagerListener class. This class communicates vi a blocking 
queue to a consumer thread. Executing all LRU transformations on a single thread means we don't need to synchronize
on the LRU data structures, which improves performance. The producer is non-blocking, so that even publishing 
is not blocked from the point of view of the event call coming from the cache. 

The LRU is implemented as a doubly-linked list, where the head is the most recently used item. In order to keep
node lookup O(1), a hash is maintained to point cache keys to list nodes. It was necessary to implement the list 
because the native java list implementations don't provide access to the nodes themselves, only the values in the 
nodes.

The LRU manager maintains the current cache size. When the size exceeds the max configured size, the LRU seeks
to recover a configurable percent of the size by searching backward for the tail for the set of keys whose value
sizes are at least equal to the size to recover. The key list is then sent as destroy advice to the backing cache.
When the backing cache deletes the nodes, the LRU manager will be notified via cache events and it will update
its data structures and size account accordingly.

#### Reaper
Expired cache items are left in the cache until they are cleared by a reaper thread. On retrieval requests, if the 
item is found but it is expired, a cache miss is returned. The reaper runs on a separate thread withing the CacheImpl
class. It is scheduled with a ScheduledExecutorService. Performance can be improved by using less frequent reaper sweeps
as this causes a write lock while each cache item is visited. The cost of less frequent sweeps is more items in the cache.
The reaper frequency can be configured with the reapInterval commandline argument.

#### Monitoring / JMX
Metrics are collected by the metrics collection listener CacheMetricsListener. This class uses atomics types
vs synchronized blocks for better performance as it is not concerned with perfectly maintaining the invariants
between metrics values.

The listener metrics are exposed via JMX. This presents an opportunity to visually inspect realtime metrics, and also
for monitoring. For example, there exists a prometheus exporter for JMX.

#### Logging
The application uses slf4j and logback. The default loglevel is debug. This can be changes in src/main/resources/logback.xml.