"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useUser } from "@/shared/hooks/useUser";

export default function Dashboard() {
    const { isAuth } = useUser();
    const router = useRouter();

    useEffect(() => {
        if (!isAuth) router.replace("/login");
    }, [isAuth]);

    return (
        <>
            <h1>Main page</h1>
        </>
    )
}