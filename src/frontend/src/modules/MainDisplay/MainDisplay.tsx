"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useUser } from "@/shared/hooks/useUser";
import { getTuyaData } from "@/shared/lib/requests";

export default function Dashboard() {
  const { isAuth } = useUser();
  const router = useRouter();

  useEffect(() => {
    if (!isAuth) router.replace("/login");
  }, [isAuth]);

  useEffect(() => {
    (async () => {
      const data = await getTuyaData();
      console.log(data);
    })();
  }, []);

  return (
    <>
      <h1>Main page</h1>
    </>
  );
}
