# Travailler en équipe sur Dolce Candela

## Branches (exigence E01)

| Branche | Rôle | Qui écrit dessus ? |
|---|---|---|
| `main` | Version stable, déployable (démo, tags `v0.1.0`…) | Personne directement : uniquement par Pull Request depuis `develop` |
| `develop` | Intégration de l'équipe | Uniquement par Pull Request depuis une branche `feature/*` |
| `feature/<sujet>` | Une tâche = une branche (ex. `feature/jenkins-sonar`, `feature/front-panier`) | Le ou la membre qui fait la tâche |
| `fix/<sujet>` | Correction de bug | Idem |

## Cycle de travail

```bash
git checkout develop
git pull                                   # récupérer le travail des autres
git checkout -b feature/ma-tache           # nouvelle branche
# ... coder, tester en local ...
git add <fichiers>
git commit -m "feat(ci): ajoute le stage SonarQube"
git push -u origin feature/ma-tache
```
Puis, sur GitHub : **Pull Request `feature/ma-tache` → `develop`**.
- Jenkins lance le pipeline sur la branche : la PR ne se fusionne que si le pipeline est **vert**.
- Au moins **1 relecture** d'un autre membre avant la fusion.
- Après la fusion, supprimer la branche feature.

## Messages de commit

Format court et lisible : `type(périmètre): description`
- `feat` : nouvelle fonctionnalité · `fix` : correction · `test` : tests · `ci` : Jenkins/pipeline
- `docs` : documentation · `refactor` : réorganisation sans changement de comportement

## Règles de sécurité

- Ne **jamais** committer `.env`, de mot de passe, de token ou de clé (le `.gitignore` protège déjà `.env`).
- Les secrets de la CI vont dans les **Credentials Jenkins**, ceux du déploiement dans des **Secrets Kubernetes**.
- Avant chaque commit : `git status` pour vérifier ce qui part.
