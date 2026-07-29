const labels: Record<string, string> = {
  PENDING_APPROVAL: "Awaiting host",
  PENDING_VERIFICATION: "Verify contact",
  APPROVED: "Approved",
  CHECKED_IN: "Inside",
  CHECKED_OUT: "Checked out",
  COMPLETED: "Completed",
  REJECTED: "Rejected",
  ONBOARDING: "Onboarding",
  ACTIVE: "Active",
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`status status--${status.toLowerCase()}`}>
      <span aria-hidden="true" />
      {labels[status] ?? status.replaceAll("_", " ").toLowerCase()}
    </span>
  );
}
