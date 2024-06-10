"use client";

import { useEffect } from "react";

import { getSmartConfig } from "@/shared/lib/requests";

export default function SettingsDisplay() {
  useEffect(() => {
    (async () => {
      const res = await getSmartConfig();
      console.log(res);
    })();
  }, []);

  return <h1>Settings page</h1>;
}
