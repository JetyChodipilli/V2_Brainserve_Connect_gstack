import { Link } from "../routing";

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link className={`brand ${compact ? "brand--compact" : ""}`} to="/" aria-label="BrainServe Connect home">
      <span className="brand__mark" aria-hidden="true">
        <span>B</span>
        <span>S</span>
      </span>
      {!compact && (
        <span>
          <strong>BrainServe</strong>
          <small>Connect</small>
        </span>
      )}
    </Link>
  );
}
