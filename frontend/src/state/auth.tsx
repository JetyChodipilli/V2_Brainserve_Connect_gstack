import {
  createContext,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import { api, setAccessToken } from "../api/client";
import type { Role, User } from "../api/types";

interface AuthState {
  user: User | null;
  preview: boolean;
  login: (login: string, password: string) => Promise<User>;
  startPreview: (role?: Role) => void;
  changePreviewRole: (role: Role) => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

const previewGrants: Record<Role, string[]> = {
  ROLE_CEO: [
    "EMPLOYEE_CREATE",
    "EMPLOYEE_READ",
    "EMPLOYEE_UPDATE",
    "EMPLOYEE_STATUS_CHANGE",
    "SALARY_READ",
    "SALARY_WRITE",
    "SALARY_APPROVE",
    "DEPARTMENT_MANAGE",
    "DESIGNATION_MANAGE",
    "ROLE_MANAGE",
    "APPOINTMENT_REQUEST",
    "APPOINTMENT_APPROVE",
    "APPOINTMENT_REJECT",
    "APPOINTMENT_RESCHEDULE",
    "CEO_APPOINTMENT_APPROVE",
    "VISITOR_REGISTER",
    "VISITOR_VERIFY",
    "VISITOR_CHECK_IN",
    "VISITOR_CHECK_OUT",
    "REPORT_VIEW",
    "AUDIT_VIEW",
  ],
  ROLE_HR_ADMIN: [
    "EMPLOYEE_CREATE",
    "EMPLOYEE_READ",
    "SALARY_READ",
    "SALARY_WRITE",
    "SALARY_APPROVE",
    "DEPARTMENT_MANAGE",
    "DESIGNATION_MANAGE",
    "APPOINTMENT_APPROVE",
    "APPOINTMENT_REJECT",
    "APPOINTMENT_RESCHEDULE",
    "VISITOR_REGISTER",
    "VISITOR_VERIFY",
    "REPORT_VIEW",
    "AUDIT_VIEW",
  ],
  ROLE_HR_EXECUTIVE: [
    "EMPLOYEE_CREATE",
    "EMPLOYEE_READ",
    "EMPLOYEE_UPDATE",
    "APPOINTMENT_APPROVE",
    "APPOINTMENT_REJECT",
    "APPOINTMENT_RESCHEDULE",
    "VISITOR_REGISTER",
    "VISITOR_VERIFY",
    "REPORT_VIEW",
  ],
  ROLE_EMPLOYEE: [
    "EMPLOYEE_READ",
    "APPOINTMENT_REQUEST",
    "APPOINTMENT_APPROVE",
    "APPOINTMENT_REJECT",
    "APPOINTMENT_RESCHEDULE",
  ],
  ROLE_RECEPTIONIST: [
    "EMPLOYEE_READ",
    "VISITOR_REGISTER",
    "VISITOR_VERIFY",
    "VISITOR_CHECK_IN",
    "VISITOR_CHECK_OUT",
    "REPORT_VIEW",
  ],
  ROLE_SECURITY: ["VISITOR_VERIFY", "VISITOR_CHECK_IN", "VISITOR_CHECK_OUT", "REPORT_VIEW"],
  ROLE_SYSTEM_ADMIN: ["ROLE_MANAGE", "SYSTEM_CONFIGURE", "AUDIT_VIEW"],
};

function previewUser(role: Role): User {
  const names: Record<Role, string> = {
    ROLE_CEO: "Ananya Rao",
    ROLE_HR_ADMIN: "Maya Reddy",
    ROLE_HR_EXECUTIVE: "Kiran Shah",
    ROLE_EMPLOYEE: "Arjun Mehta",
    ROLE_RECEPTIONIST: "Priya Nair",
    ROLE_SECURITY: "Ravi Kumar",
    ROLE_SYSTEM_ADMIN: "System Administrator",
  };
  return {
    id: `preview-${role}`,
    employeeId: role === "ROLE_SYSTEM_ADMIN" ? undefined : `employee-${role}`,
    displayName: names[role],
    login: `${role.slice(5).toLowerCase()}@brainserve.local`,
    roles: [role],
    permissions: previewGrants[role],
    mustChangePassword: false,
  };
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null);
  const [preview, setPreview] = useState(false);

  const value = useMemo<AuthState>(() => ({
    user,
    preview,
    login: async (login, password) => {
      const session = await api.login(login, password);
      setAccessToken(session.accessToken);
      setPreview(false);
      setUser(session.user);
      return session.user;
    },
    startPreview: (role = "ROLE_HR_ADMIN") => {
      setAccessToken(null);
      setPreview(true);
      setUser(previewUser(role));
    },
    changePreviewRole: (role) => {
      if (!preview) return;
      setUser(previewUser(role));
    },
    logout: async () => {
      if (!preview) await api.logout().catch(() => undefined);
      setAccessToken(null);
      setPreview(false);
      setUser(null);
    },
  }), [preview, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
