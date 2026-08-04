# Gestion de Bibliothèque (Biblio-Java)

Application de bureau en **Java / Swing** pour gérer une bibliothèque : livres, emprunts, réservations et utilisateurs, avec persistance **PostgreSQL**.

## Téléchargement

- [ZIP Windows 64 bits](dist/Biblio-Java-Windows-x64.zip)
- [EXE Windows 64 bits](dist/jpackage/Biblio-Java-Windows-x64/Biblio-Java-Windows-x64.exe)

## Installation Windows (mode utilisateur)

1. Téléchargez le ZIP ou l'EXE.
2. Installez l'application dans le dossier utilisateur : `%LOCALAPPDATA%\Programs\Biblio-Java`.
3. Le fichier de configuration DB doit être placé ici : `%LOCALAPPDATA%\Biblio-Java\database.url`.
4. Lancez l'application via `Biblio-Java-Windows-x64.exe`.

Astuce : si vous partez du code source, exécutez `install-user.bat` pour copier automatiquement l'application dans le dossier utilisateur et créer la configuration DB.

## Configuration de la base de données

L'application lit l'URL de connexion dans cet ordre :

1. variable d'environnement `DATABASE_URL`
2. fichier `%LOCALAPPDATA%\Biblio-Java\database.url`

Format accepté :

```text
postgresql://<user>:<password>@<host>/<db>?sslmode=require
```

Ou bien une URL JDBC complète :

```text
jdbc:postgresql://<host>/<db>?user=<user>&password=<password>&sslmode=require
```

## Mise en place de la base

1. Créez une base PostgreSQL sur Neon ou sur votre serveur.
2. Copiez l'URL de connexion dans `database.url`.
3. Démarrez l'application.
4. Les tables sont créées automatiquement au premier lancement.
5. Les données de démonstration sont ajoutées si la base est vide.

Exemple de fichier `database.url` :

```text
postgresql://utilisateur:motdepasse@host/neondb?sslmode=require
```

## Comptes de démonstration

| Rôle | Identifiant | Mot de passe |
| --- | --- | --- |
| Admin | `admin` | `admin123` |
| Lecteur | `lecteur` | `lecteur123` |

## Fonctionnalités

- **Tableau de bord** : statistiques, raccourcis et vue d'ensemble.
- **Catalogue** : recherche par titre, auteur, genre et gestion des livres.
- **Emprunts** : suivi des prêts et création d'un nouvel emprunt.
- **Réservations** : suivi des réservations et création d'une réservation.
- **Utilisateurs** : liste des comptes et rôles.

## Captures d'écran

### Connexion

![Connexion](screenshots/01-login.png)

Interface de connexion sécurisée avec branding institutionnel FST Settat et accès par rôle (Admin / Lecteur).

### Tableau de bord

![Tableau de bord](screenshots/02-dashboard.png)

Vue d'ensemble de la bibliothèque avec les indicateurs clés, les raccourcis et les actions rapides.

### Catalogue

![Catalogue](screenshots/03-catalogue.png)

Liste des livres en cartes avec recherche par titre, auteur ou genre, filtrage par catégorie et actions de gestion.

### Emprunts

![Emprunts](screenshots/04-emprunts.png)

Tableau de suivi des emprunts avec l'état de chaque prêt, les dates importantes et la création d'un nouvel emprunt.

### Réservations

![Réservations](screenshots/05-reservations.png)

Vue centralisée des réservations pour consulter l'historique et enregistrer une nouvelle réservation.

### Utilisateurs

![Utilisateurs](screenshots/06-utilisateurs.png)

Liste des membres de la bibliothèque avec le rôle, les coordonnées et la consultation rapide des comptes.

## Compilation depuis le code source

```bash
javac -encoding UTF-8 -cp "lib/postgresql-42.7.4.jar;lib/flatlaf-3.5.4.jar" -d out src/*.java src/gui/*.java
java -cp "out;lib/postgresql-42.7.4.jar;lib/flatlaf-3.5.4.jar" GUI_Main
```
