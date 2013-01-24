
## Build
Execute the following command
>     mvn clean assembly:assembly

to build and generate a single jar of this application in ``target/`` folder.

## Run

* Windows (32 bits only) :
>     java -jar sync-<version>-win32-x86.jar

* Mac OS X (64 bits only) :
>     java -jar -XstartOnFirstThread sync-<version>-macosx-cocoa.jar

* Linux :
>     java -jar sync-<version>-linux-gtk-<arch>.jar