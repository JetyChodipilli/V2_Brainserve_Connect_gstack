import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  CalendarCheck2,
  Check,
  CircleDollarSign,
  ClipboardCheck,
  Clock3,
  DoorOpen,
  FileClock,
  LayoutDashboard,
  LogOut,
  Menu,
  Search,
  Settings2,
  ShieldCheck,
  UsersRound,
  X,
} from "lucide-react";
import { useEffect, useMemo, useState, type ComponentType } from "react";
import { useNavigate } from "../routing";
import { api } from "../api/client";
import type {
  AccessRecord,
  Appointment,
  AuditEvent,
  CompensationPackage,
  Employee,
  Role,
  RoleDefinition,
  UserRoleAssignment,
} from "../api/types";
import { Brand } from "../components/Brand";
import { CreateAccountForm } from "../components/CreateAccountForm";
import { EmptyState } from "../components/EmptyState";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../state/auth";

type WorkspaceSection =
    | "Overview"
    | "Appointments"
    | "Visitors"
    | "People"
    | "Compensation"
    | "Audit"
    | "System";

const iconBySection: Record<WorkspaceSection, ComponentType<{ size?: number }>> = {
  Overview: LayoutDashboard,
  Appointments: CalendarCheck2,
  Visitors: DoorOpen,
  People: UsersRound,
  Compensation: CircleDollarSign,
  Audit: FileClock,
  System: Settings2,
};

const roleNames: Record<Role, string> = {
  ROLE_CEO: "CEO",
  ROLE_HR_ADMIN: "HR Admin",
  ROLE_HR_EXECUTIVE: "HR Executive",
  ROLE_EMPLOYEE: "Employee",
  ROLE_RECEPTIONIST: "Reception",
  ROLE_SECURITY: "Security",
  ROLE_SYSTEM_ADMIN: "System Admin",
};

const previewAppointments: Appointment[] = [
  {
    id: "a-1",
    referenceNumber: "BSA-28C194A",
    status: "PENDING_APPROVAL",
    type: "CLIENT_MEETING",
    purpose: "Product discovery and delivery timeline",
    startsAt: "2026-07-29T10:00:00Z",
    endsAt: "2026-07-29T10:30:00Z",
    visitor: {
      id: "v-1", displayName: "Ananya Rao", company: "Example Labs",
      maskedEmail: "a***@example.com", verificationStatus: "OTP_VERIFIED", restricted: false,
    },
    host: { id: "h-1", displayName: "Arjun Mehta", employeeNumber: "BSPL-2026-0014" },
  },
  {
    id: "a-2",
    referenceNumber: "BSA-7BD91F3",
    status: "APPROVED",
    type: "INTERVIEW",
    purpose: "Frontend engineering interview",
    startsAt: "2026-07-29T11:30:00Z",
    endsAt: "2026-07-29T12:00:00Z",
    visitor: {
      id: "v-2", displayName: "Sanjay Verma", company: "Independent",
      maskedEmail: "s***@mail.com", verificationStatus: "IDENTITY_VERIFIED", restricted: false,
    },
    host: { id: "h-2", displayName: "Maya Reddy", employeeNumber: "BSPL-2026-0007" },
  },
  {
    id: "a-3",
    referenceNumber: "BSA-3F81DA2",
    status: "CHECKED_IN",
    type: "VENDOR_VISIT",
    purpose: "Network equipment maintenance",
    startsAt: "2026-07-29T07:30:00Z",
    endsAt: "2026-07-29T08:30:00Z",
    visitor: {
      id: "v-3", displayName: "Fatima Khan", company: "NexGrid Systems",
      maskedEmail: "f***@nexgrid.in", verificationStatus: "IDENTITY_VERIFIED", restricted: false,
    },
    host: { id: "h-3", displayName: "Kiran Shah", employeeNumber: "BSPL-2026-0022" },
  },
];

const previewAccess: AccessRecord[] = [
  {
    id: "access-1", appointmentId: "a-3", visitorName: "Fatima Khan",
    company: "NexGrid Systems", hostName: "Kiran Shah", badgeNumber: "BS-014",
    entryGate: "Main gate", checkedInAt: "2026-07-29T07:26:00Z",
  },
  {
    id: "access-2", appointmentId: "a-4", visitorName: "Vikram Joshi",
    company: "Northstar Legal", hostName: "Maya Reddy", badgeNumber: "BS-021",
    entryGate: "Tower B", checkedInAt: "2026-07-29T08:04:00Z",
  },
];

const previewEmployees = [
  { initials: "AM", name: "Arjun Mehta", id: "BSPL-2026-0014", team: "Engineering", role: "Team Lead", status: "ACTIVE" },
  { initials: "MR", name: "Maya Reddy", id: "BSPL-2026-0007", team: "Human Resources", role: "HR Administrator", status: "ACTIVE" },
  { initials: "KS", name: "Kiran Shah", id: "BSPL-2026-0022", team: "Engineering", role: "Software Engineer", status: "ACTIVE" },
  { initials: "PN", name: "Priya Nair", id: "BSPL-2026-0038", team: "Operations", role: "Receptionist", status: "ONBOARDING" },
];

function sectionsFor(permissions: string[]): WorkspaceSection[] {
  const sections: WorkspaceSection[] = ["Overview"];
  if (permissions.some((item) => item.startsWith("APPOINTMENT_"))) sections.push("Appointments");
  if (permissions.some((item) => item.startsWith("VISITOR_")) || permissions.includes("REPORT_VIEW")) sections.push("Visitors");
  if (permissions.includes("EMPLOYEE_READ")) sections.push("People");
  if (permissions.includes("SALARY_READ")) sections.push("Compensation");
  if (permissions.includes("AUDIT_VIEW")) sections.push("Audit");
  if (permissions.includes("SYSTEM_CONFIGURE") || permissions.includes("ROLE_MANAGE")) sections.push("System");
  return sections;
}

export function DashboardPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user, preview, changePreviewRole, logout } = useAuth();
  const [section, setSection] = useState<WorkspaceSection>("Overview");
  const [mobileMenu, setMobileMenu] = useState(false);
  const [search, setSearch] = useState("");
  const [previewQueue, setPreviewQueue] = useState(previewAppointments);
  const [previewInside, setPreviewInside] = useState(previewAccess);
  const role = user?.roles[0] ?? "ROLE_EMPLOYEE";
  const sections = useMemo(() => sectionsFor(user?.permissions ?? []), [user?.permissions]);

  useEffect(() => {
    if (!sections.includes(section)) setSection("Overview");
  }, [section, sections]);

  const appointmentsQuery = useQuery({
    queryKey: ["appointments"],
    queryFn: api.appointments,
    enabled: !preview && sections.includes("Appointments"),
  });
  const accessQuery = useQuery({
    queryKey: ["visitors-inside"],
    queryFn: api.visitorsInside,
    enabled: !preview && sections.includes("Visitors"),
  });
  const arrivalsQuery = useQuery({
    queryKey: ["arrivals"],
    queryFn: api.arrivals,
    enabled: !preview && user?.permissions.includes("VISITOR_CHECK_IN"),
  });
  const employeesQuery = useQuery({
    queryKey: ["employees"],
    queryFn: api.employees,
    enabled: !preview && sections.includes("People"),
  });
  const compensationQuery = useQuery({
    queryKey: ["compensation-pending"],
    queryFn: api.pendingCompensation,
    enabled: !preview && user?.permissions.includes("SALARY_APPROVE"),
  });
  const auditQuery = useQuery({
    queryKey: ["audit-events"],
    queryFn: api.auditEvents,
    enabled: !preview && sections.includes("Audit"),
  });
  const configurationQuery = useQuery({
    queryKey: ["configuration"],
    queryFn: api.configuration,
    enabled: !preview && user?.permissions.includes("SYSTEM_CONFIGURE"),
  });
  const rolesQuery = useQuery({
    queryKey: ["roles"],
    queryFn: api.roles,
    enabled: !preview && user?.permissions.includes("ROLE_MANAGE"),
  });
  const roleUsersQuery = useQuery({
    queryKey: ["role-users"],
    queryFn: api.roleUsers,
    enabled: !preview && user?.permissions.includes("ROLE_MANAGE"),
  });
  const decision = useMutation({
    mutationFn: ({ id, approve }: { id: string; approve: boolean }) => api.decide(id, approve),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["appointments"] }),
  });
  const checkIn = useMutation({
    mutationFn: ({ id, overrideReason }: { id: string; overrideReason?: string }) =>
        api.checkIn(id, "Main gate", overrideReason),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["arrivals"] }),
        queryClient.invalidateQueries({ queryKey: ["visitors-inside"] }),
      ]);
    },
  });
  const checkOut = useMutation({
    mutationFn: (id: string) => api.checkOut(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["visitors-inside"] }),
  });
  const compensationDecision = useMutation({
    mutationFn: ({ id, approve }: { id: string; approve: boolean }) => api.decideCompensation(id, approve),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["compensation-pending"] }),
  });
  const roleAssignment = useMutation({
    mutationFn: ({ id, roles }: { id: string; roles: Role[] }) => api.replaceRoles(id, roles),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["role-users"] }),
  });

  const appointments = preview ? previewQueue : (appointmentsQuery.data ?? []);
  const access = preview ? previewInside : (accessQuery.data ?? []);
  const arrivals = preview
      ? previewQueue.filter((item) => item.status === "APPROVED")
      : (arrivalsQuery.data ?? []);
  const filteredAppointments = appointments.filter((item) =>
      `${item.visitor.displayName} ${item.referenceNumber} ${item.purpose}`.toLowerCase().includes(search.toLowerCase()),
  );

  const decidePreview = (id: string, approve: boolean) => {
    setPreviewQueue((current) => current.map((item) =>
        item.id === id ? { ...item, status: approve ? "APPROVED" : "REJECTED" } : item,
    ));
  };

  const checkInPreview = (id: string) => {
    const appointment = previewQueue.find((item) => item.id === id);
    if (!appointment) return;
    setPreviewQueue((current) => current.map((item) =>
        item.id === id ? { ...item, status: "CHECKED_IN" } : item,
    ));
    setPreviewInside((current) => [
      ...current,
      {
        id: `access-${id}`,
        appointmentId: id,
        visitorName: appointment.visitor.displayName,
        company: appointment.visitor.company,
        hostName: appointment.host.displayName,
        badgeNumber: `BS-${String(current.length + 30).padStart(3, "0")}`,
        entryGate: "Main gate",
        checkedInAt: new Date().toISOString(),
      },
    ]);
  };

  const checkOutPreview = (recordId: string) => {
    const record = previewInside.find((item) => item.id === recordId);
    setPreviewInside((current) => current.filter((item) => item.id !== recordId));
    if (record) {
      setPreviewQueue((current) => current.map((item) =>
          item.id === record.appointmentId ? { ...item, status: "CHECKED_OUT" } : item,
      ));
    }
  };

  const signOut = async () => {
    await logout();
    navigate("/");
  };

  return (
      <main className="workspace">
        <aside className={`sidebar ${mobileMenu ? "sidebar--open" : ""}`}>
          <div className="sidebar__top">
            <Brand />
            <button className="icon-button mobile-only" onClick={() => setMobileMenu(false)} aria-label="Close navigation"><X size={19} /></button>
          </div>
          <div className="workspace-switcher">
            <span className="avatar avatar--ruby">{user?.displayName.split(" ").map((part) => part[0]).join("").slice(0, 2)}</span>
            <div><small>WORKSPACE</small><strong>{roleNames[role]}</strong></div>
          </div>
          {preview && (
              <label className="preview-role">
                Preview as
                <select value={role} onChange={(event) => changePreviewRole(event.target.value as Role)}>
                  {(Object.keys(roleNames) as Role[]).map((item) => (
                      <option key={item} value={item}>{roleNames[item]}</option>
                  ))}
                </select>
              </label>
          )}
          <nav className="sidebar__nav" aria-label="Workspace navigation">
            <small>MAIN MENU</small>
            {sections.map((item) => {
              const Icon = iconBySection[item];
              return (
                  <button className={section === item ? "active" : ""} key={item} onClick={() => { setSection(item); setMobileMenu(false); }}>
                    <Icon size={18} /><span>{item}</span>
                    {item === "Appointments" && appointments.filter((entry) => entry.status === "PENDING_APPROVAL").length > 0 && (
                        <em>{appointments.filter((entry) => entry.status === "PENDING_APPROVAL").length}</em>
                    )}
                  </button>
              );
            })}
          </nav>
          <div className="sidebar__bottom">
            <button onClick={signOut}><LogOut size={18} /> Sign out</button>
            <div className="secure-session"><ShieldCheck size={16} /><span><strong>Secure session</strong><small>{preview ? "Preview mode" : "Access token protected"}</small></span></div>
          </div>
        </aside>

        <section className="workspace-main">
          <header className="workspace-header">
            <div>
              <button className="icon-button mobile-only" onClick={() => setMobileMenu(true)} aria-label="Open navigation"><Menu size={20} /></button>
              <span className="workspace-header__crumb">BrainServe / {roleNames[role]}</span>
            </div>
            <div className="workspace-header__actions">
              {preview && <span className="preview-pill">Preview data</span>}
              <span className="workspace-user"><span className="avatar avatar--soft">{user?.displayName.split(" ").map((part) => part[0]).join("").slice(0, 2)}</span><span><strong>{user?.displayName}</strong><small>{roleNames[role]}</small></span></span>
            </div>
          </header>

          <div className="workspace-content">
            {section === "Overview" && (
                <Overview
                    role={role}
                    name={user?.displayName.split(" ")[0] ?? "there"}
                    appointments={appointments}
                    access={access}
                    onOpen={setSection}
                />
            )}
            {section === "Appointments" && (
                <AppointmentsPanel
                    rows={filteredAppointments}
                    search={search}
                    setSearch={setSearch}
                    loading={appointmentsQuery.isLoading}
                    onDecision={(id, approve) => preview ? decidePreview(id, approve) : decision.mutate({ id, approve })}
                />
            )}
            {section === "Visitors" && (
                <VisitorsPanel
                    rows={access}
                    arrivals={arrivals}
                    loading={accessQuery.isLoading || arrivalsQuery.isLoading}
                    onCheckIn={(id, overrideReason) => preview ? checkInPreview(id) : checkIn.mutate({ id, overrideReason })}
                    onCheckOut={(id) => preview ? checkOutPreview(id) : checkOut.mutate(id)}
                    checking={checkIn.isPending || checkOut.isPending}
                    canCheckIn={Boolean(preview || user?.permissions.includes("VISITOR_CHECK_IN"))}
                    canCheckOut={Boolean(preview || user?.permissions.includes("VISITOR_CHECK_OUT"))}
                />
            )}
            {section === "People" && (
                <PeoplePanel
                    rows={preview ? [] : (employeesQuery.data ?? [])}
                    loading={employeesQuery.isLoading}
                    preview={preview}
                />
            )}
            {section === "Compensation" && (
                <CompensationPanel
                    rows={compensationQuery.data ?? []}
                    loading={compensationQuery.isLoading}
                    preview={preview}
                    canApprove={Boolean(preview || user?.permissions.includes("SALARY_APPROVE"))}
                    onDecision={(id, approve) => compensationDecision.mutate({ id, approve })}
                />
            )}
            {section === "Audit" && (
                <AuditPanel rows={auditQuery.data ?? []} loading={auditQuery.isLoading} preview={preview} />
            )}
            {section === "System" && (
                <SystemPanel
                    values={configurationQuery.data ?? {}}
                    roles={rolesQuery.data ?? []}
                    users={roleUsersQuery.data ?? []}
                    currentUserId={user?.id ?? ""}
                    loading={configurationQuery.isLoading || rolesQuery.isLoading || roleUsersQuery.isLoading}
                    preview={preview}
                    onAssign={(id, roles) => roleAssignment.mutate({ id, roles })}
                />
            )}
          </div>
        </section>
      </main>
  );
}

function Overview({
                    role,
                    name,
                    appointments,
                    access,
                    onOpen,
                  }: {
  role: Role;
  name: string;
  appointments: Appointment[];
  access: AccessRecord[];
  onOpen: (section: WorkspaceSection) => void;
}) {
  const metrics = [
    { label: "Expected", value: appointments.filter((item) => ["APPROVED", "CHECKED_IN"].includes(item.status)).length, change: "Approved or arrived", icon: CalendarCheck2 },
    { label: "Pending decisions", value: appointments.filter((item) => item.status === "PENDING_APPROVAL").length, change: "Needs attention", icon: Clock3 },
    { label: "Visitors inside", value: access.length, change: "Live occupancy", icon: DoorOpen },
    { label: "Visible appointments", value: appointments.length, change: roleNames[role], icon: Activity },
  ];
  return (
      <>
        <div className="workspace-title">
          <div><small>WEDNESDAY, 29 JULY</small><h1>Good afternoon, {name}.</h1><p>Here’s what needs your attention across BrainServe today.</p></div>
        </div>
        <div className="metrics-grid">
          {metrics.map(({ label, value, change, icon: Icon }) => (
              <article className="metric-card glass-card" key={label}>
                <span className="metric-card__icon"><Icon size={20} /></span>
                <small>{label}</small><strong>{String(value).padStart(2, "0")}</strong><span>{change}</span>
              </article>
          ))}
        </div>
        <div className="dashboard-grid">
          <section className="panel glass-card panel--wide">
            <div className="panel__heading">
              <div><small>LIVE QUEUE</small><h2>Appointments requiring action</h2></div>
              {appointments.length > 0 && <button className="text-link" onClick={() => onOpen("Appointments")}>View all</button>}
            </div>
            {appointments.filter((item) => item.status === "PENDING_APPROVAL").slice(0, 3).map((item) => (
                <div className="queue-row" key={item.id}>
                  <span className="time-box"><strong>{new Date(item.startsAt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}</strong><small>Today</small></span>
                  <span className="avatar avatar--soft">{item.visitor.displayName.split(" ").map((part) => part[0]).join("").slice(0, 2)}</span>
                  <div className="queue-row__main"><strong>{item.visitor.displayName}</strong><small>{item.type.replaceAll("_", " ")} · {item.visitor.company}</small></div>
                  <StatusBadge status={item.status} />
                </div>
            ))}
            {!appointments.some((item) => item.status === "PENDING_APPROVAL") && (
                <EmptyState title="Your queue is clear" detail="New approval requests will appear here." />
            )}
          </section>
          <section className="panel glass-card">
            <div className="panel__heading"><div><small>OCCUPANCY</small><h2>Visitors inside</h2></div><span className="live-dot">Live</span></div>
            <div className="occupancy-ring"><div><strong>{access.length}</strong><span>currently inside</span></div></div>
            <div className="mini-list">
              {access.slice(0, 3).map((item) => (
                  <div key={item.id}><span className="avatar avatar--mini">{item.visitorName.split(" ").map((part) => part[0]).join("").slice(0, 2)}</span><span><strong>{item.visitorName}</strong><small>Badge {item.badgeNumber}</small></span><small>{Math.max(1, Math.round((Date.now() - new Date(item.checkedInAt).getTime()) / 60_000))}m</small></div>
              ))}
            </div>
          </section>
          <section className="panel glass-card">
            <div className="panel__heading"><div><small>ACTIVITY</small><h2>Recent changes</h2></div></div>
            <div className="activity-list">
              {appointments.filter((item) => item.status === "APPROVED").slice(0, 2).map((item) => (
                  <div key={item.id}><span><Check size={14} /></span><p><strong>Appointment approved</strong><small>{item.visitor.displayName} · {item.referenceNumber}</small></p></div>
              ))}
              {access.slice(0, 2).map((item) => (
                  <div key={item.id}><span><DoorOpen size={14} /></span><p><strong>Visitor checked in</strong><small>{item.visitorName} · Badge {item.badgeNumber}</small></p></div>
              ))}
              {appointments.every((item) => item.status !== "APPROVED") && access.length === 0 && (
                  <p className="panel-copy">No recent appointment or access activity.</p>
              )}
            </div>
          </section>
        </div>
      </>
  );
}

function PanelTitle({ kicker, title, detail }: { kicker: string; title: string; detail: string }) {
  return <div className="workspace-title workspace-title--panel"><div><small>{kicker}</small><h1>{title}</h1><p>{detail}</p></div></div>;
}

function AppointmentsPanel({
                             rows,
                             search,
                             setSearch,
                             loading,
                             onDecision,
                           }: {
  rows: Appointment[];
  search: string;
  setSearch: (value: string) => void;
  loading: boolean;
  onDecision: (id: string, approve: boolean) => void;
}) {
  return (
      <>
        <PanelTitle kicker="HOST WORKSPACE" title="Appointment queue" detail="Review verified requests assigned to you or your authorized team." />
        <section className="panel glass-card data-panel">
          <div className="data-toolbar">
            <label className="search-field"><Search size={17} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search visitor or reference" /></label>
          </div>
          {loading ? <div className="table-loading">Loading appointment queue…</div> : rows.length === 0 ? (
              <EmptyState title="No matching appointments" detail="Try a different search or wait for a new request." />
          ) : (
              <div className="data-table-wrap">
                <table className="data-table">
                  <thead><tr><th>Visitor</th><th>Visit</th><th>Host</th><th>Time</th><th>Status</th><th><span className="sr-only">Actions</span></th></tr></thead>
                  <tbody>
                  {rows.map((item) => (
                      <tr key={item.id}>
                        <td><span className="person-cell"><span className="avatar avatar--mini">{item.visitor.displayName.split(" ").map((part) => part[0]).join("").slice(0, 2)}</span><span><strong>{item.visitor.displayName}</strong><small>{item.visitor.company || item.visitor.maskedEmail}</small></span></span></td>
                        <td><strong className="table-main">{item.type.replaceAll("_", " ")}</strong><small>{item.referenceNumber}</small></td>
                        <td>{item.host.displayName}</td>
                        <td><strong className="table-main">{new Date(item.startsAt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}</strong><small>{new Date(item.startsAt).toLocaleDateString("en-IN", { day: "2-digit", month: "short" })}</small></td>
                        <td><StatusBadge status={item.status} /></td>
                        <td>
                          {item.status === "PENDING_APPROVAL" ? (
                              <span className="row-actions"><button className="approve-button" onClick={() => onDecision(item.id, true)} aria-label={`Approve ${item.visitor.displayName}`}><Check size={16} /></button><button className="reject-button" onClick={() => onDecision(item.id, false)} aria-label={`Reject ${item.visitor.displayName}`}><X size={16} /></button></span>
                          ) : <span className="table-main">—</span>}
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </div>
          )}
        </section>
      </>
  );
}

function VisitorsPanel({
                         rows,
                         arrivals,
                         loading,
                         onCheckIn,
                         onCheckOut,
                         checking,
                         canCheckIn,
                         canCheckOut,
                       }: {
  rows: AccessRecord[];
  arrivals: Appointment[];
  loading: boolean;
  onCheckIn: (id: string, overrideReason?: string) => void;
  onCheckOut: (id: string) => void;
  checking: boolean;
  canCheckIn: boolean;
  canCheckOut: boolean;
}) {
  return (
      <>
        <PanelTitle kicker="LIVE ACCESS" title="Visitors currently inside" detail="This same verified list powers the security emergency view." />
        {canCheckIn && (
            <section className="panel glass-card data-panel">
              <div className="panel__heading"><div><small>ARRIVAL QUEUE</small><h2>Approved visitors</h2></div><span className="status status--approved"><span />{arrivals.length} ready</span></div>
              {arrivals.length === 0 ? (
                  <EmptyState title="No arrivals waiting" detail="Approved appointments will appear here for reception check-in." />
              ) : (
                  <div className="data-table-wrap">
                    <table className="data-table">
                      <thead><tr><th>Visitor</th><th>Reference</th><th>Host</th><th>Time</th><th>Action</th></tr></thead>
                      <tbody>{arrivals.map((item) => (
                          <tr key={item.id}>
                            <td><strong>{item.visitor.displayName}</strong><small>{item.visitor.company || item.visitor.maskedEmail}</small></td>
                            <td>{item.referenceNumber}</td>
                            <td>{item.host.displayName}</td>
                            <td>{new Date(item.startsAt).toLocaleString("en-IN", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" })}</td>
                            <td><button className="button button--primary button--small" disabled={checking} onClick={() => {
                              if (!item.visitor.restricted) {
                                onCheckIn(item.id);
                                return;
                              }
                              const reason = window.prompt("This visitor is restricted. Enter the authorized override reason:");
                              if (reason?.trim()) onCheckIn(item.id, reason.trim());
                            }}>Check in</button></td>
                          </tr>
                      ))}</tbody>
                    </table>
                  </div>
              )}
            </section>
        )}
        <section className="panel glass-card data-panel">
          <div className="data-toolbar"><span className="live-dot">Live occupancy · {rows.length}</span></div>
          {loading ? <div className="table-loading">Loading live access records…</div> : rows.length === 0 ? (
              <EmptyState title="The office is clear" detail="Checked-in visitors will appear here immediately." />
          ) : (
              <div className="data-table-wrap">
                <table className="data-table">
                  <thead><tr><th>Visitor</th><th>Host</th><th>Badge</th><th>Entry</th><th>Duration</th><th>Action</th></tr></thead>
                  <tbody>{rows.map((item) => (
                      <tr key={item.id}>
                        <td><span className="person-cell"><span className="avatar avatar--mini">{item.visitorName.split(" ").map((part) => part[0]).join("").slice(0, 2)}</span><span><strong>{item.visitorName}</strong><small>{item.company || "Guest"}</small></span></span></td>
                        <td>{item.hostName}</td><td><span className="badge-number">{item.badgeNumber}</span></td>
                        <td><strong className="table-main">{item.entryGate}</strong><small>{new Date(item.checkedInAt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}</small></td>
                        <td>{Math.max(1, Math.round((Date.now() - new Date(item.checkedInAt).getTime()) / 60_000))} min</td>
                        <td>{canCheckOut ? <button className="button button--ghost button--small" disabled={checking} onClick={() => onCheckOut(item.id)}>Check out</button> : "—"}</td>
                      </tr>
                  ))}</tbody>
                </table>
              </div>
          )}
        </section>
      </>
  );
}

function PeoplePanel({ rows, loading, preview }: { rows: Employee[]; loading: boolean; preview: boolean }) {
  const [query, setQuery] = useState("");
  const actualRows = rows.filter((employee) =>
      `${employee.displayName} ${employee.employeeNumber} ${employee.officialEmail}`.toLowerCase().includes(query.toLowerCase()),
  );
  return (
      <>
        <PanelTitle kicker="PEOPLE OPERATIONS" title="Employee directory" detail="Directory-safe employment information. Compensation remains in its protected module." />
        <section className="panel glass-card data-panel">
          <div className="data-toolbar"><label className="search-field"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search employee or email" /></label></div>
          {loading ? <div className="table-loading">Loading employee directory…</div> : <div className="data-table-wrap">
            <table className="data-table"><thead><tr><th>Employee</th><th>ID</th><th>Department</th><th>Designation</th><th>Status</th><th /></tr></thead>
              <tbody>{preview ? previewEmployees.filter((employee) => `${employee.name} ${employee.id} ${employee.team}`.toLowerCase().includes(query.toLowerCase())).map((employee) => (
                  <tr key={employee.id}><td><span className="person-cell"><span className="avatar avatar--mini">{employee.initials}</span><span><strong>{employee.name}</strong><small>{employee.name.toLowerCase().replace(" ", ".")}@brainserve.in</small></span></span></td><td>{employee.id}</td><td>{employee.team}</td><td>{employee.role}</td><td><StatusBadge status={employee.status} /></td><td>—</td></tr>
              )) : actualRows.map((employee) => (
                  <tr key={employee.id}><td><span className="person-cell"><span className="avatar avatar--mini">{employee.displayName.split(" ").map((part) => part[0]).join("").slice(0, 2)}</span><span><strong>{employee.displayName}</strong><small>{employee.officialEmail}</small></span></span></td><td>{employee.employeeNumber}</td><td title={employee.departmentId}>{employee.departmentId.slice(0, 8)}…</td><td title={employee.designationId}>{employee.designationId.slice(0, 8)}…</td><td><StatusBadge status={employee.status} /></td><td>—</td></tr>
              ))}</tbody>
            </table>
          </div>}
        </section>
      </>
  );
}

function CompensationPanel({
                             rows,
                             loading,
                             preview,
                             canApprove,
                             onDecision,
                           }: {
  rows: CompensationPackage[];
  loading: boolean;
  preview: boolean;
  canApprove: boolean;
  onDecision: (id: string, approve: boolean) => void;
}) {
  const money = (value: number, currency: string) => new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(value);
  return (
      <>
        <PanelTitle kicker="RESTRICTED · AUDITED" title="Compensation reviews" detail="Every read and decision is permission checked and recorded." />
        <div className="sensitive-banner"><ShieldCheck size={19} /><span><strong>Confidential workspace</strong><small>Proposers cannot approve their own requests. System administrators have no salary permission.</small></span></div>
        <div className="dashboard-grid">
          <section className="panel glass-card panel--wide">
            <div className="panel__heading"><div><small>MAKER-CHECKER</small><h2>Pending salary changes</h2></div><span className="status status--pending_approval"><span />{preview ? 2 : rows.length} awaiting review</span></div>
            {preview ? (
                <>
                  <div className="salary-request"><span className="avatar avatar--soft">KS</span><div><strong>Kiran Shah</strong><small>Effective 01 Aug 2026 · Proposed by Maya Reddy</small></div><strong>₹ 9,00,000</strong><span className="preview-pill">Preview</span></div>
                  <div className="salary-request"><span className="avatar avatar--soft">PN</span><div><strong>Priya Nair</strong><small>Effective 15 Aug 2026 · Proposed by Maya Reddy</small></div><strong>₹ 6,60,000</strong><span className="preview-pill">Preview</span></div>
                </>
            ) : loading ? <div className="table-loading">Loading restricted requests…</div> : !canApprove ? (
                <EmptyState title="Read access is protected" detail="Select an employee through the compensation API to view an audited current package." />
            ) : rows.length === 0 ? (
                <EmptyState title="No reviews pending" detail="New maker-checker requests will appear here." />
            ) : rows.map((item) => (
                <div className="salary-request" key={item.id}>
                  <span className="avatar avatar--soft">₹</span>
                  <div><strong>{item.employeeId.slice(0, 8)}…</strong><small>Effective {new Date(`${item.effectiveFrom}T00:00:00`).toLocaleDateString("en-IN")} · Proposed by {item.proposedBy.slice(0, 8)}…</small></div>
                  <strong>{money(item.annualCtc, item.currency)}</strong>
                  <span className="row-actions"><button className="approve-button" onClick={() => onDecision(item.id, true)} aria-label="Approve compensation"><Check size={16} /></button><button className="reject-button" onClick={() => onDecision(item.id, false)} aria-label="Reject compensation"><X size={16} /></button></span>
                </div>
            ))}
          </section>
          <section className="panel glass-card"><div className="panel__heading"><div><small>CONTROL</small><h2>Access hygiene</h2></div></div><div className="control-score"><strong>100%</strong><span>Salary reads audited</span></div><p className="panel-copy">No salary permission is inherited by the System Administrator role.</p></section>
        </div>
      </>
  );
}

const previewAudit: AuditEvent[] = [
  { id: "audit-1", eventType: "DOMAIN_ACTION", action: "APPOINTMENT_APPROVE", entityType: "APPOINTMENT", entityId: "BSA-7BD91F3", sensitivity: "CONFIDENTIAL", occurredAt: "2026-07-29T09:12:00Z" },
  { id: "audit-2", eventType: "DOMAIN_ACTION", action: "VISITOR_CHECK_IN", entityType: "VISIT_ACCESS", entityId: "access-1", sensitivity: "CONFIDENTIAL", occurredAt: "2026-07-29T08:54:00Z" },
  { id: "audit-3", eventType: "DOMAIN_ACTION", action: "COMPENSATION_READ", entityType: "COMPENSATION", entityId: "package-1", sensitivity: "RESTRICTED", occurredAt: "2026-07-29T08:37:00Z" },
];

function AuditPanel({ rows, loading, preview }: { rows: AuditEvent[]; loading: boolean; preview: boolean }) {
  const [query, setQuery] = useState("");
  const displayRows = (preview ? previewAudit : rows).filter((item) =>
      `${item.action} ${item.entityType} ${item.entityId ?? ""} ${item.correlationId ?? ""}`.toLowerCase().includes(query.toLowerCase()),
  );
  const exportAudit = () => {
    const body = ["occurredAt,action,entityType,entityId,sensitivity,correlationId", ...displayRows.map((item) =>
        [item.occurredAt, item.action, item.entityType, item.entityId ?? "", item.sensitivity, item.correlationId ?? ""]
            .map((value) => `"${String(value).replaceAll("\"", "\"\"")}"`).join(","),
    )].join("\n");
    const url = URL.createObjectURL(new Blob([body], { type: "text/csv" }));
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "brainserve-audit.csv";
    anchor.click();
    URL.revokeObjectURL(url);
  };
  return (
      <>
        <PanelTitle kicker="COMPLIANCE" title="Audit activity" detail="Append-only evidence for privileged actions and sensitive access." />
        <section className="panel glass-card data-panel">
          <div className="data-toolbar"><label className="search-field"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search action, entity, or correlation" /></label><button className="button button--ghost button--small" disabled={displayRows.length === 0} onClick={exportAudit}>Export CSV</button></div>
          {loading ? <div className="table-loading">Loading audit evidence…</div> : <div className="activity-list activity-list--large">
            {displayRows.map((item) => (
                <div key={item.id}><span><ClipboardCheck size={14} /></span><p><strong>{item.action}</strong><small>{item.entityType} · {item.entityId ?? "—"} · {item.sensitivity}</small></p><time>{new Date(item.occurredAt).toLocaleString("en-IN")}</time></div>
            ))}
            {displayRows.length === 0 && <EmptyState title="No audit events found" detail="Try a different search." />}
          </div>}
        </section>
      </>
  );
}

const previewRoles: RoleDefinition[] = (Object.entries(roleNames) as Array<[Role, string]>).map(([code, name], index) => ({
  id: `role-${index}`,
  code,
  name,
  systemRole: true,
  permissions: [],
}));

const previewRoleUsers: UserRoleAssignment[] = [
  { id: "preview-user-1", login: "maya.reddy@brainserve.in", displayName: "Maya Reddy", enabled: true, roles: ["ROLE_HR_ADMIN"] },
  { id: "preview-user-2", login: "priya.nair@brainserve.in", displayName: "Priya Nair", enabled: true, roles: ["ROLE_RECEPTIONIST"] },
];

function SystemPanel({
                       values,
                       roles,
                       users,
                       currentUserId,
                       loading,
                       preview,
                       onAssign,
                     }: {
  values: Record<string, unknown>;
  roles: RoleDefinition[];
  users: UserRoleAssignment[];
  currentUserId: string;
  loading: boolean;
  preview: boolean;
  onAssign: (id: string, roles: Role[]) => void;
}) {
  const [drafts, setDrafts] = useState<Record<string, Role>>({});
  const display = preview ? {
    "office.timezone": "Asia/Kolkata",
    "appointment.slotMinutes": 30,
    "visitor.retentionDays": 365,
    "salary.makerChecker": true,
  } : values;
  const displayRoles = preview ? previewRoles : roles;
  const displayUsers = preview ? previewRoleUsers : users;
  return (
      <>
        <PanelTitle kicker="TECHNICAL ADMINISTRATION" title="System configuration" detail="Operational policy and service health—without implicit compensation access." />
        {!preview && <CreateAccountForm />}
        {loading ? <div className="table-loading">Loading configuration…</div> : <div className="settings-grid">
          {Object.entries(display).map(([key, value]) => (
              <article className="setting-card glass-card" key={key}><span><Settings2 size={19} /></span><small>{key}</small><strong>{typeof value === "string" ? value : JSON.stringify(value)}</strong><p>Versioned and restricted to `SYSTEM_CONFIGURE`.</p></article>
          ))}
        </div>}
        <section className="panel glass-card data-panel">
          <div className="panel__heading"><div><small>LEAST PRIVILEGE</small><h2>User role assignments</h2></div><span className="status status--active"><span />{displayRoles.length} roles</span></div>
          {displayUsers.length === 0 ? (
              <EmptyState title="No users available" detail="Bootstrap or onboard a user to manage role assignments." />
          ) : (
              <div className="data-table-wrap">
                <table className="data-table">
                  <thead><tr><th>User</th><th>Login</th><th>Role</th><th>Permissions</th><th>Action</th></tr></thead>
                  <tbody>{displayUsers.map((account) => {
                    const selected = drafts[account.id] ?? account.roles[0] ?? "ROLE_EMPLOYEE";
                    const permissionCount = displayRoles.find((role) => role.code === selected)?.permissions.length ?? 0;
                    const ownAccount = account.id === currentUserId;
                    return (
                        <tr key={account.id}>
                          <td><strong>{account.displayName}</strong><small>{account.enabled ? "Enabled" : "Disabled"}</small></td>
                          <td>{account.login}</td>
                          <td><select value={selected} disabled={preview || ownAccount} onChange={(event) => setDrafts((current) => ({ ...current, [account.id]: event.target.value as Role }))}>{displayRoles.map((role) => <option key={role.code} value={role.code}>{role.name}</option>)}</select></td>
                          <td>{permissionCount || (preview ? "Role-scoped" : "No")} permissions</td>
                          <td>{preview ? <span className="preview-pill">Preview</span> : ownAccount ? <small>Self-change blocked</small> : <button className="button button--ghost button--small" onClick={() => onAssign(account.id, [selected])}>Apply</button>}</td>
                        </tr>
                    );
                  })}</tbody>
                </table>
              </div>
          )}
        </section>
      </>
  );
}