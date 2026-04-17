import api from "./axios";
import type { AuthTokens } from "../fixtures/types";

export interface RegisterRequest {
  email: string;
}

export interface VerifyRequest {
  email: string;
  tan: string;
}

export interface CompleteRegistrationRequest {
  firstName: string;
  lastName: string;
  academicTitle: string;
  institution: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export const authApi = {
  register: (body: RegisterRequest) => api.post<{ accepted: true }>("/auth/register", body),
  verify: (body: VerifyRequest) => api.post<{ verified: true; verificationToken: string }>("/auth/verify", body),
  complete: (body: CompleteRegistrationRequest) => api.post<AuthTokens>("/auth/complete", body),
  login: (body: LoginRequest) => api.post<AuthTokens>("/auth/login", body),
  refresh: (refresh: string) => api.post<AuthTokens>("/auth/refresh", { refresh }),
  logout: (refresh: string) => api.post<{ ok: true }>("/auth/logout", { refresh })
};
