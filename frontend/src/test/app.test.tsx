import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "../App";
import { AuthProvider } from "../state/auth";

function renderApp(path = "/") {
  window.history.replaceState(null, "", path);
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </QueryClientProvider>,
  );
}

describe("BrainServe Connect", () => {
  it("renders the visitor value proposition", () => {
    renderApp();
    expect(screen.getByRole("heading", { name: /every welcome/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /request an appointment/i })).toHaveAttribute("href", "/book");
  });

  it("protects the internal workspace", () => {
    renderApp("/app");
    expect(screen.getByRole("heading", { name: /welcome back/i })).toBeInTheDocument();
  });

  it("renders the appointment wizard with accessible host selection", () => {
    renderApp("/book");
    expect(screen.getByRole("heading", { name: /who are you meeting/i })).toBeInTheDocument();
    expect(screen.getByLabelText("Host")).toBeInTheDocument();
  });
});
