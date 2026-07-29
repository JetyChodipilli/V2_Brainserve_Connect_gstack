import { useMutation } from "@tanstack/react-query";
import { ArrowLeft, CalendarClock, Search, UserRound } from "lucide-react";
import { useState } from "react";
import { Link } from "../routing";
import { api, ApiProblem } from "../api/client";
import { Brand } from "../components/Brand";
import { StatusBadge } from "../components/StatusBadge";

export function TrackPage() {
  const [reference, setReference] = useState("");
  const tracking = useMutation({
    mutationFn: () => api.track(reference.trim().toUpperCase()),
  });

  return (
    <main className="public-flow">
      <header className="public-flow__nav container">
        <Brand />
        <Link className="back-link back-link--dark" to="/"><ArrowLeft size={16} /> Back home</Link>
      </header>
      <section className="flow-card glass-card flow-card--narrow">
        <span className="flow-icon"><Search size={23} /></span>
        <small className="kicker">VISIT TRACKING</small>
        <h1>Where is my request?</h1>
        <p>Enter the private reference shown after you submitted your appointment.</p>
        <form
          className="track-form"
          onSubmit={(event) => {
            event.preventDefault();
            tracking.mutate();
          }}
        >
          <label>
            Tracking reference
            <input
              value={reference}
              onChange={(event) => setReference(event.target.value)}
              placeholder="BSA-7Q3KX9M2"
              minLength={8}
              required
            />
          </label>
          <button className="button button--primary" disabled={tracking.isPending}>
            {tracking.isPending ? "Checking…" : "Track request"}
          </button>
        </form>
        {tracking.isError && (
          <div className="inline-error" role="alert">
            {tracking.error instanceof ApiProblem ? tracking.error.message : "Unable to check this reference."}
          </div>
        )}
        {tracking.data && (
          <article className="tracking-result">
            <div>
              <small>{tracking.data.referenceNumber}</small>
              <StatusBadge status={tracking.data.status} />
            </div>
            <h2>{tracking.data.type.replaceAll("_", " ")}</h2>
            <p><UserRound size={17} /> {tracking.data.hostName}</p>
            <p><CalendarClock size={17} /> {new Date(tracking.data.startsAt).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" })}</p>
          </article>
        )}
      </section>
    </main>
  );
}
