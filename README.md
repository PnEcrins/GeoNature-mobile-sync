
## Prerequisites and requirements

* Oracle Java JDK (6 as minimal version)
* Apache Maven (3.x, latest version)

## Build

Execute the following command to build and generate a single jar of this application in ``target/`` folder :

>     mvn clean assembly:assembly

### Without tests

>     mvn clean assembly:assembly -Dmaven.test.skip=true

### Build setup (Windows platform only)

You'll need to download and install :

* **Launch4j** : [http://sourceforge.net/projects/launch4j/files/latest/download](http://sourceforge.net/projects/launch4j/files/latest/download)
* **Inno Setup** : [http://www.jrsoftware.org/isdl.php#stable](http://www.jrsoftware.org/isdl.php#stable)
* **InnoTools Downloader** : [http://www.sherlocksoftware.org/page.php?id=51](http://www.sherlocksoftware.org/page.php?id=51)

#### Launch4j

Execute *Launch4j* and open configuration file using `setup/sync-x86.xml` or `setup/sync-x86_64.xml` depending on the target architecture.
Click on "Build wrapper" button (button shaped like a wheel gear).
*Launch4j* will produce an executable `sync.exe` in `setup/` folder.

#### Inno Setup Compiler

Execute *Inno Setup Compiler* and open script file `setup/sync-x86.iss` or `setup/sync-x86_64.iss` depending on the target architecture.
Click on "Compile" button or use combination keys ``CTRL+F9`` to perform the final build.
*Inno Setup* will produce the final executable to distribute named `setup_sync-<version>-<arch>.exe` in `setup/Output/` folder.

## Run

### Standalone (from Java)

* Windows :
>     java -jar sync-<version>-win32-<arch>.jar

* Mac OS X (64 bits only) :
>     java -jar -XstartOnFirstThread sync-<version>-macosx-cocoa.jar

* Linux :
>     java -jar sync-<version>-linux-gtk-<arch>.jar