import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  Building2,
  CalendarDays,
  Check,
  Clock3,
  Mail,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { useMemo, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { Link } from "../routing";
import { z } from "zod";
import { api, ApiProblem } from "../api/client";
import type { BookingResult, Host, Slot } from "../api/types";
import { Brand } from "../components/Brand";

const schema = z.object({
  hostEmployeeId: z.string().min(1, "Choose a host"),
  type: z.string().min(1, "Choose a visit type"),
  date: z.string().min(1, "Choose a date"),
  slot: z.string().min(1, "Choose an available time"),
  purpose: z.string().min(5, "Add a little more detail").max(500),
  firstName: z.string().min(1, "First name is required").max(80),
  lastName: z.string().min(1, "Last name is required").max(80),
  email: z.email("Enter a valid email"),
  phone: z.string().regex(/^[+0-9() -]{8,24}$/, "Enter a valid phone number"),
  company: z.string().max(180).optional(),
  consent: z.literal(true, { error: "Consent is required to request a visit" }),
});

type BookingValues = z.infer<typeof schema>;

const demoHosts: Host[] = [
  { id: "demo-arjun", displayName: "Arjun Mehta", departmentId: "Engineering", designationId: "Team Lead" },
  { id: "demo-maya", displayName: "Maya Reddy", departmentId: "Human Resources", designationId: "HR Administrator" },
  { id: "demo-ananya", displayName: "Ananya Rao", departmentId: "Executive Office", designationId: "Chief Executive Officer" },
];

function demoSlots(date: string): Slot[] {
  if (!date) return [];
  return ["10:00", "11:10", "14:00", "15:10", "16:20"].map((time) => {
    const startsAt = new Date(`${date}T${time}:00+05:30`);
    return {
      startsAt: startsAt.toISOString(),
      endsAt: new Date(startsAt.getTime() + 30 * 60_000).toISOString(),
    };
  }).filter((slot) => new Date(slot.startsAt) > new Date());
}

export function BookingPage() {
  const [step, setStep] = useState(1);
  const [result, setResult] = useState<BookingResult | null>(null);
  const form = useForm<BookingValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      hostEmployeeId: "",
      type: "CLIENT_MEETING",
      date: new Date(Date.now() + 86_400_000).toISOString().slice(0, 10),
      slot: "",
      purpose: "",
      firstName: "",
      lastName: "",
      email: "",
      phone: "+91 ",
      company: "",
      consent: false as true,
    },
  });
  const hostId = useWatch({ control: form.control, name: "hostEmployeeId" });
  const date = useWatch({ control: form.control, name: "date" });
  const selectedSlot = useWatch({ control: form.control, name: "slot" });
  const hostsQuery = useQuery({ queryKey: ["public-hosts"], queryFn: api.hosts });
  const hosts = hostsQuery.data?.length ? hostsQuery.data : demoHosts;
  const isPreviewData = !hostsQuery.data;
  const slotsQuery = useQuery({
    queryKey: ["slots", hostId, date],
    queryFn: () => api.slots(hostId, date),
    enabled: Boolean(hostId && date && !hostId.startsWith("demo-")),
  });
  const slots = hostId.startsWith("demo-") || slotsQuery.isError ? demoSlots(date) : (slotsQuery.data ?? []);

  const submission = useMutation({
    mutationFn: async (values: BookingValues) => {
      const [startsAt, endsAt] = values.slot.split("|");
      if (!startsAt || !endsAt) throw new Error("Select a valid slot");
      if (values.hostEmployeeId.startsWith("demo-")) {
        await new Promise((resolve) => setTimeout(resolve, 500));
        return {
          referenceNumber: `BSA-DEMO${Math.floor(1000 + Math.random() * 9000)}`,
          status: "PENDING_VERIFICATION",
          startsAt,
          verificationRequired: true,
          developmentVerificationCode: "482913",
        } satisfies BookingResult;
      }
      return api.book({
        hostEmployeeId: values.hostEmployeeId,
        type: values.type,
        startsAt,
        endsAt,
        purpose: values.purpose,
        visitor: {
          firstName: values.firstName,
          lastName: values.lastName,
          email: values.email,
          phone: values.phone,
          company: values.company,
          consentVersion: "2026-07",
        },
      }, crypto.randomUUID());
    },
    onSuccess: (data) => {
      setResult(data);
      setStep(4);
    },
  });

  const selectedHost = useMemo(
    () => hosts.find((host) => host.id === hostId),
    [hostId, hosts],
  );

  const continueFromFirst = async () => {
    const valid = await form.trigger(["hostEmployeeId", "type", "date", "purpose"]);
    if (valid) setStep(2);
  };
  const continueFromSlot = async () => {
    const valid = await form.trigger(["slot"]);
    if (valid) setStep(3);
  };

  return (
    <main className="public-flow booking-flow">
      <header className="public-flow__nav container">
        <Brand />
        <Link className="back-link back-link--dark" to="/"><ArrowLeft size={16} /> Back home</Link>
      </header>

      <section className="booking-shell container">
        <aside className="booking-progress glass-card">
          <small className="kicker">APPOINTMENT REQUEST</small>
          <h1>Plan your visit.</h1>
          <p>It takes about two minutes. Your host will review the request before arrival.</p>
          <ol>
            {[
              ["Visit", "Choose who and why"],
              ["Time", "Select an open slot"],
              ["Details", "Tell us about you"],
              ["Done", "Keep your reference"],
            ].map(([title, detail], index) => (
              <li className={step === index + 1 ? "active" : step > index + 1 ? "complete" : ""} key={title}>
                <span>{step > index + 1 ? <Check size={15} /> : index + 1}</span>
                <div><strong>{title}</strong><small>{detail}</small></div>
              </li>
            ))}
          </ol>
          <div className="privacy-note">
            <ShieldCheck size={18} />
            <span><strong>Your details stay private.</strong><small>Only authorized BrainServe teams can access this request.</small></span>
          </div>
        </aside>

        <section className="booking-form glass-card">
          {isPreviewData && (
            <div className="preview-banner">
              Preview availability is shown because the local API is not connected.
            </div>
          )}

          {step === 1 && (
            <div className="form-step">
              <div className="form-step__heading">
                <span className="flow-icon"><UserRound size={21} /></span>
                <div><small>STEP 1 OF 3</small><h2>Who are you meeting?</h2></div>
              </div>
              <div className="field-grid">
                <label className="field-grid__wide">
                  Host
                  <select {...form.register("hostEmployeeId")} aria-invalid={Boolean(form.formState.errors.hostEmployeeId)}>
                    <option value="">Select a BrainServe host</option>
                    {hosts.map((host) => (
                      <option key={host.id} value={host.id}>{host.displayName} · {host.departmentId}</option>
                    ))}
                  </select>
                  {form.formState.errors.hostEmployeeId && <span className="field-error">{form.formState.errors.hostEmployeeId.message}</span>}
                </label>
                <label>
                  Visit type
                  <select {...form.register("type")}>
                    <option value="CLIENT_MEETING">Client meeting</option>
                    <option value="INTERVIEW">Interview</option>
                    <option value="HR_VISIT">HR visit</option>
                    <option value="VENDOR_VISIT">Vendor visit</option>
                    <option value="SERVICE_VISIT">Service visit</option>
                    <option value="DELIVERY">Delivery</option>
                  </select>
                </label>
                <label>
                  Preferred date
                  <input
                    type="date"
                    min={new Date().toISOString().slice(0, 10)}
                    {...form.register("date")}
                  />
                </label>
                <label className="field-grid__wide">
                  Purpose
                  <textarea
                    rows={4}
                    placeholder="A short note helps your host prepare."
                    {...form.register("purpose")}
                  />
                  {form.formState.errors.purpose && <span className="field-error">{form.formState.errors.purpose.message}</span>}
                </label>
              </div>
              <div className="form-footer">
                <span />
                <button className="button button--primary" type="button" onClick={continueFromFirst}>
                  Choose a time <ArrowRight size={17} />
                </button>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="form-step">
              <div className="form-step__heading">
                <span className="flow-icon"><CalendarDays size={21} /></span>
                <div><small>STEP 2 OF 3</small><h2>Select an available time.</h2></div>
              </div>
              <div className="selection-summary">
                <span className="avatar avatar--ruby">{selectedHost?.displayName.split(" ").map((part) => part[0]).join("").slice(0, 2)}</span>
                <div><strong>{selectedHost?.displayName}</strong><small>{selectedHost?.departmentId} · {new Date(`${date}T00:00:00`).toLocaleDateString("en-IN", { dateStyle: "full" })}</small></div>
              </div>
              {slotsQuery.isLoading && <div className="slot-loading">Finding open times…</div>}
              {!slotsQuery.isLoading && slots.length === 0 && (
                <div className="inline-error">No future slots are available on this date. Please choose another date.</div>
              )}
              <div className="slot-grid" role="radiogroup" aria-label="Available appointment times">
                {slots.map((slot) => {
                  const value = `${slot.startsAt}|${slot.endsAt}`;
                  return (
                    <label className={selectedSlot === value ? "slot selected" : "slot"} key={slot.startsAt}>
                      <input type="radio" value={value} {...form.register("slot")} />
                      <Clock3 size={17} />
                      <span>{new Date(slot.startsAt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}</span>
                      <small>30 min</small>
                    </label>
                  );
                })}
              </div>
              {form.formState.errors.slot && <span className="field-error">{form.formState.errors.slot.message}</span>}
              <div className="form-footer">
                <button className="button button--ghost" type="button" onClick={() => setStep(1)}><ArrowLeft size={16} /> Back</button>
                <button className="button button--primary" type="button" onClick={continueFromSlot}>Your details <ArrowRight size={17} /></button>
              </div>
            </div>
          )}

          {step === 3 && (
            <form className="form-step" onSubmit={form.handleSubmit((values) => submission.mutate(values))}>
              <div className="form-step__heading">
                <span className="flow-icon"><Mail size={21} /></span>
                <div><small>STEP 3 OF 3</small><h2>How can we reach you?</h2></div>
              </div>
              <div className="field-grid">
                <label>First name<input {...form.register("firstName")} />{form.formState.errors.firstName && <span className="field-error">{form.formState.errors.firstName.message}</span>}</label>
                <label>Last name<input {...form.register("lastName")} />{form.formState.errors.lastName && <span className="field-error">{form.formState.errors.lastName.message}</span>}</label>
                <label>Email<input type="email" {...form.register("email")} />{form.formState.errors.email && <span className="field-error">{form.formState.errors.email.message}</span>}</label>
                <label>Phone<input {...form.register("phone")} />{form.formState.errors.phone && <span className="field-error">{form.formState.errors.phone.message}</span>}</label>
                <label className="field-grid__wide">Company <span className="optional">Optional</span><input {...form.register("company")} /></label>
                <label className="checkbox field-grid__wide">
                  <input type="checkbox" {...form.register("consent")} />
                  <span>I agree to BrainServe’s visitor privacy notice and consent to the use of these details for this visit.</span>
                </label>
                {form.formState.errors.consent && <span className="field-error field-grid__wide">{form.formState.errors.consent.message}</span>}
              </div>
              {submission.isError && (
                <div className="inline-error" role="alert">
                  {submission.error instanceof ApiProblem ? submission.error.message : submission.error.message}
                </div>
              )}
              <div className="form-footer">
                <button className="button button--ghost" type="button" onClick={() => setStep(2)}><ArrowLeft size={16} /> Back</button>
                <button className="button button--primary" disabled={submission.isPending}>
                  {submission.isPending ? "Sending request…" : "Send appointment request"} <ArrowRight size={17} />
                </button>
              </div>
            </form>
          )}

          {step === 4 && result && (
            <div className="confirmation">
              <span className="confirmation__icon"><BadgeCheck size={35} /></span>
              <small className="kicker">REQUEST RECEIVED</small>
              <h2>Thank you. Your visit is in motion.</h2>
              <p>Keep this private reference. We’ll use your contact details to complete verification and share the host’s decision.</p>
              <div className="reference-card">
                <small>TRACKING REFERENCE</small>
                <strong>{result.referenceNumber}</strong>
                <span>{result.developmentVerificationCode && `Preview verification code: ${result.developmentVerificationCode}`}</span>
              </div>
              <div className="confirmation__facts">
                <span><Building2 size={18} /><div><small>Host</small><strong>{selectedHost?.displayName}</strong></div></span>
                <span><CalendarDays size={18} /><div><small>Requested time</small><strong>{new Date(result.startsAt).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" })}</strong></div></span>
              </div>
              <div className="hero__actions">
                <Link className="button button--primary" to="/track">Track this visit</Link>
                <Link className="button button--glass" to="/">Return home</Link>
              </div>
            </div>
          )}
        </section>
      </section>
    </main>
  );
}
