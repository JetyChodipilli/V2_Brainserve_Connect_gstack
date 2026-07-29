import { useEffect } from "react";
import { BookingPage } from "./pages/BookingPage";
import { DashboardPage } from "./pages/DashboardPage";
import { LandingPage } from "./pages/LandingPage";
import { LoginPage } from "./pages/LoginPage";
import { TrackPage } from "./pages/TrackPage";
import { navigate, usePathname } from "./routing";
import { useAuth } from "./state/auth";

function Protected({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  useEffect(() => {
    if (!user) navigate("/login", { replace: true });
  }, [user]);
  return user ? children : <LoginPage />;
}

export function App() {
  const path = usePathname();
  let page: React.ReactNode;
  if (path === "/") page = <LandingPage />;
  else if (path === "/book") page = <BookingPage />;
  else if (path === "/track") page = <TrackPage />;
  else if (path === "/login") page = <LoginPage />;
  else if (path === "/app" || path.startsWith("/app/")) {
    page = <Protected><DashboardPage /></Protected>;
  } else {
    page = <LandingPage />;
  }

  return (
    <div className="app-shell">
      <div className="ambient ambient--one" />
      <div className="ambient ambient--two" />
      {page}
    </div>
  );
}
