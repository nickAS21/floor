"use client";

import { Box } from "@mantine/core";
import { useEffect } from "react";
import { useRouter } from "next/navigation";

import Header from "@/modules/Header";
import { useUser } from "@/shared/hooks/useUser";
import Sidebar from "../Sidebar";

export default function Dashboard() {
    const { isAuth } = useUser();
    const router = useRouter();

    useEffect(() => {
        if (!isAuth) router.replace("/login");
    }, [isAuth]);

    return (
        <Box className="dashboard_layout dark:text-white">
            <Sidebar />
            <Header />
            <div className="h-[calc(100vh - 70px)] board">
                <h1>Main page</h1>
            </div>
        </Box>
    )
}