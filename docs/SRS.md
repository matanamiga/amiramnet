**1. Introduction**

**1.1 Purpose**
This Software Requirements Specification (SRS) document defines the functional and non‑functional requirements for **INSIDE JOB**, a mobile application built with Flutter. It is intended for a senior Flutter developer and outlines all necessary features, constraints, and design considerations to deliver a production‑ready app.

**1.2 Scope**
INSIDE JOB is a personal productivity and health tracker that enables users to:

* Track daily, weekly, and monthly weight with interactive charts.
* Manage tasks on a daily, weekly, and monthly basis, with Firebase‑driven notifications.
* Additional Feature A: **Goal Setting & Reminders** for weight and tasks.
* Additional Feature B: **Data Export & Backup** (CSV/JSON export to cloud storage).

**1.3 Definitions, Acronyms, Abbreviations**

* SRS: Software Requirements Specification
* UI: User Interface
* API: Application Programming Interface
* FCM: Firebase Cloud Messaging
* CRUD: Create, Read, Update, Delete

**1.4 References**

* Flutter Official Docs (flutter.dev)
* Firebase Docs (firebase.google.com)
* Material Design Guidelines (material.io)

**1.5 Overview**
The remainder of this document covers overall description, detailed requirements, architecture, use cases, testing, and operational guidelines.

---

**2. Overall Description**

**2.1 Product Perspective**
INSIDE JOB is a standalone mobile app developed in Flutter. It integrates with Firebase for real‑time database, authentication, and push notifications. Charting is implemented via a Flutter charting library (e.g., charts_flutter or fl_chart).

**2.2 Product Functions**

1. Weight Tracking Module
2. Task Management Module
3. Notification Module (Firebase)
4. Graphing Component
5. Goal Setting & Reminders
6. Data Export & Backup

**2.3 User Classes and Characteristics**

* **End User**: Non‑technical individual tracking weight and tasks.
* **Admin**: Optional; manages app settings, backups, and user support.

**2.4 Operating Environment**

* Android 8.0+ and iOS 13+
* Network connectivity for Firebase and backup services

**2.5 Design and Implementation Constraints**

* Must use Flutter (Dart)
* Firebase for backend services
* Charts library must be MIT/BSD licensed

**2.6 Assumptions and Dependencies**

* Users have internet access for sync, notifications, and backups.
* Firebase and third‑party services (Google Drive, Dropbox) are pre‑configured.

---

**3. External Interface Requirements**

**3.1 User Interfaces**

* **Home Screen**: Summary cards for weight, tasks, and goals.
* **Weight Screen**: Data entry form + toggles (daily/weekly/monthly) + interactive chart.
* **Tasks Screen**: Task list with filters + creation/edit form.
* **Goals Screen**: Define and review weight/task goals.
* **Settings Screen**: Backup, export, preferences (language, notifications, theme).

**3.2 Hardware Interfaces**

* No special hardware dependencies.
* Optional: local storage (SQLite) for offline cache.

**3.3 Software Interfaces**

* Firebase Firestore, Auth, Cloud Messaging
* Google Drive / Dropbox API via OAuth
* Optional: Crashlytics for error reporting

**3.4 Communications Interfaces**

* HTTPS for all backend and third‑party API calls

---

**4. System Features and Requirements**

### 4.1 Weight Tracking Module

**4.1.1 Description & Priority:** Core feature.
**4.1.2 Functional Requirements:**

* FR‑W1: User can input weight (kg or lb) with timestamp.
* FR‑W2: Store entries under `/users/{uid}/weights/`.
* FR‑W3: View history by day/week/month.
* FR‑W4: Interactive line chart with zoom and pan.
* FR‑W5: Offline entry and sync when online.

### 4.2 Task Management Module

**4.2.1 Description & Priority:** Core feature.
**4.2.2 Functional Requirements:**

* FR‑T1: CRUD tasks with title, description, due date/time, recurrence.
* FR‑T2: Store under `/users/{uid}/tasks/`.
* FR‑T3: Filters for daily/weekly/monthly views.
* FR‑T4: Mark complete and archive.
* FR‑T5: Offline support with local persistence.

### 4.3 Notification Module (Firebase)

**4.3.1 Functional Requirements:**

* FR‑N1: Schedule push/local notifications via FCM.
* FR‑N2: Configurable reminders (lead time, snooze).
* FR‑N3: Handle permission requests gracefully.

### 4.4 Graphing Component

**4.4.1 Functional Requirements:**

* FR‑G1: Support line, bar, and pie charts as needed.
* FR‑G2: Time range toggles (7d, 4w, 12m).
* FR‑G3: Export chart images via share intent.

### 4.5 Goal Setting & Reminders

**4.5.1 Functional Requirements:**

* FR‑G1: Set numerical weight goal and deadline.
* FR‑G2: Set quantitative task goals (e.g., X tasks/day).
* FR‑G3: Automated progress notifications.

### 4.6 Data Export & Backup

**4.6.1 Functional Requirements:**

* FR‑E1: Export data as CSV/JSON.
* FR‑E2: OAuth-backed backup to Google Drive/Dropbox.
* FR‑E3: Manual and scheduled weekly backups.

---

**5. Non‑Functional Requirements**

**5.1 Performance:**

* <2s cold start on mid‑range devices.
* <200ms chart render for 1 year of data.

**5.2 Security & Privacy:**

* HTTPS + TLS for all data in transit.
* Firebase Security Rules to restrict user data access.
* GDPR compliance: user consent for backups and analytics.

**5.3 Reliability & Availability:**

* Offline-first with local cache.
* 99.9% backend uptime (Firebase SLA).
* Crash reporting via Crashlytics.

**5.4 Scalability:**

* Support 10k+ users without schema updates.
* Modular codebase to ease future feature additions.

**5.5 Maintainability:**

* MVVM/BLoC architecture.
* Clean separation: UI, business logic, data.
* Comprehensive code comments and documentation.

**5.6 Accessibility:**

* WCAG 2.1 AA compliance for color contrast and screen readers.
* Support dynamic font sizing and screen magnification.

**5.7 Localization & Internationalization:**

* Flutter Intl plugin for string externalization.
* Right-to-left language support.

---

**6. Architecture Design**

**6.1 Layers:**

* Presentation: Flutter widgets + Riverpod.
* Domain: Use-case services, business rules.
* Data: Firestore, SQLite cache, third‑party APIs.

**6.2 Technology Stack:**

* Flutter & Dart
* Riverpod/BLoC, Freezed for models
* Firebase Core, Auth, Firestore, Messaging, Crashlytics
* fl_chart / charts_flutter
* OAuth2 client for cloud services

**6.3 Data Models:**

```dart
class WeightEntry {String id; double value; DateTime timestamp;}
class Task {String id; String title; String description; DateTime dueDate; Recurrence recurrence; bool isCompleted;}
class Goal {String id; GoalType type; double targetValue; DateTime deadline;}
class UserSettings {String uid; String locale; bool notificationsEnabled;}
```

---

**7. Detailed Use Cases**

**7.1 Add Daily Weight**

1. Tap [+] on Weight screen.
2. Enter value & timestamp; save.
3. Offline: store locally; sync when online.

**7.2 View Weight Trends**

1. Select filter (daily/weekly/monthly).
2. Chart updates; option to export image.

**7.3 Create Recurring Task**

1. Tap [+] on Tasks screen.
2. Input fields; choose recurrence.
3. Save → Firestore + schedule FCM.

**7.4 Export & Backup**

1. Settings → Export & Backup.
2. Choose format/destination; confirm.
3. App generates file and uploads.

**7.5 Onboarding & Tutorial**

1. First launch triggers walkthrough.
2. Highlight key screens and actions.

---

**8. API Endpoints / Services**

| Service     | Method | Path                            | Description             |
| ----------- | ------ | ------------------------------- | ----------------------- |
| Add Weight  | POST   | /weights                        | Create weight entry     |
| Get Weights | GET    | /weights?range={day,week,month} | Retrieve weight entries |
| Add Task    | POST   | /tasks                          | Create new task         |
| Get Tasks   | GET    | /tasks?range={day,week,month}   | Retrieve tasks          |
| Export Data | POST   | /export?type={csv,json}         | Generate export file    |
| Backup Data | POST   | /backup?service={drive,dropbox} | Trigger backup job      |

---

**9. UI/UX Guidelines**

* Material Design 3 with dynamic color.
* Responsive layouts for tablets/large screens.
* ≥48dp touch targets; accessible labels.
* Light/dark themes; high contrast option.

---

**10. Testing Requirements**

**10.1 Unit Tests:**

* Business logic: weight, tasks, goals.
* Serialization and validation.

**10.2 Integration Tests:**

* Firestore & emulator.
* FCM scheduling and delivery.

**10.3 UI Tests:**

* Golden screenshots for key screens.
* Widget tests for form error handling.

**10.4 Performance & Load Testing:**

* Simulate 10k users; measure response and render times.

---

**11. Deployment & Operations**

**11.1 CI/CD:**

* GitHub Actions: lint → test → build → deploy.
* Deploy to TestFlight & Play Store internal.

**11.2 Monitoring & Logging:**

* Crashlytics for crash reporting.
* Firebase Analytics for usage metrics.
* Centralized logging (Stackdriver).

**11.3 Rollback Strategy:**

* Version pinning; ability to revert builds.
* Feature flags for incremental rollouts.

**11.4 Support & Maintenance:**

* Issue tracking in Jira.
* Scheduled health checks and backups.

---

**12. Security & Compliance**

* GDPR: data export/deletion on user request.
* Privacy Policy embedded in app.
* OAuth scopes minimized; tokens secured.

---

**13. Accessibility Requirements**

* Screen reader support; semantic labels.
* Contrast ratio ≥4.5:1.
* Adjustable font sizes.

---

**14. Risk Analysis & Mitigation**

| Risk                     | Likelihood | Impact | Mitigation                       |
| ------------------------ | ---------- | ------ | -------------------------------- |
| Firebase outages         | Medium     | High   | Offline cache; retry logic       |
| Data loss during sync    | Low        | High   | Transactional writes; backups    |
| Unauthorized data access | Low        | High   | Strict security rules; pen tests |

---

**Appendix A: Wireframes**
*(Attach Figma/Sketch links)*

**Appendix B: Security Rules Snippet**

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth.uid === $uid",
        ".write": "auth.uid === $uid"
      }
    }
  }
}
```

**Appendix C: Data Retention Policy**

* Weights and tasks archived after 2 years.
* Users can request full deletion of data.

**Appendix D: Glossary**

* **FCM**: Firebase Cloud Messaging
* **CRUD**: Create, Read, Update, Delete
* **OAuth**: Open Authorization standard
