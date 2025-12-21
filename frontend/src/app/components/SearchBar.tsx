"use client";

import { useState, useEffect } from "react";
import { useDebounce } from "use-debounce"; // npm install use-debounce

export default function SearchBar() {
  const [text, setText] = useState("");
  const [query] = useDebounce(text, 500); // Wait 500ms
  const [results, setResults] = useState([]);

  useEffect(() => {
    if (query) {
      // Fetch dynamic results
      fetch(`http://localhost:8080/api/players/search?name=${query}`)
        .then((res) => res.json())
        .then((data) => setResults(data));
    }
  }, [query]);

  return (
    <div>
      <input
        type="text"
        placeholder="Search Player..."
        className="w-full p-4 border rounded shadow"
        onChange={(e) => setText(e.target.value)}
      />
      {/* Render Dynamic Results Dropdown here */}
    </div>
  );
}
