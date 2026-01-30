# Keycloak Setup Guide

This guide explains how to configure Keycloak for the Java Banking Core application.

## Quick Start

### 1. Start Keycloak

```bash
docker-compose up keycloak keycloak-db
```

Keycloak will be available at: http://localhost:8180

**Admin Credentials:**
- Username: `admin`
- Password: `admin`

### 2. Access Admin Console

1. Navigate to http://localhost:8180
2. Click "Administration Console"
3. Login with admin credentials

### 3. Create Realm

1. Click the dropdown in the top-left (currently showing "master")
2. Click "Create Realm"
3. Set **Realm name**: `banking-realm`
4. Click "Create"

### 4. Create Client

1. Go to "Clients" in the left menu
2. Click "Create client"
3. Configure:
   - **Client ID**: `banking-api`
   - **Client Protocol**: `openid-connect`
   - Click "Next"
4. Capability config:
   - Enable **Client authentication**: ON
   - Enable **Authorization**: OFF
   - Enable **Standard flow**: ON
   - Enable **Direct access grants**: ON
   - Click "Next"
5. Login settings:
   - **Valid redirect URIs**: `*` (for development only)
   - **Web origins**: `*` (for development only)
   - Click "Save"

### 5. Create Roles

1. Go to "Realm roles" in the left menu
2. Click "Create role"
3. Create the following roles:
   - **Role name**: `ADMIN`
     - Description: Administrator with full access
   - **Role name**: `USER`
     - Description: Regular user with read/write access

### 6. Create Test Users

#### Admin User
1. Go to "Users" in the left menu
2. Click "Add user"
3. Configure:
   - **Username**: `admin_user`
   - **Email**: `admin@banking.com`
   - **First name**: `Admin`
   - **Last name**: `User`
   - **Email verified**: ON
   - Click "Create"
4. Go to "Credentials" tab:
   - Click "Set password"
   - **Password**: `admin123`
   - **Temporary**: OFF
   - Click "Save"
5. Go to "Role mapping" tab:
   - Click "Assign role"
   - Select `ADMIN` and `USER` roles
   - Click "Assign"

#### Regular User
1. Click "Add user" again
2. Configure:
   - **Username**: `test_user`
   - **Email**: `user@banking.com`
   - **First name**: `Test`
   - **Last name**: `User`
   - **Email verified**: ON
   - Click "Create"
3. Go to "Credentials" tab:
   - Click "Set password"
   - **Password**: `user123`
   - **Temporary**: OFF
   - Click "Save"
4. Go to "Role mapping" tab:
   - Click "Assign role"
   - Select `USER` role
   - Click "Assign"

## Obtaining Access Tokens

### Using Password Grant (for testing)

```bash
curl -X POST http://localhost:8180/realms/banking-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=banking-api" \
  -d "grant_type=password" \
  -d "username=admin_user" \
  -d "password=admin123"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer"
}
```

### Using the Access Token

```bash
# Extract the access token from the response
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI..."

# Use it in API requests
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/accounts
```

## Token Information

### Inspecting Tokens

You can decode JWT tokens at https://jwt.io to see their contents:

**Typical token payload:**
```json
{
  "exp": 1706737372,
  "iat": 1706737072,
  "jti": "4f8b4c8a-...",
  "iss": "http://localhost:8180/realms/banking-realm",
  "sub": "a5b1c2d3-...",
  "typ": "Bearer",
  "azp": "banking-api",
  "realm_access": {
    "roles": ["ADMIN", "USER"]
  },
  "preferred_username": "admin_user",
  "email": "admin@banking.com"
}
```

## Production Considerations

### Security Hardening

1. **Enable HTTPS**: Configure SSL/TLS certificates
2. **Restrict Origins**: Set specific redirect URIs and web origins
3. **Strong Passwords**: Enforce password policies
4. **Token Expiration**: Reduce token lifetime (default: 5 minutes)
5. **Refresh Tokens**: Use refresh tokens for long-lived sessions

### Realm Configuration Export/Import

#### Export Realm
```bash
docker exec -it <keycloak-container> \
  /opt/keycloak/bin/kc.sh export \
  --dir /tmp/export \
  --realm banking-realm
```

#### Import Realm
Place `banking-realm.json` in a volume and import on startup:
```yaml
keycloak:
  command: start-dev --import-realm
  volumes:
    - ./keycloak/realms:/opt/keycloak/data/import
```

### Database Backend

In production, use a production-grade database:
- PostgreSQL (already configured in docker-compose)
- MySQL/MariaDB
- Oracle
- MS SQL Server

### High Availability

For production deployments:
1. Run multiple Keycloak instances behind a load balancer
2. Use shared database for session storage
3. Configure clustering for cache replication
4. Use external cache (Infinispan/Redis)

## Troubleshooting

### Cannot Obtain Token

**Error**: `invalid_grant` or `unauthorized_client`

**Solutions:**
- Verify username/password are correct
- Check that user roles are assigned
- Ensure client ID is correct (`banking-api`)
- Verify realm name is `banking-realm`

### Token Validation Fails

**Error**: `An error occurred while attempting to decode the Jwt`

**Solutions:**
- Verify `issuer-uri` in application.yaml matches Keycloak
- Check that Keycloak is accessible from the application
- Ensure realm and client are properly configured
- Check network connectivity between services

### Connection Refused

**Error**: `Connection refused: localhost:8180`

**Solutions:**
- Wait for Keycloak to fully start (can take 30-60 seconds)
- Check Keycloak logs: `docker-compose logs keycloak`
- Verify port 8180 is not in use by another service

## Additional Resources

- [Official Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/)
- [Spring Security OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
