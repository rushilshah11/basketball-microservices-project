import PlayerCard from "@/components/PlayerCard";
import SearchBar from "@/components/SearchBar";

async function getTop10Players() {
  // This fetch happens on the server (node.js), not the browser!
  // It connects to your API Gateway container directly if on same network,
  // or localhost if running locally.
  const res = await fetch(
    "http://api-gateway:8080/api/players/search?name=LeBron",
    { cache: "no-store" }
  );
  // Note: Your backend logic for "Top 10" needs to exist, using search for demo
  return res.json();
}

export default async function HomePage() {
  const initialPlayers = await getTop10Players();

  return (
    <main className="container mx-auto p-4">
      <h1 className="text-3xl font-bold mb-6">NBA Stats Tracker</h1>

      {/* Client Component for interactivity */}
      <SearchBar />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-8">
        {initialPlayers.map((player: any) => (
          <PlayerCard key={player.id} player={player} />
        ))}
      </div>
    </main>
  );
}
