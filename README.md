# Jaja Shop

Boutique en ligne avec tableau de bord administrateur. Paiement par Wave (XOF).
Projet de démonstration : API Spring Boot, puis frontend React.

## Statut

Backend en construction. Le frontend démarre après la fin du backend.

## Prérequis

- Java 21
- Git
- Docker Desktop

## Structure du dépôt

```
jaja-shop/
├── backend/             API Spring Boot (Maven)
├── docker/              scripts d'initialisation de la base
├── docker-compose.yml   PostgreSQL pour le développement
├── .env.example         modèle des variables d'environnement
├── frontend/            Application React (à venir)
└── README.md
```

## Base de données (PostgreSQL via Docker)

```bash
cp .env.example .env        # puis éditer .env et choisir un mot de passe
docker compose up -d        # démarre PostgreSQL
docker compose ps           # l'état doit être "healthy"
docker compose down         # arrête (les données sont conservées)
docker compose down -v      # arrête ET supprime les données
```

Le fichier `.env` n'est jamais versionné. Deux bases sont créées : `jajashop`
(développement) et `jajashop_test` (tests).

Le script de création de `jajashop_test` ne s'exécute qu'à la première création
du volume. Si la base de test manque : `docker compose down -v` puis `docker compose up -d`.

## Profils Spring

| Profil | Usage | Configuration |
|---|---|---|
| dev (par défaut) | développement local | valeurs par défaut + fichier `.env` |
| test | tests automatiques | base `jajashop_test`, séparée de dev |
| prod | production | variables d'environnement obligatoires, aucun secret dans le code |

En profil `prod`, l'application refuse de démarrer si une variable `DB_HOST`,
`DB_PORT`, `DB_NAME`, `DB_USER` ou `DB_PASSWORD` est absente.

Lancer avec un autre profil : `SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run`

## Lancer le backend

```bash
docker compose up -d        # depuis la racine
cd backend
./mvnw spring-boot:run      # PowerShell : .\mvnw.cmd spring-boot:run
```

Vérification : `curl http://localhost:8081/api/health`

Réponse attendue :

```json
{"status":"UP","service":"jaja-shop-api","timestamp":"..."}
```

## Lancer les tests

PostgreSQL doit être démarré.

```bash
cd backend
./mvnw test
```
