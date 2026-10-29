# Online Shopping Cart – WebLogic Edition (`online-welogic-pro`)

This project is a copy of the **Online-Pro** Open Liberty application adapted to run on **Oracle WebLogic Server 14.1.x** (Jakarta EE 10 compatible).

---

## What changed from the Open Liberty version

| Aspect | Open Liberty | WebLogic |
|---|---|---|
| Server plugin | `liberty-maven-plugin` | `weblogic-maven-plugin` |
| Server config | `src/main/liberty/config/server.xml` | Removed – WebLogic uses domain config |
| DataSource config | `server.xml` `<dataSource>` | Domain-level `ShoppingCartDS-jdbc.xml` + `WEB-INF/weblogic.xml` |
| WAR descriptor | none needed | `WEB-INF/web.xml` (resource-ref) + `WEB-INF/weblogic.xml` |
| H2 driver location | Liberty `shared/resources/` | Bundled in `WEB-INF/lib` (compile-scope dep) |
| Context root | `contextRoot="/"` in server.xml | `<context-root>/</context-root>` in weblogic.xml |
| Default HTTP port | 5555 | 7001 (WebLogic default; change in domain config) |

---

## Prerequisites

| Tool | Version |
|---|---|
| Oracle WebLogic Server | 14.1.2.0+ (Jakarta EE 10) |
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | v20 (downloaded automatically by build) |

---

## Build

```bash
cd online-welogic-pro
mvn clean package
# Produces: target/online-welogic-pro.war
```

---

## DataSource setup (required before first deploy)

The application needs a JNDI DataSource named **`jdbc/ShoppingCartDS`** to be registered in the WebLogic domain.

### Option A – Copy the JDBC descriptor (recommended for dev)

1. Copy `src/main/webapp/WEB-INF/ShoppingCartDS-jdbc.xml` to your domain:

   ```bash
   cp src/main/webapp/WEB-INF/ShoppingCartDS-jdbc.xml \
       $DOMAIN_HOME/config/jdbc/ShoppingCartDS-jdbc.xml
   ```

2. Add H2 driver to the domain classpath (so WebLogic can load the driver at server start):

   ```bash
   cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar \
       $DOMAIN_HOME/lib/
   ```

3. Restart the WebLogic server.

### Option B – Administration Console

1. Open `http://localhost:7001/console`.
2. Navigate to **Services → Data Sources → New → Generic Data Source**.
3. Fill in:
   - **Name:** `ShoppingCartDS`
   - **JNDI Name:** `jdbc/ShoppingCartDS`
   - **Database Type:** Other
   - **Driver:** `org.h2.Driver`
   - **URL:** `jdbc:h2:mem:shoppingdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
   - **User:** `sa` / **Password:** *(empty)*
4. Target to your server (e.g., `AdminServer`) and activate changes.

---

## Deploy

### Manual (copy WAR)

```bash
mvn clean package
cp target/online-welogic-pro.war $DOMAIN_HOME/autodeploy/
```

### Via WebLogic Maven Plugin

The `weblogic-maven-plugin` JAR must be installed in your local Maven repository from your WLS installation:

```bash
cd $WL_HOME/server/lib
mvn install:install-file \
  -DgroupId=com.oracle.weblogic \
  -DartifactId=weblogic-maven-plugin \
  -Dversion=14.1.1-0-0 \
  -Dpackaging=jar \
  -Dfile=weblogic-maven-plugin.jar
```

Then deploy:

```bash
mvn clean package wls:deploy \
  -Dwls.adminUrl=t3://localhost:7001 \
  -Dwls.user=weblogic \
  -Dwls.password=<your-password>
```

---

## Access the application

Once deployed, open:

```
http://localhost:7001/
```

Default test credentials (seeded on first start):

| Username | Password | Role |
|---|---|---|
| `testuser` | `password123` | USER |
| `admin` | `admin123` | ADMIN |

---

## Running unit tests (no server required)

```bash
mvn test
```
