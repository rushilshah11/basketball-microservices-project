import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import Link from 'next/link';
import StatsDashboard from '@/components/StatsDashboard';

export default async function HomePage() {
  const cookieStore = await cookies();
  const token = cookieStore.get('token');
  const isAuthenticated = !!token;

  // If authenticated, show dashboard
  if (isAuthenticated) {
    return <StatsDashboard />;
  }

  // Public state: Show hero section
  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <div className="text-center max-w-4xl mx-auto">
        <div className="mb-8">
          <h1 className="text-6xl md:text-7xl font-bold text-white mb-4 drop-shadow-lg">
            Welcome to Basketball Analytics
          </h1>
          <p className="text-xl md:text-2xl text-gray-200 mb-8">
            Track player stats, predictions, and build your dream team
          </p>
        </div>
        
        <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
          <Link
            href="/login"
            className="px-8 py-4 bg-orange-600 text-white font-bold text-lg rounded-lg hover:bg-orange-700 transition-all duration-200 shadow-xl hover:shadow-2xl transform hover:scale-105"
          >
            Get Started
          </Link>
          <Link
            href="/register"
            className="px-8 py-4 bg-white text-orange-600 font-bold text-lg rounded-lg hover:bg-gray-100 transition-all duration-200 shadow-xl hover:shadow-2xl transform hover:scale-105"
          >
            Create Account
          </Link>
        </div>

        {/* Feature highlights */}
        <div className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="bg-white/10 backdrop-blur-sm rounded-lg p-6 border border-white/20">
            <div className="text-4xl mb-4">📊</div>
            <h3 className="text-xl font-semibold mb-2">Real-Time Stats</h3>
            <p className="text-gray-300">Access comprehensive player statistics and performance metrics</p>
          </div>
          <div className="bg-white/10 backdrop-blur-sm rounded-lg p-6 border border-white/20">
            <div className="text-4xl mb-4">🔮</div>
            <h3 className="text-xl font-semibold mb-2">AI Predictions</h3>
            <p className="text-gray-300">Get AI-powered predictions for player performance in upcoming games</p>
          </div>
          <div className="bg-white/10 backdrop-blur-sm rounded-lg p-6 border border-white/20">
            <div className="text-4xl mb-4">⭐</div>
            <h3 className="text-xl font-semibold mb-2">Watchlist</h3>
            <p className="text-gray-300">Build and manage your personal watchlist of favorite players</p>
          </div>
        </div>
      </div>
    </div>
  );
}

