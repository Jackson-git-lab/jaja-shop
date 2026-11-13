# Jaja Shop

[![Backend CI](https://github.com/Jackson-git-lab/jaja-shop/actions/workflows/backend-ci.yml/badge.svg?branch=main)](https://github.com/Jackson-git-lab/jaja-shop/actions/workflows/backend-ci.yml)

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

## Migrations de base de données (Flyway)

Le schéma de la base est géré uniquement par des scripts SQL versionnés, dans
`backend/src/main/resources/db/migration/`. Flyway les applique au démarrage de l'application.

- Nommage : `V<numéro>__<description>.sql` (deux underscores), par exemple `V2__create_users.sql`
- Une migration déjà appliquée ne se modifie jamais : pour corriger, on ajoute une nouvelle migration
- Hibernate est configuré en `ddl-auto: validate` : il vérifie que les entités correspondent
  au schéma, mais ne le crée ni ne le modifie
- Historique des migrations appliquées : table `flyway_schema_history`

L'application a besoin que PostgreSQL soit démarré (`docker compose up -d`) : Flyway se
connecte à la base dès le démarrage, y compris en profil `prod`.


## Format des erreurs de l'API

Toutes les erreurs ont la même forme JSON, produite par `GlobalExceptionHandler`
(`@RestControllerAdvice`) :

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Produit introuvable (identifiant : 42).",
  "details": [],
  "timestamp": "2026-11-11T10:15:30.123Z"
}
```

| Statut | Code | Cas |
|---|---|---|
| 400 | VALIDATION_ERROR | champs invalides (`details` : une ligne par champ) |
| 400 | MALFORMED_REQUEST | JSON illisible |
| 400 | INVALID_PARAMETER | paramètre d'URL invalide ou manquant |
| 400 | BAD_REQUEST | demande invalide pour une raison métier |
| 401 | UNAUTHORIZED | non authentifié ou identifiants invalides |
| 403 | FORBIDDEN | connecté mais non autorisé |
| 404 | RESOURCE_NOT_FOUND / ROUTE_NOT_FOUND | ressource ou route inexistante |
| 405 | METHOD_NOT_ALLOWED | mauvaise méthode HTTP |
| 409 | CONFLICT | conflit avec l'état actuel (doublon, stock insuffisant) |
| 415 | UNSUPPORTED_MEDIA_TYPE | type de contenu non supporté |
| 500 | INTERNAL_ERROR | erreur inattendue (aucun détail technique renvoyé) |

Dans le code, on lance une exception métier (`ResourceNotFoundException`,
`ConflictException`, `UnauthorizedException`, `ForbiddenException`, `BadRequestException`)
et le handler global s'occupe de la réponse.


## Documentation de l'API (Swagger / OpenAPI)

La documentation est générée automatiquement à partir du code (springdoc-openapi).
Elle est **active en dev et en test, désactivée en prod**.

| Ressource | URL (dev) |
|---|---|
| Interface Swagger UI | http://localhost:8081/swagger-ui.html |
| Spécification OpenAPI (JSON) | http://localhost:8081/v3/api-docs |

Authentification : le schéma `bearerAuth` (HTTP Bearer, JWT) est déclaré dans `OpenApiConfig`.
Les contrôleurs protégés l'activent avec `@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)`.
Dans Swagger UI, bouton **Authorize**, puis coller le token JWT (sans le mot `Bearer`).

Chaque ticket qui ajoute une route doit la documenter (`@Tag` sur le contrôleur,
`@Operation` sur chaque méthode).



## Intégration continue (CI)

À chaque push, GitHub Actions (`.github/workflows/backend-ci.yml`) :

1. récupère le code ;
2. installe Java 21 (avec cache Maven) ;
3. démarre un PostgreSQL temporaire (base `jajashop_test`) ;
4. lance `./mvnw verify` : compilation, tests, construction du jar.

En cas d'échec, les rapports de tests sont conservés comme artefact téléchargeable.
Le badge en haut de ce fichier indique l'état de la branche `main`.

Reproduire la CI en local :

```bash
docker compose up -d
cd backend
./mvnw -B -ntp verify
```