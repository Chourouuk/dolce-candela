# Dolce Candela – Backend (API REST)

API e-commerce de **Dolce Candela**, bougies artisanales à Monastir.
Stack : **Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Flyway · JWT · Docker**.
Le front Angular (FR / AR / EN) viendra se brancher dessus.

---

## 1. Lancer le projet

### Avec Docker (recommandé)
```bash
cp .env.example .env        # puis remplacer toutes les valeurs "change-me"
docker compose up --build
```
- API : http://localhost:8081
- Documentation Swagger : http://localhost:8081/swagger-ui.html
- Santé : http://localhost:8081/actuator/health

### Sans Docker (développement)
PostgreSQL doit tourner en local, avec une base `dolce_candela`.
```bash
export DB_PASSWORD=... JWT_SECRET=$(openssl rand -base64 48)
export ADMIN_EMAIL=admin@dolcecandela.tn ADMIN_PASSWORD=MotDePasseAdminSolide1
mvnw.cmd spring-boot:run
```

### Tests
```bash
mvnw.cmd test                 # tests unitaires (*Test.java)      → target/surefire-reports
mvnw.cmd verify               # + tests d'intégration (*IT.java) → target/failsafe-reports + couverture JaCoCo
```
(Le Maven Wrapper `mvnw` s'ajoute une fois : voir `docs/J1-guide.md`, étape 1.)

### Travail en équipe et CI
- Stratégie de branches et règles : [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Mise en place Git + Jenkins + webhook (jalon J1) : [`docs/J1-guide.md`](docs/J1-guide.md)

---

## 2. Base de données

Le schéma est **uniquement** géré par Flyway (`src/main/resources/db/migration`) :
- `V1__init_schema.sql` crée les tables, les contraintes et les index ;
- `V2__seed_catalog.sql` ajoute les 4 catégories, 7 produits d'exemple, les couleurs et les parfums.

| Table | Rôle |
|---|---|
| `users` | Clients et administrateurs (mot de passe haché en BCrypt) |
| `categories` | Bouquets, bougies décoratives, plateaux, diffuseurs (noms FR/AR/EN) |
| `products` | Prix, stock, « fait sur commande », personnalisable, badge |
| `product_images` | Photos des produits |
| `customization_options` | Couleurs, parfums, tailles, emballages (+ supplément éventuel) |
| `product_customization_options` | Options autorisées pour chaque produit |
| `orders` | Commande : client, statut, totaux, acompte |
| `order_items` / `order_item_customizations` | Lignes et options choisies (copie figée des prix) |
| `payments` | Acomptes et soldes |
| `deliveries` | Livraison (adresse, frais) ou retrait (centre-ville de Monastir) |
| `reviews` | Avis clients (achat vérifié + modération) |

Pour ajouter une évolution, créez `V3__description.sql`. Ne modifiez jamais une migration déjà appliquée.

---

## 3. Règles métier

| Règle | Où |
|---|---|
| Livraison **8 DT**, retrait gratuit | `app.business.delivery-fee` |
| **Acompte de 30 %** uniquement pour les grandes quantités (≥ 10 articles au total) | `app.business.deposit-percent`, `deposit-min-quantity` |
| Une commande ne passe en `CONFIRMED` que si l'acompte est payé | `OrderService.updateStatus` |
| Stock décrémenté à la commande, puis remis en stock en cas d'annulation (sauf produits « faits sur commande ») | `Product.decreaseStock` |
| Commande sur mesure : prix fixé par l'admin (devis), puis calcul de l'acompte | `PATCH /api/admin/orders/{id}/quote` |
| Cycle : `PENDING → CONFIRMED → IN_PREPARATION → SHIPPED / READY_FOR_PICKUP → DELIVERED` (+ `CANCELLED`) | `OrderStatus` |
| Tous les prix sont recalculés côté serveur | `PricingService` |

---

## 4. Principales routes

**Public**
- `POST /api/auth/register`, `POST /api/auth/login`
- `GET /api/categories`
- `GET /api/products?category=&badge=&q=&inStock=&page=&size=&sort=price,asc`
- `GET /api/products/{slug}`, `GET /api/products/{slug}/reviews`
- `GET /api/customization-options?type=COLOR`
- `POST /api/orders` (panier, invité possible), `POST /api/orders/custom` (formulaire Custom Order)
- `GET /api/orders/track?reference=DC-261113-AB12CD&phone=22333444`

**Client connecté** (`Authorization: Bearer <jwt>`)
- `GET /api/auth/me`, `GET /api/orders/me`, `GET /api/orders/me/{ref}`, `POST /api/orders/me/{ref}/cancel`
- `POST /api/uploads/reference-image` (photo de référence)
- `POST /api/products/{slug}/reviews`

**Administrateur**
- `GET /api/admin/dashboard`
- `GET|POST|PUT|DELETE /api/admin/products…`, `PATCH /api/admin/products/{id}/stock`, `POST /api/admin/products/{id}/images`
- `POST|PUT /api/admin/categories…`, `POST|PUT /api/admin/customization-options…`
- `GET /api/admin/orders?status=`, `PATCH /api/admin/orders/{id}/status`, `/quote`, `/delivery`, `POST /api/admin/orders/{id}/payments`
- `GET /api/admin/reviews/pending`, `PATCH /api/admin/reviews/{id}/approve`, `DELETE /api/admin/reviews/{id}`

Exemple de commande :
```json
POST /api/orders
{
  "customerName": "Amira Ben Salah",
  "customerPhone": "+216 22 333 444",
  "items": [{ "productId": 1, "quantity": 1, "optionIds": [1, 11], "customText": "Emma & Tom" }],
  "delivery": { "method": "DELIVERY", "addressLine": "Rue de la Corniche", "city": "Monastir" }
}
```

---

## 5. Sécurité déjà en place (base DevSecOps)

- **Aucun secret dans le code** : JWT, mots de passe de la base et de l'admin passent par des variables d'environnement. L'application refuse de démarrer si `JWT_SECRET` manque ou fait moins de 32 caractères.
- Mots de passe hachés en **BCrypt (coût 12)**, politique de mot de passe minimale.
- **Anti brute-force** : blocage de 15 min après 5 échecs de connexion. Temps de réponse identique, que l'email existe ou non.
- **JWT** signé HS256 avec émetteur vérifié. Le compte est relu en base à chaque requête, donc un compte désactivé perd l'accès immédiatement.
- Rôles `CUSTOMER` / `ADMIN` ; tout `/api/admin/**` est réservé aux administrateurs.
- **Validation** de toutes les entrées (Bean Validation) et contraintes `CHECK` en base.
- **Uploads** : type réel vérifié via les octets d'en-tête (PNG/JPG/WebP), 10 Mo max, nom aléatoire, aucune traversée de répertoire possible.
- Erreurs au format JSON `ProblemDetail`, **sans trace technique**.
- Verrouillage optimiste sur le stock, pour éviter de vendre deux fois la dernière pièce.
- Suivi de commande public protégé par référence aléatoire **et** téléphone. Aucune donnée personnelle renvoyée.
- Docker : image JRE Alpine, **utilisateur non-root**, système de fichiers en lecture seule, `no-new-privileges`, healthcheck. La base n'est exposée que sur `127.0.0.1`.
- CORS limité aux origines déclarées ; Actuator n'expose que `health` et `info`.

---

## 6. Prochaine étape : pipeline CI/CD DevSecOps

Le projet est prêt à être branché sur Jenkins ou GitHub Actions. Étapes recommandées :

1. **Secrets** : gitleaks (aucun secret dans le dépôt)
2. **Build + tests** : `mvn verify` (génère `target/site/jacoco/jacoco.xml`)
3. **SAST + qualité** : SonarQube (`mvn sonar:sonar`, la couverture JaCoCo est déjà configurée)
4. **SCA** : OWASP Dependency-Check ou Trivy `fs` sur les dépendances Maven
5. **Image** : `docker build`, puis scan Trivy de l'image (blocage si vulnérabilité CRITICAL)
6. **Déploiement** : push vers un registre, puis `docker compose` sur un VPS ou un cloud
7. **DAST** : OWASP ZAP baseline sur l'API déployée (Swagger fournit la définition OpenAPI)
