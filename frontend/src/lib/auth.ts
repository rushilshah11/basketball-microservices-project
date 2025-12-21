// Auth utilities for managing authentication state

export function setAuthToken(token: string): void {
  if (typeof document === 'undefined') return;
  // Set HTTP-only cookie via server action or use secure cookie
  // For now, using document.cookie with httpOnly simulation
  document.cookie = `token=${token}; path=/; max-age=86400; SameSite=Lax`;
}

export function removeAuthToken(): void {
  if (typeof document === 'undefined') return;
  document.cookie = 'token=; path=/; max-age=0';
}

export function isAuthenticated(): boolean {
  if (typeof document === 'undefined') return false;
  const cookies = document.cookie.split(';');
  return cookies.some(c => c.trim().startsWith('token='));
}

