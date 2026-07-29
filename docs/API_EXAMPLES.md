# API examples

All endpoints use `/api/v1`. Protected calls use `Authorization: Bearer <access-token>`.
Refresh tokens are issued as HttpOnly cookies.

## Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{"login":"ceo@brainserve.example","password":"replace-me"}
```

```json
{
  "accessToken": "eyJ...",
  "expiresInSeconds": 600,
  "mustChangePassword": true,
  "user": {
    "id": "9cd12c4b-7c56-4c91-9134-498f8b31f591",
    "displayName": "BrainServe CEO",
    "roles": ["ROLE_CEO"],
    "permissions": ["EMPLOYEE_CREATE", "APPOINTMENT_APPROVE"]
  }
}
```

## Public appointment

```http
POST /api/v1/public/appointments
Idempotency-Key: 887d4db8-f068-45fd-8e30-ec011730f32a
Content-Type: application/json

{
  "hostEmployeeId": "6f526e04-dc36-43b7-a74f-403d95072616",
  "type": "CLIENT_MEETING",
  "startsAt": "2026-08-03T05:30:00Z",
  "endsAt": "2026-08-03T06:00:00Z",
  "purpose": "Product discovery meeting",
  "visitor": {
    "firstName": "Ananya",
    "lastName": "Rao",
    "email": "ananya@example.com",
    "phone": "+919876543210",
    "company": "Example Labs",
    "consentVersion": "2026-07"
  }
}
```

```json
{
  "referenceNumber": "BSA-7Q3KX9M2",
  "status": "PENDING_VERIFICATION",
  "startsAt": "2026-08-03T05:30:00Z"
}
```

## Problem response

```json
{
  "type": "https://brainserve.example/problems/invalid-transition",
  "title": "Invalid state transition",
  "status": 409,
  "detail": "An APPROVED appointment cannot transition to PENDING_APPROVAL.",
  "instance": "/api/v1/appointments/4fd...",
  "errorCode": "APPOINTMENT_INVALID_TRANSITION",
  "timestamp": "2026-07-29T08:30:00Z",
  "correlationId": "3e898e3d-112a-4b3e-a52f-ad12eaf36b15",
  "fieldErrors": []
}
```
