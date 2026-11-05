import { createClient } from "@supabase/supabase-js";

// One shared client for the whole app. It stores the login session in the browser
// and attaches it to every request, so Row Level Security knows who the user is.
export const supabase = createClient(
  process.env.NEXT_PUBLIC_SUPABASE_URL!,
  process.env.NEXT_PUBLIC_SUPABASE_KEY!,
);
