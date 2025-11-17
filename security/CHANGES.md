# Security Service - Eureka Integration Changes

## What Changed

The Security Service has been updated to register with Eureka Server for service discovery.

### 1. Dependencies Updated (pom.xml)

**Spring Boot Version:**
- Before: `3.5.7`
- After: `3.4.1` (compatible with Spring Cloud 2024.0.0)

**Java Version:**
- Before: `21` (but you have Java 17)
- After: `17` (matches your system)

**New Dependencies Added:**
```xml
<!-- Added Spring Cloud version -->
<spring-cloud.version>2024.0.0</spring-cloud.version>

<!-- Added Eureka Client -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<!-- Added Spring Cloud Dependency Management -->
<dependencyManagement>...</dependencyManagement>
```

### 2. Application Configuration (application.yml)

**Port Changed:**
- Before: `8080` (default)
- After: `8082` (to make room for API Gateway on 8080)

**Service Name Added:**
```yaml
spring:
  application:
    name: security-service  # How it appears in Eureka
```

**Database Name Changed:**
- Before: `bball-microservice`
- After: `bball-security` (following microservices pattern: one DB per service)

**Eureka Configuration Added:**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    hostname: localhost
```

### 3. Application Class (SecurityApplication.java)

**Annotation Added:**
```java
@EnableDiscoveryClient  // Enables Eureka client features
```

---

## What This Means

### Before (Standalone):
```
Security Service (port 8080)
└─ Works independently
└─ No service discovery
```

### After (With Eureka):
```
Eureka Server (port 8761)
└─ Security Service registers here (port 8082)
   └─ Name: SECURITY-SERVICE
   └─ Other services can discover it by name
```

---

## How to Run

### 1. Start Eureka Server First
```bash
cd eureka-server
./mvnw spring-boot:run
```

### 2. Start Security Service
```bash
cd security
./mvnw spring-boot:run
```

### 3. Verify Registration
Visit: **http://localhost:8761**

You should see:
```
Application         Status    
---------------------------
SECURITY-SERVICE    UP (1)
```

---

## What Happens When It Starts

```
1. Security Service starts on port 8082
2. Connects to Eureka at http://localhost:8761/eureka/
3. Registers as "SECURITY-SERVICE"
4. Sends heartbeat every 30 seconds ❤️
5. Other services can now find it via Eureka
```

---

## Important Database Note

⚠️ **You need to create the new database:**

```sql
-- Before starting security service
CREATE DATABASE "bball-security";
```

The old database name was `bball-microservice`, but we're following the microservices pattern where each service has its own database.

---

## Testing Endpoints

All existing endpoints still work, just on a new port:

**Before:**
- `http://localhost:8080/api/v1/auth/register`
- `http://localhost:8080/api/v1/auth/authenticate`

**After:**
- `http://localhost:8082/api/v1/auth/register`
- `http://localhost:8082/api/v1/auth/authenticate`

---

## Benefits of Eureka Integration

1. **Service Discovery**: Other services can find security service by name, not hardcoded URL
2. **Load Balancing**: Can run multiple security service instances
3. **Health Monitoring**: Eureka tracks if service is UP or DOWN
4. **Dynamic Scaling**: Add/remove instances without code changes

---

## Next Steps

- ✅ Security Service now registers with Eureka
- ⏭️ Create Stats Service (will also register with Eureka)
- ⏭️ Stats Service will discover Security Service via Eureka for token validation
- ⏭️ Add API Gateway to route requests

---

## Latest Update: Logout Endpoint Added

### New Files Created:
- `LogoutResponse.java` - DTO for logout response
- `LOGOUT_GUIDE.md` - Complete logout documentation

### Files Modified:
- `AuthenticationController.java` - Added `/logout` endpoint
- `AuthenticationService.java` - Added logout logic with TODO for production token blacklist
- `SecurityConfiguration.java` - Updated to require authentication for logout

### New Endpoint:
```http
POST /api/v1/auth/logout
Headers: Authorization: Bearer <token>
Response: { "message": "Logged out successfully...", "timestamp": 123456789 }
```

**Security:** Logout requires valid JWT token (protected endpoint)

**How It Works:**
- Client calls logout with JWT token
- Server clears security context
- Client must delete token from storage
- Token expires naturally after 24 minutes

**Production TODO:** Implement Redis-based token blacklist to invalidate tokens immediately

See `LOGOUT_GUIDE.md` for complete documentation and usage examples.

---

## Rolling Back (If Needed)

If you want to go back to standalone mode:

1. Remove Eureka config from `application.yml`
2. Change port back to 8080
3. Remove `@EnableDiscoveryClient` annotation
4. The service will work standalone again

