# MC-Chunker

Mc-Chunker is an application that uses a vanilla [Minecraft](https://www.minecraft.net/) Java Server to generate / pre-generate chunks in a configurable area.

### Features
- Generate any amount of chunks
- Generate chunks in the Overworld, Nether or the End

### Usage
Place the mc-chunker.jar in the directory of a Java Minecraft server (both jars must be in the directory). Start mc-chunker via a terminal/command line like:

`java -jar mc-chunker.jar server.jar`

The application will run and stop, prompting you to fill in the configuration. Edit the
generated `chunker.properties` and start the application again. The application
will run, start the Minecraft server, generate the configured chunks and it will stop,
leaving the server running. You can interact with the server in the normal way.

To start mc-chunker you can start it the same way you are starting the Minecraft server.
If you have a script or command like:

`java -Xms4G -Xmx4G -jar server.jar nogui`

You can modify it by inserting the name of the `mc-chunker.jar` like:

`java -Xms4G -Xmx4G -jar mc-chunker.jar server.jar nogui`

All JVM arguments will apply to the started server. All program arguments
except the name of the server jar will be given to the Minecraft server.

Once you are done using mc-chunker you should remove it from your starting script.
Continuing to use the server with mc-chunker is discouraged.

### Configuration
To configure mc-chunker edit the chunker.properties file. Some of the values are:
- x1, z1, x2 and z2 - these specify **Chunk** coordinates of two diagonal corners
of a rectangular area to generate chunks in. These are inclusive
- dimension - in which Minecraft dimension(s) to generate the chunks - possible
values: OVERWORLD, NETHER or END. The default value is OVERWORLD. Comma separated list of values
- stop - Whether to stop the Minecraft server when the chunk generation is
done - possible values: true or false
- suppress-server-output - If set to true, console output from the server will not
be shown. Log output will remain the same. Possible values: true or false 

### Requirements
- Java 8 or newer
- A Minecraft vanilla server
- Internet connection to retrieve Minecraft obfuscation mappings (planned to be optional)

### Known issues

- If the Minecraft server fails to start MC-Chunker will hang/crash. This includes the
server not starting because you have not agreed to the EULA yet.

- MC-Chunker is not compatible with older versions

### Minecraft version compatability
Currently only Snapshot 20w22a through 1.16.2_rc2 have been tested (not exhaustively). Other
snapshots might work, but they have not been tested yet.

### Planned features

- Compatibility with more Minecraft versions. This may include older versions depending
on how much work is required for compatibility, but there are no plans for compatibility
with versions older than Snapshot 19w36a (the first to include obfuscation mappings; this is
a snapshot for 1.15) and including 1.14.4
- ~~Option to stop server when chunk generation is done~~
- ~~Properly handle server not starting~~
- Throttling of the generation speed
- Ability to stop the server, save generation progress, start and resume the chunk generation
from where it reached
- Option to provide mappings as a file and enter the filename in chunker.properties
- Option to use excessive amounts of CPU in order to speed up generation

### How it works
Mc-chunker loads the server, makes a small modification (explained bellow) and starts the server,
waits for the server to fully start and then starts telling the server to generate (and
temporarily load) chunks, one at time. That is all.

The modification mc-chunker makes to the server is required in order to get a reference to a
local variable from outside the scope. No (other) behaviour is changed. The server jar in the
file system is not modified. When you are done using mc-chunker and start the server the normal
way it will be 100% vanilla.

The rate at which chunks are generated is the rate at which Minecraft can generate them (unless
there is a better way to do so than the one used). A reference speed is 20 chunks per second, but
this will be affected by the speed of your system.

### License

MC-Chunker is released under the [Apache 2.0 license](LICENSE).

```
Copyright 2020 Ivan Zhivkov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

### Disclaimer

Technically, running a Minecraft server with mc-chunker can be classified as a modded server, even
if it behaves exactly like a vanilla server.

Minecraft content and materials are the intellectual property of their respective owners.
