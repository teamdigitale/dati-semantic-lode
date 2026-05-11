# LODE (lode-ng)

LODE (*Live OWL Documentation Environment*) automatically generates navigable HTML documentation from OWL ontologies.

This fork ([teamdigitale/dati-semantic-lode](https://github.com/teamdigitale/dati-semantic-lode)) modernizes the upstream project [essepuntato/LODE](https://github.com/essepuntato/LODE) by porting it to:

- **Java 21** (Eclipse Temurin)
- **Spring Boot 3.x** with embedded Tomcat
- **Gradle** as the build system
- An executable JAR runnable with `java -jar` (no external Tomcat required)

> **Note for users of the upstream version with Tomcat:** previous LODE releases required deploying a WAR onto a servlet container (Tomcat, Jetty). This fork uses Spring Boot with embedded Tomcat: running `java -jar lode.jar` is enough. Deployment to an external application server is **not supported nor recommended**.

---

## Prerequisites

- Ubuntu 22.04+ (for native installation) or Docker

---

## Option 1: Docker (recommended)

Official images are published on GitHub Container Registry. This is the recommended installation mode.

### 1.1 Install Docker

```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable docker
sudo systemctl start docker
```

### 1.2 Create the environment file

Create a `lode.env` file and **customize the values** for your environment:

```env
# Public URL where LODE is reachable
EXTERNAL_URL=<LODE_PUBLIC_URL>

# WebVOWL URL for graphical ontology visualization
# If WebVOWL runs on the same host, use its public URL
WEBVOWL_URL=<WEBVOWL_URL>/#iri=

# Origins allowed for CORS (default: * = any)
CORS_ALLOWED_ORIGINS=*
```

### 1.3 Start the container

```bash
docker run -d \
  --name lode \
  --restart unless-stopped \
  -p 8080:8080 \
  --env-file lode.env \
  ghcr.io/teamdigitale/dati-semantic-lode:latest
```

Verify:

```bash
docker logs lode
# The application is available on http://localhost:8080
```

---

## Option 2: Native installation with systemd

This mode builds from source and runs the service as a system service.

### 2.1 Install Java 21

```bash
sudo apt update
sudo apt install -y eclipse-temurin-21-jdk
```

If the package is not available, add the Adoptium repository:

```bash
sudo apt install -y wget apt-transport-https gpg
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg
echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install -y temurin-21-jdk
```

Verify:

```bash
java -version
# openjdk version "21.x.x" ...
```

### 2.2 Fetch the sources and build

```bash
sudo useradd -r -s /usr/sbin/nologin lode

cd /opt
sudo git clone https://github.com/teamdigitale/dati-semantic-lode.git
cd dati-semantic-lode

# Build without tests
sudo ./gradlew clean build -x test

# Copy the artifact to the installation directory
sudo mkdir -p /opt/lode
sudo cp build/libs/lode.jar /opt/lode/lode.jar
sudo chown -R lode:lode /opt/lode
```

**(Optional)** Remove the sources after the build to save space:

```bash
sudo rm -rf /opt/dati-semantic-lode
```

### 2.3 Create the environment file

Create `/opt/lode/lode.env` and **customize the values** (same format as for Docker):

```env
# Public URL where LODE is reachable
EXTERNAL_URL=<LODE_PUBLIC_URL>

# WebVOWL URL for graphical ontology visualization
# If WebVOWL runs on the same host, use its public URL
WEBVOWL_URL=<WEBVOWL_URL>/#iri=

# Origins allowed for CORS (default: * = any)
CORS_ALLOWED_ORIGINS=*
```

### 2.4 Create the systemd unit file

Create `/etc/systemd/system/lode.service`:

```ini
[Unit]
Description=LODE - Live OWL Documentation Environment
After=network.target

[Service]
Type=simple
User=lode
Group=lode
WorkingDirectory=/opt/lode

EnvironmentFile=/opt/lode/lode.env
ExecStart=/usr/bin/java -jar /opt/lode/lode.jar

Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 2.5 Start the service

```bash
sudo systemctl daemon-reload
sudo systemctl enable lode
sudo systemctl start lode

# Check status
sudo systemctl status lode

# The service is available on http://localhost:8080
```

---

## Environment variables

| Variable | Description | Default |
|---|---|---|
| `EXTERNAL_URL` | Public URL where LODE is reachable | empty |
| `WEBVOWL_URL` | WebVOWL URL for graphical visualization (e.g. `https://webvowl.example.com/#iri=`) | `/webvowl/#iri=` |
| `CORS_ALLOWED_ORIGINS` | Origins allowed for CORS | `*` |

---

## Note for users of Apache HTTPD as a reverse proxy

If you already have an Apache HTTPD reverse proxy configured for the previous version (external Tomcat), keep in mind that the architectural model has changed:

- **Before:** Apache talked to a single Tomcat process on a single port, routing requests by path (e.g. `/lodview`, `/lode`, `/webvowl`).
- **Now:** each visualizer is an autonomous Spring Boot process listening on its own local port.

All applications start on port **8080** by default. If you run multiple visualizers on the same machine you need to assign different ports via the `SERVER_PORT` environment variable (see the [ports section](#ports) and the [LodView README](https://github.com/teamdigitale/dati-semantic-lodview) for the full table).

Apache can keep acting as a reverse proxy, but the backend is no longer a single shared Tomcat.

### Dedicated virtual hosts

If you use a dedicated domain (or subdomain) for each visualizer, the configuration is minimal:

```apache
<VirtualHost *:443>
    ServerName lode.example.com

    ProxyPass / http://localhost:8081/
    ProxyPassReverse / http://localhost:8081/

    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-Port "443"

    # ... SSL configuration ...
</VirtualHost>
```

### Path-based proxy (multiple visualizers on the same domain)

If you want to expose multiple visualizers under different paths of the same domain, you need to configure `SERVER_PORT`, `SERVER_SERVLET_CONTEXT_PATH` and **update LODE's environment variables to reflect the effective public URLs**.

Example `.env` file for LODE in path-based mode on `example.com`:

```env
SERVER_PORT=8081
SERVER_SERVLET_CONTEXT_PATH=/lode

# IMPORTANT: in path-based mode, EXTERNAL_URL and WEBVOWL_URL
# must reflect the effective public paths
EXTERNAL_URL=https://example.com/lode
WEBVOWL_URL=https://example.com/webvowl/#iri=

CORS_ALLOWED_ORIGINS=*
```

Apache configuration:

```apache
ProxyPass /lode http://localhost:8081/lode
ProxyPassReverse /lode http://localhost:8081/lode
```

> **Note:** without `SERVER_SERVLET_CONTEXT_PATH`, Spring Boot applications serve on `/` (root) and the path-based proxy would not work correctly. For the full Apache configuration covering all visualizers, see the [LodView README](https://github.com/teamdigitale/dati-semantic-lodview).

---

## Ports

| Port | Protocol | Description |
|---|---|---|
| 8080 | HTTP | LODE web interface (default, configurable via `SERVER_PORT`) |

---

## License

This project is released under the **GNU Affero General Public License v3.0 or later**. A copy of the license is available in [`LICENSE`](./LICENSE).
