# CAFAT — cas pratique, code initial

Ce projet reproduit le code fourni dans l'énoncé avec les classes nécessaires à son exécution. Il conserve volontairement ses problèmes : cette version constitue le point de départ de l'exercice, pas une solution corrigée.

## Lancement

Prérequis : Java 21, Maven, Docker Desktop. Dans le dossier du projet :

```powershell
docker compose up -d
mvn clean spring-boot:run
```

Si `docker` n'est pas disponible dans le terminal, lancer les conteneurs depuis Docker Desktop. Sur ce poste, PostgreSQL est accessible sur le port **5433**, Artemis sur **61616**. La base `cafat_initial` est dédiée à cette version. Le compte et le mot de passe PostgreSQL sont `cafat` / `cafat` pour cette démonstration locale. Les variables `DB_URL`, `DB_USER` et `DB_PASSWORD` permettent d'adapter la connexion.

Sur un nouveau volume Docker, la base `cafat` est créée par Compose, mais la base `cafat_initial` doit être créée une fois :

```powershell
docker compose exec postgres createdb -U cafat cafat_initial
```

Au premier démarrage de l'application, Hibernate crée les tables et un compte fictif 1001 avec un solde nul. La base `cafat_initial` est distincte de la base `cafat` précédemment créée.

Les requêtes de démonstration sont disponibles dans [requests.http](requests.http). Le barème de 20 % est fictif. `mvn test` lance les tests ; ils ne démontrent pas que les problèmes du code initial sont corrigés.
