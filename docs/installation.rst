============
Installation
============

Prérequis
=========

* Système d'exploitation Windows ou linux Debian et Ubuntu.
* Installer Java dans la version adapter à votre système (En x64 : Java soit être installé en x32 ET en x64)

* Installer les drivers USB des teminaux sur les ordinateurs concernés.

*  Télécharger et installer l'utilitaire de synchronisation : [GeoNature-mobile-sync](/docs/install/) en suivant la documentation dédiée : [GeoNature-mobile-sync/docs/install_conf_sync.odt](GeoNature-mobile-sync/docs/install_conf_sync.odt)

* Configurer [le fichier server.json](/docs/install/server.json) en adaptant les paramètres à votre contexte. Ce fichier se trouve dans ``C://program/PNE sync/`` sous windows ou dans ``/usr/share/sync/`` pour linux.
Dans un contexte multi-organismes, le paramètre ``settings_url`` permet de déclarer un sous répertoire par organisme dans le répertoire ``datas`` de la webapi. GeoNature-mobile-sync trouvera ses settings.json dans ce sous répertoire de la webapi.
Le fichier de settings de GeoNature-mobile-sync est dans le répertoire de l'utilisateur. Il se trouve dans un répertoire caché nommé .sync 
	* ``/home/user/.sync/settings.json`` pour linux
	* ``C:/Utilisateurs/user/.sync/settings.json`` pour windows


======================================
Configurer le ou les terminaux Android
======================================

Prérequis
=========
* Disposer d'un terminal mobile sous Android (4.X , 5.X ou 6.X) ; 7x non testé.
* Disposer de plus de 100 Mo d'espace de stockage sur la mémoire interne du terminal mobile.
* Disposer d'une carte mémoire microSD avec autant d'espace disponible que nécessaire aux fichiers des fonds cartographiques.
* Activer le mode développeur et le debogage USB :

	    Paramètres -> A propos du téléphone : cliquer 6 ou 7 fois sur le numéro de build pour activer le mode développeur
    	Paramètres -> Options pour les développeurs : Activer le debogage USB

* Connecter le terminal à l'ordinateur en tant que périphérique multimédia (MTP).
* Lancer GeoNature-mobile-sync.

ATTENTION sur Android 5 : Il est nécessaire de redémarer le terminal pour que le contenu de la mémoire soit rafraîchi.
Sous Android 5 et 6 lors de la connexion du terminal au PC via USB, une autorisation est demandée par Android. Vous devez accepter cette autorisation à chaque connexion du terminal.

===============================================================
Installation des application Android avec GeoNature-mobile-sync 
===============================================================

Si les applications geonature-moblie ne sont pas installées sur le terminal, geoNature-mobile-sync va les installer et télécharger leur fichiers de configuration ainsi que la base de données data.db. Par contre GeoNature-mobile-sync  ne peut pas écrire sur la carte mémoire externe, il n'installe donc pas les fonds de carte ni le fichier unities.wkt que vous devez copier sur chaque terminal manuellement.

* Une fois l'installation terminée se rendre dans la mémoire de la carte SD de l'appareil puis dans : ``Android/data`` et creer un nouveau répertoire ``com.makina.ecrins``
* Ajouter dans ce dernier un nouveau dossier ``databases``
* Y copier depuis l'ordinateur les 3 fichiers .mbtiles nécessaire au fonctionnement des applications (les rasters : scan, ortho et unities). [Voir la documentation](https://github.com/PnEcrins/GeoNature-mobile/blob/master/docs/install/installation.rst)

* Lancer les applications pour vérifier leur bon fonctionnement et le bon chargement des settings et des fonds rasters à l'étape "la "carte".
