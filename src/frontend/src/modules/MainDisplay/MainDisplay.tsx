"use client";

import { useContext, useEffect } from "react";
import { getTuyaData } from "@/shared/lib/requests";
import { isAxiosError } from "axios";
import { notifyContext } from "@/shared/context/notifications";

export default function Dashboard() {
  const { setNotification } = useContext(notifyContext);

  useEffect(() => {
    (async () => {
      try {
        const data = await getTuyaData();
        console.log(data);
      } catch (err) {
        if (isAxiosError(err)) {
          setNotification("Failed", err.response?.data.message || err.message);
        } else {
          console.log(err);
        }
      }
    })();
  }, []);

  return (
    <>
      <h1>Main page</h1>
    </>
  );
}
