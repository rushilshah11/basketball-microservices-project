import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const SPRING_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

async function handler(
    req: NextRequest,
    { params }: { params: Promise<{ path: string[] }> }
) {
    // 1. Await the params before using them
    const resolvedParams = await params;
    const path = resolvedParams.path.join('/');

    const query = req.nextUrl.search; // Keep query params like ?name=LeBron

    // 2. Get the Token from the HTTP-Only Cookie
    const cookieStore = await cookies();
    const token = cookieStore.get('token')?.value;

    // 3. Prepare headers
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
    };

    // Add the Authorization header for Spring Boot
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        // 4. Forward the request to Spring Boot
        const response = await fetch(`${SPRING_API_URL}/api/${path}${query}`, {
            method: req.method,
            headers: headers,
            body: req.method !== 'GET' ? req.body : undefined,
            // @ts-ignore
            duplex: 'half',
        });

        // 5. Return the response back to the Client
        const data = await response.json();
        return NextResponse.json(data, { status: response.status });

    } catch (error) {
        return NextResponse.json({ error: 'Proxy failed' }, { status: 500 });
    }
}

export { handler as GET, handler as POST, handler as PUT, handler as DELETE };