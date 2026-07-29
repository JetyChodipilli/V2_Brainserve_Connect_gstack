import { Inbox } from "lucide-react";

export function EmptyState({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="empty-state">
      <span className="empty-state__icon"><Inbox size={22} /></span>
      <strong>{title}</strong>
      <p>{detail}</p>
    </div>
  );
}
