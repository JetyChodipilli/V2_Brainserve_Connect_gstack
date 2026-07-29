import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Copy, ShieldCheck, UserPlus } from "lucide-react";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { api } from "../api/client";
import type { CreateAccountPayload, ProvisionableRole } from "../api/types";

const roleOptions: Array<{ value: ProvisionableRole; label: string; detail: string }> = [
    { value: "ROLE_EMPLOYEE", label: "Employee", detail: "Standard employee workspace" },
    { value: "ROLE_SECURITY", label: "Security", detail: "Visitor verification and gate access" },
    { value: "ROLE_RECEPTIONIST", label: "Receptionist", detail: "Arrival desk and visitor coordination" },
];

const initialForm: CreateAccountPayload = {
    firstName: "",
    middleName: "",
    lastName: "",
    officialEmail: "",
    personalEmail: "",
    phone: "",
    branchId: "",
    departmentId: "",
    designationId: "",
    employmentType: "FULL_TIME",
    joiningDate: new Date().toISOString().slice(0, 10),
    workLocation: "Hyderabad",
    role: "ROLE_EMPLOYEE",
};

export function CreateAccountForm() {
    const queryClient = useQueryClient();
    const [form, setForm] = useState<CreateAccountPayload>(initialForm);
    const [copied, setCopied] = useState(false);

    const branches = useQuery({ queryKey: ["branches"], queryFn: api.branches });
    const departments = useQuery({ queryKey: ["departments"], queryFn: api.departments });
    const designations = useQuery({ queryKey: ["designations"], queryFn: api.designations });

    const visibleDepartments = useMemo(
        () => (departments.data ?? []).filter((item) => item.active && item.branchId === form.branchId),
        [departments.data, form.branchId],
    );
    const visibleDesignations = useMemo(
        () => (designations.data ?? []).filter(
            (item) => item.active && (!item.departmentId || item.departmentId === form.departmentId),
        ),
        [designations.data, form.departmentId],
    );

    useEffect(() => {
        if (form.departmentId && !visibleDepartments.some((item) => item.id === form.departmentId)) {
            setForm((current) => ({ ...current, departmentId: "", designationId: "" }));
        }
    }, [form.departmentId, visibleDepartments]);

    useEffect(() => {
        if (form.designationId && !visibleDesignations.some((item) => item.id === form.designationId)) {
            setForm((current) => ({ ...current, designationId: "" }));
        }
    }, [form.designationId, visibleDesignations]);

    const creation = useMutation({
        mutationFn: api.createAccount,
        onSuccess: async () => {
            setCopied(false);
            await Promise.all([
                queryClient.invalidateQueries({ queryKey: ["employees"] }),
                queryClient.invalidateQueries({ queryKey: ["role-users"] }),
            ]);
        },
    });

    const update = <K extends keyof CreateAccountPayload>(key: K, value: CreateAccountPayload[K]) => {
        setForm((current) => ({ ...current, [key]: value }));
        if (creation.isSuccess || creation.isError) creation.reset();
    };

    const submit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        creation.mutate({
            ...form,
            middleName: form.middleName?.trim() || undefined,
            personalEmail: form.personalEmail?.trim() || undefined,
            phone: form.phone?.trim() || undefined,
            workLocation: form.workLocation?.trim() || undefined,
        });
    };

    const copyPassword = async () => {
        if (!creation.data?.temporaryPassword) return;
        await navigator.clipboard.writeText(creation.data.temporaryPassword);
        setCopied(true);
    };

    const loadingOrganization = branches.isLoading || departments.isLoading || designations.isLoading;
    const organizationError = branches.error || departments.error || designations.error;

    return (
        <section className="panel glass-card account-creator">
            <div className="panel__heading">
                <div>
                    <small>IDENTITY PROVISIONING</small>
                    <h2>Create an account</h2>
                </div>
                <span className="status status--active"><span />Audited action</span>
            </div>

            <div className="account-creator__intro">
                <span><UserPlus size={21} /></span>
                <div>
                    <strong>Employee profile and login are created together</strong>
                    <p>The new user receives the selected role and must change the temporary password at first login.</p>
                </div>
            </div>

            {organizationError && (
                <div className="form-message form-message--error" role="alert">
                    Organization data could not be loaded. Refresh the page and try again.
                </div>
            )}

            <form onSubmit={submit}>
                <div className="account-form__grid">
                    <label>
                        First name
                        <input
                            required
                            maxLength={80}
                            value={form.firstName}
                            onChange={(event) => update("firstName", event.target.value)}
                            autoComplete="given-name"
                        />
                    </label>
                    <label>
                        Middle name <span className="optional">Optional</span>
                        <input
                            maxLength={80}
                            value={form.middleName ?? ""}
                            onChange={(event) => update("middleName", event.target.value)}
                            autoComplete="additional-name"
                        />
                    </label>
                    <label>
                        Last name
                        <input
                            required
                            maxLength={80}
                            value={form.lastName}
                            onChange={(event) => update("lastName", event.target.value)}
                            autoComplete="family-name"
                        />
                    </label>

                    <label>
                        Official email
                        <input
                            required
                            type="email"
                            maxLength={180}
                            value={form.officialEmail}
                            onChange={(event) => update("officialEmail", event.target.value)}
                            autoComplete="email"
                        />
                    </label>
                    <label>
                        Personal email <span className="optional">Optional</span>
                        <input
                            type="email"
                            maxLength={180}
                            value={form.personalEmail ?? ""}
                            onChange={(event) => update("personalEmail", event.target.value)}
                        />
                    </label>
                    <label>
                        Phone <span className="optional">Optional</span>
                        <input
                            maxLength={30}
                            value={form.phone ?? ""}
                            onChange={(event) => update("phone", event.target.value)}
                            autoComplete="tel"
                        />
                    </label>

                    <label>
                        Account role
                        <select
                            required
                            value={form.role}
                            onChange={(event) => update("role", event.target.value as ProvisionableRole)}
                        >
                            {roleOptions.map((item) => (
                                <option key={item.value} value={item.value}>{item.label} — {item.detail}</option>
                            ))}
                        </select>
                    </label>
                    <label>
                        Branch
                        <select
                            required
                            disabled={loadingOrganization}
                            value={form.branchId}
                            onChange={(event) => update("branchId", event.target.value)}
                        >
                            <option value="">Select branch</option>
                            {(branches.data ?? []).filter((item) => item.active).map((item) => (
                                <option key={item.id} value={item.id}>{item.name} · {item.city}</option>
                            ))}
                        </select>
                    </label>
                    <label>
                        Department
                        <select
                            required
                            disabled={!form.branchId || loadingOrganization}
                            value={form.departmentId}
                            onChange={(event) => update("departmentId", event.target.value)}
                        >
                            <option value="">Select department</option>
                            {visibleDepartments.map((item) => (
                                <option key={item.id} value={item.id}>{item.name}</option>
                            ))}
                        </select>
                    </label>

                    <label>
                        Designation
                        <select
                            required
                            disabled={!form.departmentId || loadingOrganization}
                            value={form.designationId}
                            onChange={(event) => update("designationId", event.target.value)}
                        >
                            <option value="">Select designation</option>
                            {visibleDesignations.map((item) => (
                                <option key={item.id} value={item.id}>{item.name}</option>
                            ))}
                        </select>
                    </label>
                    <label>
                        Employment type
                        <select value={form.employmentType} onChange={(event) => update("employmentType", event.target.value)}>
                            <option value="FULL_TIME">Full time</option>
                            <option value="PART_TIME">Part time</option>
                            <option value="CONTRACT">Contract</option>
                            <option value="INTERN">Intern</option>
                        </select>
                    </label>
                    <label>
                        Joining date
                        <input
                            required
                            type="date"
                            value={form.joiningDate}
                            onChange={(event) => update("joiningDate", event.target.value)}
                        />
                    </label>

                    <label className="account-form__wide">
                        Work location <span className="optional">Optional</span>
                        <input
                            maxLength={160}
                            value={form.workLocation ?? ""}
                            onChange={(event) => update("workLocation", event.target.value)}
                        />
                    </label>
                </div>

                {creation.isError && (
                    <div className="form-message form-message--error" role="alert">{creation.error.message}</div>
                )}

                {creation.data && (
                    <div className="account-created" role="status">
                        <span className="account-created__icon"><Check size={20} /></span>
                        <div>
                            <strong>{creation.data.employee.displayName} is ready</strong>
                            <p>
                                Login: <b>{creation.data.employee.officialEmail}</b> · Employee ID:{" "}
                                <b>{creation.data.employee.employeeNumber}</b>
                            </p>
                            <div className="temporary-password">
                                <span><small>Temporary password</small><code>{creation.data.temporaryPassword}</code></span>
                                <button type="button" className="button button--ghost button--small" onClick={copyPassword}>
                                    {copied ? <Check size={15} /> : <Copy size={15} />}
                                    {copied ? "Copied" : "Copy"}
                                </button>
                            </div>
                            <small>Copy this password now. It is not returned again.</small>
                        </div>
                    </div>
                )}

                <div className="account-form__actions">
                    <span><ShieldCheck size={16} /> Role assignment and creator identity are recorded in the audit log.</span>
                    <button
                        className="button button--primary"
                        disabled={creation.isPending || loadingOrganization || Boolean(organizationError)}
                        type="submit"
                    >
                        <UserPlus size={17} />
                        {creation.isPending ? "Creating account…" : "Create account"}
                    </button>
                </div>
            </form>
        </section>
    );
}