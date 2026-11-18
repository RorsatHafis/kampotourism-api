# Kampotourism API

REST API for the Kampotourism tourism website.

- **Visitors**: no accounts, no login. Public endpoints are read-only `GET`s.
- **Editors/admins**: sign in at `/api/v1/auth/login`, then manage content under `/api/v1/admin/**`.

Stack: Java 21, Spring Boot 3.5, PostgreSQL, Flyway, Spring Security (JWT), springdoc-openapi.

## Run it

```bash
docker compose up -d                        # PostgreSQL on :5432

export JWT_SECRET="$(openssl rand -base64 48)"   # required, >= 32 bytes
export ADMIN_EMAIL="you@example.com"             # first admin, created only if no admin exists
export ADMIN_PASSWORD="a-long-password-12+"

mvn spring-boot:run     # or: mvn -N wrapper:wrapper once, then ./mvnw spring-boot:run
```

- Swagger UI: http://localhost:8080/docs (use *Authorize* with the token from login)
- Tests need Docker (Testcontainers): `mvn verify`

## Routes

| Method | Path | Auth |
|---|---|---|
| POST | `/api/v1/auth/login` | public |
| POST | `/api/v1/auth/logout` | token (revokes it) |
| GET | `/api/v1/auth/me` | token |
| GET | `/api/v1/destinations` · `/{slug}` | public (`category`, `featured`, `page`, `size`, `lang`) |
| GET | `/api/v1/experiences`, `/accommodations`, `/transportation`, `/articles`, `/travel-information` (+ `/{slug}`) | public (`category`, `featured`, `q`, `page`, `size`, `lang`) |
| GET | `/api/v1/itineraries` · `/{slug}` | public (`featured`, `page`, `size`, `lang`) |
| GET | `/api/v1/homepage` | public (`lang`); returns configuration and featured content in one response |
| GET | `/api/v1/categories?kind=` | public (`destination`, `experience`, `stay-type`, `transport`, `blog`) |
| GET/POST | `/api/v1/admin/destinations` | token |
| GET/PUT/DELETE | `/api/v1/admin/destinations/{id}` | token |
| PUT/DELETE | `/api/v1/admin/destinations/{id}/translations/{lang}` | token |
| POST | `/api/v1/admin/destinations/{id}/publish` · `/unpublish` | token |
| PUT | `/api/v1/admin/destinations/{id}/images` | token (`{"cover":12,"gallery":[15,14]}`) |
| CRUD + lifecycle | `/api/v1/admin/experiences`, `/accommodations`, `/transportation`, `/articles`, `/travel-information` | token; translations, images, publish/unpublish/archive are available per item |
| CRUD + lifecycle | `/api/v1/admin/itineraries` | token; `PUT /{id}/days` replaces ordered days/stops, where a stop references one destination or experience |
| GET/PUT | `/api/v1/admin/settings/homepage` | ADMIN only; structured introduction/sections/navigation/footer/social/contact configuration |
| GET/POST/PUT/DELETE | `/api/v1/admin/categories[/{id}]` (+ `/translations/{lang}`) | token |
| POST/GET | `/api/v1/admin/media` | token (multipart: `file`, optional `alt`, `caption`) |
| GET/PUT/DELETE | `/api/v1/admin/media/{id}` | token (`DELETE ?force=true` detaches everywhere) |
| POST | `/api/v1/admin/media/{id}/file` | token (replace the file, keep id and texts) |
| GET | `/media/**` | public (uploaded files) |

## How things work

- **Language**: `?lang=km` wins, then `Accept-Language`, then `en`. Each record falls back
  to English if the translation is missing; the language served is in `language` and `Content-Language`.
- **Images**: an asset is a file plus per-language alt text and caption. Alt text in the
  default language is required before an image can be attached to content. The same
  attachment mechanism will serve hotels (galleries), blogs (covers) and the rest.
  Public responses expose `cover` and `gallery[]` with `url`, `alt`, `caption`, `width`, `height`.
- **Lifecycle**: new editorial content starts as `DRAFT`; public endpoints return only `PUBLISHED`. `ARCHIVED` is retained for audit/history and is never public. Publishing records `publishedAt`.
- **Rich content**: article bodies, travel-information sections, and other long-form text are validated JSON values (normally an array of frontend-defined content blocks), rather than server-rendered HTML. This keeps rendering safe and lets the frontend evolve its block vocabulary.
- **Concurrent edits**: updates send the `version` they were based on; a stale one gets 409.
- **Deleting**: a published destination must be unpublished first; an image or category
  that is still in use returns 409.
- **Errors**: RFC 9457 `application/problem+json`; validation errors include an `errors` object per field.
- **Caching**: public GETs carry `Cache-Control` + ETag, so edits can take up to 5 minutes to appear.

## Security notes

- Tokens expire after 2 hours (`app.security.access-token-ttl`); logout revokes a token immediately.
- Five failed logins from one IP for one email locks that pair for 15 minutes (in memory, one instance).
  Behind a reverse proxy, enable `server.forward-headers-strategy: framework` so the real IP is used.
- Set `app.cors.allowed-origins` to your real frontend and admin origins in production.
- Editor text is plain text; the frontend must never render it as HTML.
- Schema changes only through new Flyway migrations (`V5__...`); never edit applied ones.
- `destination.image_url` is a leftover from the demo seed (external hotlinks). It is used only
  when no cover image is attached; remove the column once real images are uploaded.
- Images are inspected by signature and dimensions. JPEG, PNG, and WebP are accepted; uploads above the configured multipart limit, malformed images, images over 12,000px on either axis, or images over 40 megapixels are rejected. Filesystem keys are generated server-side and never exposed.

## Example content workflow

1. `POST /api/v1/auth/login` with an administrator or editor account and use the returned bearer token.
2. `POST /api/v1/admin/articles` with a lower-case `slug`, `metadata: {}`, and a default-language `translation` whose `body`, `highlights`, and `tips` are JSON arrays.
3. Upload images to `/api/v1/admin/media`, set default-language alt text, then attach them with `PUT /api/v1/admin/articles/{id}/images`.
4. Add Khmer with `PUT /api/v1/admin/articles/{id}/translations/km`, then publish with `POST /api/v1/admin/articles/{id}/publish`.

The same workflow applies to experiences, accommodations, transportation, and travel information. Itineraries use a `days` array on create or `PUT /api/v1/admin/itineraries/{id}/days`; each activity references exactly one destination or experience. The publish step rejects itineraries that point to unpublished stops.

## Production notes

- Keep `JWT_SECRET`, database credentials, bootstrap credentials, and `MEDIA_DIR` in the deployment secret store/environment, not source control.
- Replace local `MEDIA_DIR` with durable object storage behind `MediaStorage` when running more than one application instance.
- Restrict `app.cors.allowed-origins` to the public site and staff CMS domains. Run migrations as part of deployment before scaling application instances.
