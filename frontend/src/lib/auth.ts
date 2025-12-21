// frontend/src/lib/auth.ts

// Auth utilities for managing authentication state
// NOTE: Actual security is handled by HttpOnly cookies via server actions.
// These functions manage a "UI flag" cookie so the frontend knows to show "Logout" vs "Login".

export function setAuthToken(token: string): void {
    if (typeof document === 'undefined') return;

    // We ignore the actual 'token' string here because we rely on the HttpOnly cookie for security.
    // Instead, we set a simple flag so the client knows it's logged in.
    document.cookie = `auth_status=authenticated; path=/; max-age=86400; SameSite=Lax`;

    // Notify components (like Navbar) of the change immediately
    window.dispatchEvent(new Event('auth-change'));
}

export function removeAuthToken(): void {
    if (typeof document === 'undefined') return;

    // Remove the flag cookie
    document.cookie = 'auth_status=; path=/; max-age=0';

    // Notify components of the change immediately
    window.dispatchEvent(new Event('auth-change'));
}

export function isAuthenticated(): boolean {
    if (typeof document === 'undefined') return false;

    // Check for the flag cookie
    const cookies = document.cookie.split(';');
    return cookies.some(c => c.trim().startsWith('auth_status='));
}