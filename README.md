# FarmConnect — Farmer Market Direct Selling App

Full-stack app connecting farmers directly with customers. Two projects:

```
farmconnect-backend/   Spring Boot 3.2.5 + Java 17 + MySQL REST API
farmconnect-android/   Android app, Java + XML, Retrofit
```

---

## 1. Backend setup

**Requirements:** Java 17, Maven, MySQL 8 running locally.

```bash
cd farmconnect-backend

# 1. Create the database (or let ddl-auto=update create it for you)
mysql -u root -p -e "CREATE DATABASE farmconnect_db;"

# 2. Edit src/main/resources/application.properties:
#    - spring.datasource.password -> your MySQL root password
#    - app.jwt.secret             -> replace with your own random 32+ char secret
#      (a demo fallback is already set so it runs out of the box)

# 3. Run it
mvn spring-boot:run
```

The API comes up on `http://localhost:8080`. Tables are auto-created from the
JPA entities on first run (`spring.jpa.hibernate.ddl-auto=update`).

**Quick smoke test with curl:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Customer","email":"test@example.com","phone":"9876543210","password":"password123","confirmPassword":"password123","role":"CUSTOMER"}'
```
You should get back a JSON object with a `token`.

**Postman:** import the endpoints from `ApiService.java` in the Android
project, or hit them directly — every controller under
`src/main/java/com/farmconnect/controller/` maps 1:1 to a REST resource
(`/api/auth`, `/api/products`, `/api/customer/cart`, `/api/customer/orders`,
`/api/farmer/orders`, `/api/admin/...`, etc).

### Notes on what's real vs. heuristic
- **Everything is fully wired** — auth, products, cart, orders (with stock
  validation and status flow), reviews (gated to delivered orders), farmer
  verification, admin dashboard, image upload, notifications.
- **AI price suggestion** (`GET /api/farmer/price-suggestion`) uses a
  transparent heuristic today (average price of similar listed products,
  +15% for organic) rather than a trained ML model — this was called out as
  "structure only" in the original spec. `PricePredictionServiceImpl.java`
  has a clear seam documented for swapping in a real model/API later.
- **Image storage** is local disk under `uploads/` (served back at
  `/uploads/<file>`) — no external API key needed, works fully offline.

---

## 2. Android app setup

**Requirements:** Android Studio (Hedgehog+), JDK 17.

```bash
# Open farmconnect-android/ in Android Studio and let Gradle sync.
```

**Before running:** check `ApiClient.java`
(`app/src/main/java/com/farmconnect/android/network/ApiClient.java`):

- **Emulator** (default): `BASE_URL = "http://10.0.2.2:8080/"` — this
  already points at your host machine's `localhost:8080`, so just make sure
  the backend is running. No changes needed.
- **Physical device on the same Wi-Fi:** change `BASE_URL` to your dev
  machine's LAN IP, e.g. `"http://192.168.1.23:8080/"`.
- **Production:** point it at your deployed backend's `https://` URL, and
  remove `android:usesCleartextTraffic="true"` from `AndroidManifest.xml`.

Run the app — Splash routes to Login/Register, then to the right home
screen based on role (Customer / Farmer / Admin).

### What's implemented
- **Auth:** Splash, Login, Register (role picker, farmer fields shown
  conditionally)
- **Customer:** Home (category chips, search, organic filter), Product
  Detail, Cart (quantity +/-, remove), Checkout (address + COD/UPI/Card),
  Order History, Order Tracking (visual status stepper)
- **Farmer:** Home (shows verification status banner), Manage Products
  (list, availability toggle, delete), Add/Edit Product (with image picker
  and "Get AI Price Suggestion" button), Orders for My Products
  (status dropdown + update)
- **Admin:** Dashboard (live stats: users, farmers, customers, products,
  orders, pending verifications, revenue), Farmer Verification
  (approve/reject pending farmers)

### Known gaps / next steps if you keep building
- Reviews & ratings have a working backend + UI hooks in `ApiService`, but
  no dedicated "write a review" screen in the app yet — worth adding to
  `ProductDetailActivity`.
- Notifications: backend fully tracks them (`/api/notifications`), but
  there's no notifications screen/badge in the Android app yet.
- Sales reports (`SalesReportResponse` DTO exists) aren't wired to a
  controller endpoint or Android screen yet.
- No offline caching — every screen hits the network on load.
- Farmer/Admin profile editing screens aren't built (backend endpoints for
  farmer profile updates exist and are ready to use).

---

## 3. Why some things aren't "fully automatic"

This was built in a sandboxed environment without access to Maven Central
or an Android SDK/emulator, so **neither project has been compiled**. I've
written it carefully and the patterns are consistent throughout, but please
build both locally before relying on it — there could be a typo or a small
wiring mismatch I couldn't catch without a compiler. If `mvn` or Gradle
flags an error, it's most likely a missing import or a small type mismatch,
not a structural issue.


## Continuation update (20 Aug 2026)

The next build layer adds:
- Customer review UI on Product Detail, backed by delivered-order eligibility checks.
- Role-independent Notifications screen backed by notification list/read endpoints.
- Farmer Sales Report endpoint and Android screen for delivered-order revenue, units sold, and top products.
- Notification entry points from Customer, Farmer, and Admin home screens.

The source was updated after inspecting the supplied project archives. Local Android Studio and Maven builds are still required because this environment does not provide the Android SDK/Gradle or Maven executable.
