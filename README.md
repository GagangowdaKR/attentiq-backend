# ATTENTIQ — Backend
### Spring Boot 3.2 · Gradle · MySQL · JWT · Socket.IO

---

## ▶️  SETUP — Copy-paste these commands

### Prerequisites
- Java 17+ installed  →  `java -version`
- MySQL running       →  `mysql --version`
- Gradle (wrapper included, no install needed)

---

### Step 1 — Create MySQL database
```sql
-- Open MySQL Workbench or terminal:
mysql -u root -p

-- Then run:
CREATE DATABASE attentiq_db;
```

### Step 2 — Configure DB password
Open `src/main/resources/application.properties` and update:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 3 — Run the backend

**Windows:**
```bash
cd attentiq-backend
gradlew.bat bootRun
```

**Mac / Linux:**
```bash
cd attentiq-backend
chmod +x gradlew
./gradlew bootRun
```

### Step 4 — Verify it's running
Open browser → `http://localhost:8080/api/auth/login` should return 405 (Method Not Allowed = it's alive ✅)

---

## 📁  Project Structure

```
attentiq-backend/
│
├── build.gradle                          ← All dependencies (Gradle)
├── settings.gradle
│
└── src/main/java/com/attentiq/
    │
    ├── AttentiqApplication.java          ← Main entry point
    │
    ├── config/
    │   ├── SecurityConfig.java           ← JWT + CORS + Spring Security
    │   └── WebConfig.java                ← Static screenshot serving
    │
    ├── controller/
    │   ├── AuthController.java           ← /api/auth/*
    │   ├── MeetingController.java        ← /api/meetings/*
    │   ├── EventController.java          ← /api/events/*
    │   └── AnalyticsController.java      ← /api/analytics/*
    │
    ├── dto/
    │   ├── request/
    │   │   ├── AuthRequest.java          ← Login + Register bodies
    │   │   ├── MeetingRequest.java       ← Create + Join + Thresholds
    │   │   └── EventLogRequest.java      ← AI service event payload
    │   └── response/
    │       ├── AuthResponse.java
    │       ├── MeetingResponse.java
    │       ├── JoinMeetingResponse.java
    │       ├── EventResponse.java
    │       ├── HostOverviewResponse.java
    │       └── MeetingAnalyticsResponse.java
    │
    ├── entity/
    │   ├── User.java                     ← users table
    │   ├── Meeting.java                  ← meetings table
    │   ├── Participant.java              ← participants table
    │   └── AttentionEvent.java           ← attention_events table
    │
    ├── enums/
    │   ├── Role.java                     ← HOST | PARTICIPANT
    │   ├── EventType.java                ← EYES_CLOSED | FACE_MISSING | PHONE_DETECTED
    │   └── MeetingStatus.java            ← SCHEDULED | ACTIVE | ENDED
    │
    ├── exception/
    │   └── GlobalExceptionHandler.java   ← Unified error responses
    │
    ├── repository/                       ← Spring Data JPA
    │   ├── UserRepository.java
    │   ├── MeetingRepository.java
    │   ├── ParticipantRepository.java
    │   └── AttentionEventRepository.java
    │
    ├── security/
    │   ├── JwtUtil.java                  ← Token generate + validate
    │   └── JwtAuthFilter.java            ← Auth filter per request
    │
    ├── service/
    │   ├── AuthService.java              ← Register + Login
    │   ├── MeetingService.java           ← Full meeting lifecycle
    │   ├── EventService.java             ← Log AI events + screenshots
    │   ├── AnalyticsService.java         ← KPIs + overview + charts
    │   └── UserDetailsServiceImpl.java   ← Spring Security user loader
    │
    └── websocket/
        └── SocketIOService.java          ← Real-time alerts to host
```

---

## 🗄️  Database Tables (auto-created by Hibernate)

| Table              | Key Columns                                                     |
|--------------------|-----------------------------------------------------------------|
| `users`            | id, name, email, password, role, created_at                    |
| `meetings`         | id, title, code, host_id, status, thresholds, created_at       |
| `participants`     | id, meeting_id, user_id, attention_score, joined_at, left_at   |
| `attention_events` | id, meeting_id, user_id, event_type, screenshot_path, timestamp|

---

## 🌐  All API Endpoints

### Auth  `/api/auth`
| Method | Endpoint          | Body                          | Auth |
|--------|-------------------|-------------------------------|------|
| POST   | `/register`       | name, email, password, role   | ❌   |
| POST   | `/login`          | email, password               | ❌   |
| POST   | `/logout`         | —                             | ✅   |

### Meetings  `/api/meetings`
| Method | Endpoint              | Body / Param         | Auth |
|--------|-----------------------|----------------------|------|
| POST   | `/create`             | title                | ✅   |
| POST   | `/join`               | code                 | ✅   |
| POST   | `/{id}/leave`         | —                    | ✅   |
| POST   | `/{id}/end`           | —                    | ✅   |
| GET    | `/history`            | —                    | ✅   |
| GET    | `/{id}`               | —                    | ✅   |
| PUT    | `/{id}/thresholds`    | eye/face/phone vals  | ✅   |

### Events  `/api/events`
| Method | Endpoint              | Body                              | Auth |
|--------|-----------------------|-----------------------------------|------|
| POST   | `/log`                | userId, meetingId, eventType, img | ❌ (AI service) |
| GET    | `/meeting/{meetingId}`| —                                 | ✅   |

### Analytics  `/api/analytics`
| Method | Endpoint                      | Auth |
|--------|-------------------------------|------|
| GET    | `/host/overview`              | ✅   |
| GET    | `/meeting/{id}`               | ✅   |
| GET    | `/meeting/{id}/timeline`      | ✅   |

---

## 🔌  Real-time (Socket.IO)
- Runs on port **9092**
- Frontend connects with `meetingId` + `role` as query params
- Server pushes `attention:alert` event to host when AI detects something

---

## ➡️  Next Step
**AI Service** — Python + MediaPipe + YOLOv8
