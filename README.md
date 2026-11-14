# POS SaaS Backend – Part 1 (Setup + Auth)

Spring Boot **4.1.2** · Java 21 (17+ works) · **MySQL** · Spring Security · JWT (jjwt 0.13, gson)
Extras on the classpath: DevTools (auto restart), Spring Mail, Stripe Java SDK
Multi-tenancy: shared DB, `tenant_id` column.

## Run
1. Start MySQL (database `pos_saas` is created automatically).
2. Set `DB_USERNAME` / `DB_PASSWORD` if not root/root.
3. Set your own `JWT_SECRET` (Base64, 32+ bytes): `openssl rand -base64 32`
4. `mvn spring-boot:run` -> http://localhost:5000

If you created the `users` table before `ROLE_USER` was added, drop the
`pos_saas` database once and let it be re-created.

## Endpoints
| Method | Path | Access |
|---|---|---|
| POST | /auth/signup | public (creates a store + its STORE_ADMIN) |
| POST | /auth/login | public |
| GET | /auth/me | logged-in user |

## Try it it now
```bash
curl -X POST localhost:5000/auth/signup -H "Content-Type: application/json" \
  -d '{"fullName":"Ali Khan","email":"ali@shop.com","password":"secret123","phone":"0170000","storeName":"Ali Mart"}'

curl -X POST localhost:5000/auth/login -H "Content-Type: application/json" \
  -d '{"email":"ali@shop.com","password":"secret123"}'

curl localhost:5000/auth/me -H "Authorization: Bearer <jwt>"
```

## Roles
ROLE_USER, ROLE_SUPER_ADMIN, ROLE_STORE_ADMIN, ROLE_BRANCH_MANAGER, ROLE_CASHIER

## Added later
- Mail: set `spring.mail.*` in application.yml and add the mail service.
- Stripe: set `STRIPE_SECRET_KEY` and add the payment service/controller.
