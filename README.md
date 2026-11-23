# CAFAT — cas pratique, code initial

Ce projet démontre le dépôt transactionnel et idempotent des déclarations ainsi que la mise à jour atomique du solde.

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

Les requêtes de démonstration sont disponibles dans [requests.http](requests.http). Le barème de 20 % est fictif. `mvn test` lance les tests unitaires. Les tests PostgreSQL sont activés avec `TEST_DATABASE_URL` : utilisez une base ou un schéma dédié, car ils effacent les tables de déclarations et de comptes. Les variables `DB_USER` et `DB_PASSWORD` configurent ses identifiants.

## Déploiement et concurrence

La référence est unique pour toutes les déclarations. Toute référence déjà enregistrée retourne la déclaration existante sans nouveau mouvement de solde, même si les données envoyées diffèrent. Avant de mettre à jour une base existante, rechercher et résoudre les références en double : Hibernate ne peut pas créer la contrainte tant qu'elles subsistent. Vérifier que la contrainte `uk_declaration_reference` est bien présente avant d'accepter des dépôts.

Tous les mouvements de solde, y compris les futurs paiements (montant négatif), doivent utiliser `CompteRepository.majSolde` dans une transaction. Une écriture du solde par lecture puis remplacement contournerait cette protection. Une autre approche serait un verrou optimiste `@Version`, avec gestion des conflits et nouvelles tentatives.

L'envoi JMS et la transaction PostgreSQL ne sont pas coordonnés par une transaction distribuée : une panne entre l'envoi et le commit peut produire un avis sans déclaration enregistrée. Une boîte d'envoi transactionnelle serait nécessaire pour garantir leur cohérence.

Pour les tests d'intégration, définir par exemple `TEST_DATABASE_URL=jdbc:postgresql://localhost:5433/cafat_test` sur une base de test vide, puis exécuter `mvn clean test`.
Si la contrainte est absente, l'application refuse de démarrer. Pour identifier les références à rapprocher dans PostgreSQL :

```sql
SELECT reference, COUNT(*) AS nombre
FROM declaration
GROUP BY reference
HAVING COUNT(*) > 1;

SELECT id, employeur_id, reference, salaire_brut
FROM declaration
WHERE reference IN (
    SELECT reference FROM declaration GROUP BY reference HAVING COUNT(*) > 1
)
ORDER BY reference, id;
```

Après analyse et correction manuelle des données et des soldes concernés, ajouter la contrainte si elle est absente :

```sql
ALTER TABLE declaration ADD CONSTRAINT uk_declaration_reference UNIQUE (reference);
```

Ne pas supprimer automatiquement les doublons : le solde historique peut également nécessiter un rapprochement. Une nouvelle déclaration doit avoir sa propre référence : une référence existante retourne toujours le dépôt initial.
Dans IntelliJ, renseigner également la variable d'environnement TEST_DATABASE_URL dans la configuration JUnit pour exécuter les tests d'intégration. Sans cette variable, ils sont ignorés afin de protéger les données de la base de l'application.
