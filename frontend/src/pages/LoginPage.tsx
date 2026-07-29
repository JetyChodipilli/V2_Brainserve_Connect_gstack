import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight, Eye, EyeOff, LockKeyhole, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "../routing";
import { z } from "zod";
import { ApiProblem } from "../api/client";
import { Brand } from "../components/Brand";
import { useAuth } from "../state/auth";

const schema = z.object({
  login: z.email("Enter a valid BrainServe email"),
  password: z.string().min(12, "Password must contain at least 12 characters"),
});

type LoginValues = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const { login, startPreview } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState("");
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginValues>({
    resolver: zodResolver(schema),
  });

  const submit = handleSubmit(async (values) => {
    setServerError("");
    try {
      await login(values.login, values.password);
      navigate("/app");
    } catch (error) {
      setServerError(error instanceof ApiProblem ? error.message : "BrainServe Connect is not reachable.");
    }
  });

  const preview = () => {
    startPreview("ROLE_HR_ADMIN");
    navigate("/app");
  };

  return (
    <main className="auth-page">
      <section className="auth-story">
        <Link className="back-link" to="/"><ArrowLeft size={16} /> Back to visitor portal</Link>
        <div className="auth-story__content">
          <Brand />
          <span className="eyebrow eyebrow--light">Internal workspace</span>
          <h1>Good work starts with a clear welcome.</h1>
          <p>
            Review appointments, support visitors, and keep your team connected from one secure place.
          </p>
          <div className="auth-proof">
            <ShieldCheck size={22} />
            <div><strong>Least-privilege access</strong><small>Your workspace adapts to your role and permissions.</small></div>
          </div>
        </div>
        <span className="auth-story__meta">Brain Serve Pvt. Ltd. · Hyderabad</span>
      </section>
      <section className="auth-form-wrap">
        <div className="auth-form-card">
          <span className="auth-lock"><LockKeyhole size={20} /></span>
          <small className="kicker">TEAM SIGN IN</small>
          <h2>Welcome back</h2>
          <p>Use your BrainServe account to continue.</p>
          <form onSubmit={submit} noValidate>
            <label>
              Work email
              <input
                autoComplete="username"
                placeholder="name@brainserve.in"
                aria-invalid={Boolean(errors.login)}
                {...register("login")}
              />
              {errors.login && <span className="field-error">{errors.login.message}</span>}
            </label>
            <label>
              Password
              <span className="password-field">
                <input
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  aria-invalid={Boolean(errors.password)}
                  {...register("password")}
                />
                <button type="button" onClick={() => setShowPassword((value) => !value)} aria-label="Toggle password visibility">
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </span>
              {errors.password && <span className="field-error">{errors.password.message}</span>}
            </label>
            <small className="auth-help">Password recovery is handled by your BrainServe system administrator.</small>
            {serverError && <div className="inline-error" role="alert">{serverError}</div>}
            <button className="button button--primary button--full" disabled={isSubmitting}>
              {isSubmitting ? "Signing in…" : "Sign in securely"} <ArrowRight size={17} />
            </button>
          </form>
          <div className="preview-divider"><span>or explore the interface</span></div>
          <button className="button button--glass button--full" type="button" onClick={preview}>
            Preview workspace
          </button>
          <small className="auth-help">Preview data stays in this browser session and is clearly labelled.</small>
        </div>
      </section>
    </main>
  );
}
