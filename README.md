Étapes de Création de la Version RMI avec Registre La version RMI a été développée selon une architecture client-serveur classique utilisant 
le registre RMI intégré à Java :

Définition des interfaces distantes : 
Nous avons créé des interfaces Java étendant java.rmi.Remote pour définir les méthodes accessibles à distance, 
garantissant une séparation claire entre contrat et implémentation.

Implémentation serveur : 
Le serveur RMI implémente ces interfaces, gère la persistance des données via SQL Server, 
et exporte les objets pour les rendre accessibles aux clients distants.

Création de module client : 
Après développement, nous avons extrait les classes nécessaires dans un fichier JAR séparé. 
Ce module client a ensuite été ajouté en tant que dépendance dans le projet client, permettant une distribution et versionnage indépendant du serveur.

Enregistrement dans le registre : 
Le serveur publie ses services via le registre RMI standard sur le port 1099, permettant aux clients de découvrir et 
d'accéder aux objets distants via des références URL.

Interface JavaFX unifiée : 
L'interface utilisateur a été développée en JavaFX assurant une expérience utilisateur cohérente entre les deux versions.

Système TCP parallèle : 
Un canal de communication TCP dédié a été implémenté pour les notifications asynchrones, 
fonctionnant indépendamment des appels RMI synchrones.

Extensibilité future : Cette architecture pourra évoluer vers une gestion d'objets connectés 
où chaque équipement de chambre (thermostats, serrures intelligentes, capteurs) serait représenté comme un objet distant, 
permettant un contrôle granulaires, une automatisation poussée et une intégration avec des écosystèmes IoT via le même paradigme RMI.
