import { cookies } from "next/headers";

async function getPredictions() {
  const cookieStore = cookies();
  const token = cookieStore.get("token"); // Retrieve token from HTTP cookie

  // Fetch watchlist + predictions
  const res = await fetch("http://api-gateway:8080/api/watchlists?userId=1", {
    headers: { Authorization: `Bearer ${token?.value}` },
  });
  return res.json();
}

export default async function PredictionsPage() {
  const predictions = await getPredictions();

  return (
    <div className="p-4">
      <h2 className="text-2xl font-bold">Next Game Predictions</h2>
      <ul>
        {predictions.map((p: any) => (
          <li
            key={p.id}
            className="border p-4 my-2 rounded flex justify-between"
          >
            <span>{p.playerName}</span>
            <span className="font-bold text-green-600">
              Predicted Pts: {p.predictedPoints}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
