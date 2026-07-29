import type {
    AccessRecord,
    Appointment,
    AuditEvent,
    BookingPayload,
    BookingResult,
    Branch,
    CompensationPackage,
    CreateAccountPayload,
    CreatedAccount,
    Department,
    Designation,
    Employee,
    Host,
    Role,
    RoleDefinition,
    SessionResponse,
    Slot,
    UserRoleAssignment,
} from "./types";

const API_URL = import.meta.env.VITE_API_URL ?? "/api/v1";
let accessToken: string | null = null;

export class ApiProblem extends Error {
    constructor(
        message: string,
        public readonly status: number,
        public readonly errorCode?: string,
        public readonly fieldErrors: Array<{ field: string; message: string }> = [],
    ) {
        super(message);
    }
}

export function setAccessToken(value: string | null) {
    accessToken = value;
}

async function request<T>(
    path: string,
    init: RequestInit = {},
    authenticated = true,
    retry = true,
): Promise<T> {
    const headers = new Headers(init.headers);
    if (init.body) headers.set("Content-Type", "application/json");
    if (authenticated && accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
    headers.set("X-Correlation-ID", crypto.randomUUID());

    const response = await fetch(`${API_URL}${path}`, {
        ...init,
        credentials: "include",
        headers,
    });

    if (response.status === 401 && authenticated && retry) {
        const refreshed = await fetch(`${API_URL}/auth/refresh`, {
            method: "POST",
            credentials: "include",
            headers: { "X-Refresh-Intent": "rotate" },
        });
        if (refreshed.ok) {
            const session = (await refreshed.json()) as SessionResponse;
            setAccessToken(session.accessToken);
            return request<T>(path, init, authenticated, false);
        }
        setAccessToken(null);
    }

    if (!response.ok) {
        const problem = (await response.json().catch(() => ({}))) as {
            detail?: string;
            errorCode?: string;
            fieldErrors?: Array<{ field: string; message: string }>;
        };
        throw new ApiProblem(
            problem.detail ?? "The request could not be completed.",
            response.status,
            problem.errorCode,
            problem.fieldErrors,
        );
    }

    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
}

export const api = {
    login: (login: string, password: string) =>
        request<SessionResponse>("/auth/login", {
            method: "POST",
            body: JSON.stringify({ login, password }),
        }, false),
    logout: () =>
        request<void>("/auth/logout", {
            method: "POST",
            headers: { "X-Refresh-Intent": "revoke" },
        }),
    hosts: () => request<Host[]>("/public/hosts", {}, false),
    slots: (hostId: string, date: string) =>
        request<Slot[]>(`/hosts/${hostId}/available-slots?date=${date}`, {}, false),
    book: (payload: BookingPayload, idempotencyKey: string) =>
        request<BookingResult>("/public/appointments", {
            method: "POST",
            headers: { "Idempotency-Key": idempotencyKey },
            body: JSON.stringify(payload),
        }, false),
    track: (reference: string) =>
        request<{
            referenceNumber: string;
            status: string;
            type: string;
            startsAt: string;
            endsAt: string;
            hostName: string;
        }>(`/public/appointments/${reference}`, {}, false),
    appointments: () => request<Appointment[]>("/appointments"),
    decide: (id: string, approve: boolean, remarks = "") =>
        request<Appointment>(`/appointments/${id}/${approve ? "approve" : "reject"}`, {
            method: "POST",
            body: JSON.stringify({ remarks }),
        }),
    visitorsInside: () => request<AccessRecord[]>("/reception/visitors-inside"),
    arrivals: () => request<Appointment[]>("/reception/appointments/arrivals"),
    checkIn: (appointmentId: string, gate = "Main gate", overrideReason?: string) =>
        request<AccessRecord>(`/reception/appointments/${appointmentId}/check-in`, {
            method: "POST",
            body: JSON.stringify({ gate, overrideReason }),
        }),
    checkOut: (recordId: string, gate = "Main gate") =>
        request<AccessRecord>(`/reception/access-records/${recordId}/check-out`, {
            method: "POST",
            body: JSON.stringify({ gate }),
        }),
    employees: () => request<Employee[]>("/employees?page=0&size=100"),
    branches: () => request<Branch[]>("/branches"),
    departments: () => request<Department[]>("/departments"),
    designations: () => request<Designation[]>("/designations"),
    createAccount: (payload: CreateAccountPayload) =>
        request<CreatedAccount>("/admin/accounts", {
            method: "POST",
            body: JSON.stringify(payload),
        }),
    pendingCompensation: () => request<CompensationPackage[]>("/compensation/change-requests"),
    decideCompensation: (id: string, approve: boolean) =>
        request<CompensationPackage>(`/compensation/change-requests/${id}/${approve ? "approve" : "reject"}`, {
            method: "POST",
        }),
    auditEvents: () => request<AuditEvent[]>("/audit-events?page=0&size=100"),
    configuration: () => request<Record<string, unknown>>("/admin/configuration"),
    roles: () => request<RoleDefinition[]>("/admin/roles"),
    roleUsers: () => request<UserRoleAssignment[]>("/admin/users"),
    replaceRoles: (userId: string, roles: Role[]) =>
        request<UserRoleAssignment>(`/admin/users/${userId}/roles`, {
            method: "PUT",
            body: JSON.stringify({ roles }),
        }),
};