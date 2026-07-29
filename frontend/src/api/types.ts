export type Role =
  | "ROLE_CEO"
  | "ROLE_HR_ADMIN"
  | "ROLE_HR_EXECUTIVE"
  | "ROLE_EMPLOYEE"
  | "ROLE_RECEPTIONIST"
  | "ROLE_SECURITY"
  | "ROLE_SYSTEM_ADMIN";

export interface User {
  id: string;
  employeeId?: string;
  displayName: string;
  login: string;
  roles: Role[];
  permissions: string[];
  mustChangePassword: boolean;
}

export interface SessionResponse {
  accessToken: string;
  expiresInSeconds: number;
  user: User;
}

export interface Host {
  id: string;
  displayName: string;
  departmentId: string;
  designationId: string;
}

export interface Slot {
  startsAt: string;
  endsAt: string;
}

export interface BookingPayload {
  hostEmployeeId: string;
  type: string;
  startsAt: string;
  endsAt: string;
  purpose: string;
  visitor: {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    company?: string;
    consentVersion: string;
  };
}

export interface BookingResult {
  referenceNumber: string;
  status: string;
  startsAt: string;
  verificationRequired: boolean;
  developmentVerificationCode?: string;
}

export interface Appointment {
  id: string;
  referenceNumber: string;
  status: string;
  type: string;
  purpose: string;
  startsAt: string;
  endsAt: string;
  visitor: {
    id: string;
    displayName: string;
    company?: string;
    maskedEmail: string;
    verificationStatus: string;
    restricted: boolean;
  };
  host: {
    id: string;
    displayName: string;
    employeeNumber: string;
  };
}

export interface AccessRecord {
  id: string;
  appointmentId: string;
  visitorName: string;
  company?: string;
  hostName: string;
  badgeNumber: string;
  entryGate: string;
  checkedInAt: string;
  checkedOutAt?: string;
}

export interface Employee {
  id: string;
  employeeNumber: string;
  displayName: string;
  officialEmail: string;
  phone?: string;
  departmentId: string;
  designationId: string;
  branchId: string;
  managerId?: string;
  employmentType: string;
  joiningDate: string;
  status: string;
  workLocation?: string;
  version: number;
}

export interface CompensationPackage {
  id: string;
  employeeId: string;
  basic: number;
  hra: number;
  allowances: number;
  deductions: number;
  gross: number;
  net: number;
  annualCtc: number;
  currency: string;
  effectiveFrom: string;
  effectiveTo?: string;
  status: string;
  proposedBy: string;
  approvedBy?: string;
  version: number;
}

export interface AuditEvent {
  id: string;
  eventType: string;
  actorUserId?: string;
  action: string;
  entityType: string;
  entityId?: string;
  reason?: string;
  correlationId?: string;
  sensitivity: string;
  occurredAt: string;
}

export interface RoleDefinition {
  id: string;
  code: Role;
  name: string;
  systemRole: boolean;
  permissions: string[];
}

export interface UserRoleAssignment {
  id: string;
  login: string;
  displayName: string;
  enabled: boolean;
  roles: Role[];
}
