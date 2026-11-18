# Jalon J1 — Mise en place de la CI

Objectif J1 du cahier des charges : dépôt Git, Jenkinsfile, webhook, build, tests unitaires **et** tests d'intégration automatisés.

Ce qui est déjà dans le projet :
- `Jenkinsfile` : Checkout → Build → Tests unitaires → Tests d'intégration → Package ;
- tests unitaires (`*Test.java`, Surefire → `target/surefire-reports`) et tests d'intégration (`*IT.java`, Failsafe → `target/failsafe-reports`), avec des **rapports séparés** ;
- version `0.1.0-SNAPSHOT` (prête pour Nexus en J2) ;
- `.gitattributes` (fins de ligne Windows/Linux) et `CONTRIBUTING.md` (stratégie de branches).

---

## Étape 1 — Ajouter le Maven Wrapper (une seule fois, par une seule personne)

Le wrapper (`mvnw` / `mvnw.cmd`) permet de builder **sans installer Maven**, sur vos PC comme dans Jenkins (exigence E03).
À la racine du projet, avec Docker Desktop démarré :

```cmd
docker run --rm -v "%cd%":/app -w /app maven:3.9-eclipse-temurin-21 mvn -N wrapper:wrapper
```
Cela crée `mvnw`, `mvnw.cmd` et `.mvn/wrapper/maven-wrapper.properties`. Pour vérifier :
```cmd
mvnw.cmd -v
mvnw.cmd verify
```
(Il faut un JDK 21 installé et `JAVA_HOME` défini. Sinon, lancez les tests via Docker comme ci-dessus en remplaçant `-N wrapper:wrapper` par `verify`.)

## Étape 2 — Créer le dépôt GitHub et le partager

1. Sur GitHub : **New repository** → `dolce-candela` → **Private** → sans README (il existe déjà).
2. Dans le dossier du projet :
   ```cmd
   git init
   git add .
   git status
   ```
   ⚠️ Vérifiez que **`.env` n'apparaît pas** dans la liste. S'il apparaît, ne continuez pas.
   ```cmd
   git commit -m "feat: backend Dolce Candela (API, BD, Docker, Jenkinsfile J1)"
   git branch -M main
   git remote add origin https://github.com/<votre-compte>/dolce-candela.git
   git push -u origin main
   git checkout -b develop
   git push -u origin develop
   ```
3. **Settings → Collaborators** : invitez les membres de l'équipe.
4. **Settings → Branches → Add rule** pour `main` et `develop` : *Require a pull request before merging* (1 approbation) et, après la première exécution Jenkins, *Require status checks to pass*.

Chaque membre récupère ensuite le projet :
```cmd
git clone https://github.com/<votre-compte>/dolce-candela.git
cd dolce-candela
copy .env.example .env
```

## Étape 3 — Jenkins

Si Jenkins tourne sur un PC où le port 8080 est déjà pris (Oracle XE par exemple), lancez-le dans Docker sur un autre port. L'image `lts-jdk21` contient déjà Java 21 :
```cmd
docker run -d --name jenkins -p 8090:8080 -p 50000:50000 -v jenkins_home:/var/jenkins_home jenkins/jenkins:lts-jdk21
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
Puis http://localhost:8090 → plugins suggérés (+ **GitHub**, **Pipeline**, **JUnit** s'ils manquent).

1. **Credentials** : *Administrer Jenkins → Credentials → Global → Add* → *Username with password* :
   votre identifiant GitHub + un **Personal Access Token** (GitHub → Settings → Developer settings → Tokens, droits `repo` et `admin:repo_hook`). ID : `github-credentials`.
2. **Nouveau job** → *Multibranch Pipeline* → `dolce-candela` :
   - Branch Sources → GitHub → credentials `github-credentials` → URL du dépôt ;
   - Build Configuration → *by Jenkinsfile* ;
   - Enregistrer : Jenkins scanne les branches et lance un build pour `main` et `develop`.

## Étape 4 — Webhook GitHub → Jenkins

GitHub doit pouvoir joindre votre Jenkins. En local, il faut un tunnel, par exemple ngrok :
```cmd
ngrok http 8090
```
Copiez l'URL `https://xxxx.ngrok-free.app`, puis sur GitHub : **Settings → Webhooks → Add webhook**
- Payload URL : `https://xxxx.ngrok-free.app/github-webhook/` (le `/` final est obligatoire)
- Content type : `application/json`
- Events : *Pushes* et *Pull requests*

Test : faites un petit commit sur une branche `feature/test-webhook` → un build doit démarrer tout seul dans Jenkins.

## Étape 5 — Preuves pour le jalon J1

| Preuve | Comment l'obtenir |
|---|---|
| Pipeline déclenché par webhook | Capture de l'onglet *Recent Deliveries* du webhook (code 200) + build « Started by GitHub push » |
| Stages explicites | Capture de la vue *Stage View* |
| Rapports de tests séparés | Onglet *Test Result* du build : tests unitaires et d'intégration |
| **Échec contrôlé** (obligatoire) | Sur une branche `feature/demo-echec`, changez une valeur attendue dans `PricingServiceTest` (ex. `"92.000"` → `"93.000"`), poussez : le stage *Tests unitaires* devient rouge et les stages suivants ne s'exécutent pas. Faites la capture, puis supprimez la branche. |
| Historique Git | Liste des Pull Requests fusionnées et graphe des contributeurs |
