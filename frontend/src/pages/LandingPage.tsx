import {
  ArrowRight,
  BadgeCheck,
  Building2,
  CalendarCheck2,
  ChevronRight,
  Clock3,
  QrCode,
  ShieldCheck,
  Sparkles,
  UsersRound,
} from "lucide-react";
import { Link } from "../routing";
import { Brand } from "../components/Brand";

const portals = [
  { icon: UsersRound, title: "People operations", detail: "Onboard employees and manage organization structure." },
  { icon: CalendarCheck2, title: "Host approvals", detail: "Keep every appointment decision clear and accountable." },
  { icon: QrCode, title: "Faster arrivals", detail: "Verify, issue a badge, and check visitors in within seconds." },
  { icon: ShieldCheck, title: "Live access control", detail: "Know exactly who is inside during normal and emergency operations." },
];

export function LandingPage() {
  return (
    <main>
      <header className="public-nav container">
        <Brand />
        <nav aria-label="Primary navigation">
          <a href="#how-it-works">How it works</a>
          <a href="#portals">Portals</a>
          <Link to="/track">Track visit</Link>
        </nav>
        <div className="public-nav__actions">
          <Link className="button button--ghost" to="/login">Team sign in</Link>
          <Link className="button button--primary button--small" to="/book">
            Book a visit <ArrowRight size={16} />
          </Link>
        </div>
      </header>

      <section className="hero container">
        <div className="hero__content">
          <span className="eyebrow"><Sparkles size={14} /> A calmer way to coordinate every visit</span>
          <h1>
            Every welcome,
            <span> perfectly connected.</span>
          </h1>
          <p>
            BrainServe Connect brings visitors, hosts, reception, HR, and security into one
            secure appointment journey—without the calls, spreadsheets, or uncertainty.
          </p>
          <div className="hero__actions">
            <Link className="button button--primary" to="/book">
              Request an appointment <ArrowRight size={18} />
            </Link>
            <Link className="button button--glass" to="/track">
              Track an existing visit
            </Link>
          </div>
          <div className="hero__trust">
            <span><BadgeCheck size={18} /> Permission controlled</span>
            <span><Clock3 size={18} /> Real-time status</span>
            <span><ShieldCheck size={18} /> Privacy by design</span>
          </div>
        </div>

        <div className="hero__visual" aria-label="BrainServe visit overview preview">
          <div className="glass-card visit-card">
            <div className="visit-card__top">
              <div>
                <small>TODAY AT 03:30 PM</small>
                <h2>Client discovery</h2>
              </div>
              <span className="status status--approved"><span />Approved</span>
            </div>
            <div className="visitor-profile">
              <span className="avatar avatar--ruby">AR</span>
              <div>
                <strong>Ananya Rao</strong>
                <small>Example Labs · Guest</small>
              </div>
              <span className="qr-mini"><QrCode size={29} /></span>
            </div>
            <div className="visit-route">
              <span className="route-node route-node--complete"><BadgeCheck size={15} /></span>
              <span className="route-line route-line--complete" />
              <span className="route-node route-node--complete"><BadgeCheck size={15} /></span>
              <span className="route-line" />
              <span className="route-node">3</span>
            </div>
            <div className="visit-labels">
              <span>Requested</span><span>Host approved</span><span>Arrival</span>
            </div>
            <div className="host-row">
              <span className="avatar avatar--soft">AM</span>
              <div><small>MEETING WITH</small><strong>Arjun Mehta · Engineering</strong></div>
              <ChevronRight size={18} />
            </div>
          </div>
          <div className="floating-chip floating-chip--top">
            <span><ShieldCheck size={17} /></span>
            <div><strong>Visitor verified</strong><small>Privacy consent recorded</small></div>
          </div>
          <div className="floating-chip floating-chip--bottom">
            <span><Building2 size={17} /></span>
            <div><strong>Hyderabad office</strong><small>Gate A · Badge BS-014</small></div>
          </div>
        </div>
      </section>

      <section className="signal-strip">
        <div className="container signal-strip__inner">
          <div><strong>01</strong><span>One clear visitor journey</span></div>
          <div><strong>07</strong><span>Purpose-built portals</span></div>
          <div><strong>24/7</strong><span>Live emergency visibility</span></div>
          <div><strong>0</strong><span>Salary fields in reception APIs</span></div>
        </div>
      </section>

      <section className="section container" id="portals">
        <div className="section-heading">
          <span className="eyebrow">One system, each team’s view</span>
          <h2>Focused tools for every BrainServe role.</h2>
          <p>Each portal exposes only the information and actions required for the work at hand.</p>
        </div>
        <div className="portal-grid">
          {portals.map(({ icon: Icon, title, detail }, index) => (
            <article className="portal-card glass-card" key={title}>
              <span className="portal-card__number">0{index + 1}</span>
              <span className="portal-card__icon"><Icon size={22} /></span>
              <h3>{title}</h3>
              <p>{detail}</p>
              <a className="portal-card__link" href="#how-it-works">Explore the workflow <ArrowRight size={15} /></a>
            </article>
          ))}
        </div>
      </section>

      <section className="section workflow-section" id="how-it-works">
        <div className="container workflow-layout">
          <div className="section-heading section-heading--left">
            <span className="eyebrow">From request to goodbye</span>
            <h2>One traceable path. No loose ends.</h2>
            <p>
              Every handoff has an owner, timestamp, status, and audit trail—so your team can
              welcome people instead of chasing updates.
            </p>
            <Link className="text-link" to="/book">Start a visitor request <ArrowRight size={16} /></Link>
          </div>
          <ol className="workflow-list">
            <li><span>1</span><div><strong>Choose a host and time</strong><p>Only future, currently available slots can be requested.</p></div></li>
            <li><span>2</span><div><strong>Verify and receive a decision</strong><p>The assigned host or authorized delegate reviews the purpose.</p></div></li>
            <li><span>3</span><div><strong>Arrive with a secure reference</strong><p>Reception verifies identity and allocates an available badge.</p></div></li>
            <li><span>4</span><div><strong>Check out cleanly</strong><p>The badge is released and live occupancy updates immediately.</p></div></li>
          </ol>
        </div>
      </section>

      <footer className="footer container">
        <Brand />
        <p>Secure appointments and visitor operations for Brain Serve Pvt. Ltd.</p>
        <span>© 2026 Brain Serve</span>
      </footer>
    </main>
  );
}
