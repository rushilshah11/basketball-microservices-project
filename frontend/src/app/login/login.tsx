"use client"; // Client component for form handling

import { useState } from "react";
import { useRouter } from "next/navigation";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    // Call your backend
    const res = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/api/v1/auth/authenticate`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      }
    );

    if (res.ok) {
      const data = await res.json();
      // IN A REAL APP: Call a Server Action here to set the cookie securely
      // For now (learning): Store in document.cookie or simple storage
      document.cookie = `token=${data.token}; path=/`;
      router.push("/"); // Redirect to Home
    }
  };

  return (
    <form onSubmit={handleLogin} className="p-4 max-w-sm mx-auto">
      <h1 className="text-2xl font-bold mb-4">Login</h1>
      <input
        type="email"
        placeholder="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        className="block w-full border p-2 mb-2 rounded"
      />
      <input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        className="block w-full border p-2 mb-4 rounded"
      />
      <button
        type="submit"
        className="bg-blue-600 text-white px-4 py-2 rounded"
      >
        Authenticate
      </button>
    </form>
  );
}
