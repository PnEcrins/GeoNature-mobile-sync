# GeoNature-mobile-sync

GeoNature est une application de saisie et de synthèse des observations faune et flore : https://github.com/PnEcrins/GeoNature

Pour pouvoir importer les données saisies avec [Geonature-mobile](https://github.com/PnEcrins/GeoNature-mobile) dans la BDD PostgreSQL de GeoNature, une web-API doit être installée sur le serveur : https://github.com/PnEcrins/GeoNature-mobile-webapi

La synchronisation de ces données peut être faite par le réseau (wifi ou 3G) ou en connectant le mobile en USB à un PC connecté à internet. Dans ce cas, cette application de synchronisation des données doit être installée sur le PC.

![GeoNature schema general](https://github.com/PnEcrins/GeoNature/raw/master/docs/images/schema-geonature-environnement.jpg)

Il est possible d'installer cette application Windows à partir du .EXE disponible dans https://github.com/PnEcrins/GeoNature-mobile-sync/tree/master/docs/install

Documentation d'installation : https://github.com/PnEcrins/GeoNature-mobile-sync/tree/master/docs

# Development

## Prerequisites and requirements
* Oracle Java JDK (6 as minimal version)
* [Apache Maven (3.0.x)](http://maven.apache.org/download.cgi#Maven_3.0.5)

## Build
Execute the following command to build and generate a single jar of this application in ``target/`` folder :

    mvn clean assembly:assembly

### Without tests
    mvn clean assembly:assembly -Dmaven.test.skip=true

### Build setup (Windows platform only)
You'll need to download and install :

* **Inno Setup** : [http://www.jrsoftware.org/isdl.php#stable](http://www.jrsoftware.org/isdl.php#stable)
* **InnoTools Downloader** : [http://www.sherlocksoftware.org/page.php?id=51](http://www.sherlocksoftware.org/page.php?id=51)

#### Inno Setup Compiler
Execute *Inno Setup Compiler* and open script file `setup/sync-x86.iss` or `setup/sync-x86_64.iss` depending on the target architecture.
Click on "Compile" button or use combination keys ``CTRL+F9`` to perform the final build.
*Inno Setup* will produce the final executable to distribute named `setup_sync-<version>-<arch>.exe` in `setup/Output/` folder.

## Run
### Standalone (from Java)
* Windows :

        java -jar sync-<version>-win32-<arch>.jar

* Mac OS X (64 bits only) :

        java -jar -XstartOnFirstThread sync-<version>-macosx-cocoa.jar

* Linux :

        java -jar sync-<version>-linux-gtk-<arch>.jar

## License
&copy; Makina Corpus / Parc national des Ecrins 2012 - 2015