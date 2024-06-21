"use client";

import { useContext, useEffect } from "react";

import { getSmartConfig } from "@/shared/lib/requests";
import { isAxiosError } from "axios";
import { notifyContext } from "@/shared/context/notifications";

export default function SettingsDisplay() {
  const { setNotification } = useContext(notifyContext);

  useEffect(() => {
    (async () => {
      try {
        const res = await getSmartConfig();
        console.log(res);
      } catch (err) {
        if (isAxiosError(err) && err.response?.status == 404) {
          setNotification("Failed", err.response.data[0]);
        } else {
          console.error(err);
        }
      }
    })();
  }, []);

  return <h1>Settings page</h1>;
}
