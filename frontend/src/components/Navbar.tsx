'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { isAuthenticated, removeAuthToken } from '@/lib/auth';
import { removeAuthCookie } from '@/app/actions';

export default function Navbar() {
  const [authenticated, setAuthenticated] = useState(false);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    setAuthenticated(isAuthenticated());
      // Event handler for custom auth-change event
      const handleAuthChange = () => {
          setAuthenticated(isAuthenticated());
      };
      // Listen for custom event
      window.addEventListener('auth-change', handleAuthChange);
      // Cleanup
      return () => {
          window.removeEventListener('auth-change', handleAuthChange);
      };
  }, [pathname]);

  const handleLogout = async () => {
    removeAuthToken();
    await removeAuthCookie();
    setAuthenticated(false);
    router.push('/');
    router.refresh();
  };

  return (
    <nav className="bg-gradient-to-r from-orange-600 via-red-600 to-orange-600 shadow-lg border-b-4 border-orange-400">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center space-x-2 group">
            <div className="text-2xl font-bold text-white tracking-wide">
              🏀 NBA Stats Tracker
            </div>
          </Link>

          {/* Right Side Navigation */}
          <div className="flex items-center space-x-4">
            {authenticated ? (
              <>
                <Link
                  href="/team"
                  className="px-4 py-2 bg-white text-orange-600 font-semibold rounded-lg hover:bg-orange-50 transition-colors duration-200 shadow-md hover:shadow-lg"
                >
                  View Team
                </Link>
                <button
                  onClick={handleLogout}
                  className="px-4 py-2 bg-red-700 text-white font-semibold rounded-lg hover:bg-red-800 transition-colors duration-200 shadow-md hover:shadow-lg"
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link
                  href="/login"
                  className="px-4 py-2 bg-white text-orange-600 font-semibold rounded-lg hover:bg-orange-50 transition-colors duration-200 shadow-md hover:shadow-lg"
                >
                  Login
                </Link>
                <Link
                  href="/register"
                  className="px-4 py-2 bg-orange-500 text-white font-semibold rounded-lg hover:bg-orange-600 transition-colors duration-200 shadow-md hover:shadow-lg"
                >
                  Register
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

